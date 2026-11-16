package fr.friday.assistant;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Stockage des clés API : chiffrement AES-GCM via Android Keystore.
 *
 * Depuis la v3.8.0, aucune écriture ne replie sur Base64 non chiffré.
 * Les anciens secrets Base64 sont réchiffrés lorsque Keystore fonctionne,
 * sinon leur lecture est refusée et {@link #LAST_ERROR} indique le problème.
 */
public final class SecurePrefs {
    private static final String ALIAS = "friday_local_api_keys_v17";
    private static final String PLAIN = "plain:";

    /** Dernier incident de chiffrement / déchiffrement (vide si tout va bien). */
    public static volatile String LAST_ERROR = "";

    private final SharedPreferences prefs;

    public SecurePrefs(Context c) {
        prefs = c.getSharedPreferences("friday_native", Context.MODE_PRIVATE);
    }

    public String get(String k, String d) { return prefs.getString(k, d); }
    public void put(String k, String v) { prefs.edit().putString(k, v).apply(); }

    /** Indique si une clé est utilisable (chiffrée ou repli). */
    public boolean hasSecret(String name) {
        String data = prefs.getString("secret_" + name, "");
        return data != null && !data.isEmpty();
    }

    private SecretKey key() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        if (ks.containsAlias(ALIAS)) return ((KeyStore.SecretKeyEntry) ks.getEntry(ALIAS, null)).getSecretKey();
        KeyGenerator gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        gen.init(new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true).build());
        return gen.generateKey();
    }

    public synchronized void setSecret(String name, String value) {
        if (value == null || value.isEmpty()) {
            prefs.edit().remove("secret_" + name).apply();
            return;
        }
        try {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key());
            byte[] encrypted = c.doFinal(value.getBytes(StandardCharsets.UTF_8));
            String data = Base64.encodeToString(c.getIV(), Base64.NO_WRAP) + ":" +
                    Base64.encodeToString(encrypted, Base64.NO_WRAP);
            prefs.edit().putString("secret_" + name, data).apply();
            LAST_ERROR = "";
        } catch (Exception e) {
            // Depuis v3.8, aucun secret ni profil ne peut être écrit en clair.
            LAST_ERROR = "Keystore indisponible : stockage non chiffré refusé";
            throw new IllegalStateException(LAST_ERROR, e);
        }
    }

    public synchronized String secret(String name) {
        String data = prefs.getString("secret_" + name, "");
        if (data == null || data.isEmpty()) return "";
        if (data.startsWith(PLAIN)) {
            try {
                // Migration de compatibilité : ne retourner le secret historique qu'après
                // écriture AES-GCM et lecture contrôlée via Android Keystore.
                String legacy = new String(Base64.decode(
                        data.substring(PLAIN.length()), Base64.NO_WRAP), StandardCharsets.UTF_8);
                setSecret(name, legacy);
                return legacy;
            } catch (Exception e) {
                LAST_ERROR = "Secret ancien en clair bloqué : migration Keystore impossible";
                return "";
            }
        }
        try {
            String[] split = data.split(":", 2);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Base64.decode(split[0], Base64.NO_WRAP)));
            String value = new String(c.doFinal(Base64.decode(split[1], Base64.NO_WRAP)), StandardCharsets.UTF_8);
            LAST_ERROR = "";
            return value;
        } catch (Exception e) {
            LAST_ERROR = "Déchiffrement impossible (" + e.getClass().getSimpleName() + ") : réenregistrez la clé";
            return "";
        }
    }

    /**
     * Stockage privé STRICT : contrairement aux anciennes clés API, aucun
     * repli Base64/plaintext n'est jamais autorisé pour la mémoire et l'historique.
     * L'écriture est synchrone : la migration ne détruit pas la source si elle échoue.
     */
    public synchronized void putPrivateSecret(String name, String value) throws Exception {
        if (name == null || !name.matches("[a-zA-Z0-9_]{1,80}"))
            throw new IllegalArgumentException("Identifiant sécurisé invalide");
        if (value == null) value = "";
        if (value.getBytes(StandardCharsets.UTF_8).length > 350000)
            throw new IllegalArgumentException("Mémoire privée trop volumineuse");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key());
        String data = Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP) + ":"
                + Base64.encodeToString(cipher.doFinal(value.getBytes(StandardCharsets.UTF_8)),
                Base64.NO_WRAP);
        if (!prefs.edit().putString("secret_" + name, data).commit())
            throw new java.io.IOException("Écriture chiffrée impossible");
    }

    public synchronized String privateSecret(String name) throws Exception {
        String data = prefs.getString("secret_" + name, "");
        if (data == null || data.isEmpty()) return "";
        if (data.startsWith(PLAIN))
            throw new java.security.GeneralSecurityException(
                    "Mémoire non chiffrée refusée : aucun repli en clair");
        String[] parts = data.split(":", 2);
        if (parts.length != 2) throw new java.security.GeneralSecurityException("Mémoire chiffrée illisible");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(),
                new GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)));
        return new String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)),
                StandardCharsets.UTF_8);
    }

    public synchronized boolean hasPrivateSecret(String name) {
        return hasSecret(name);
    }

    public synchronized void removePrivateSecret(String name) throws Exception {
        if (!prefs.edit().remove("secret_" + name).commit())
            throw new java.io.IOException("Suppression locale impossible");
    }
}
