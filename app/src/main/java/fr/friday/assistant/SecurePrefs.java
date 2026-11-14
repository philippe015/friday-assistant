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

/** Les clés API restent privées, chiffrées via Android Keystore, non transmises au WebView. */
public final class SecurePrefs {
    private static final String ALIAS = "friday_local_api_keys_v17";
    private final SharedPreferences prefs;
    public SecurePrefs(Context c) { prefs = c.getSharedPreferences("friday_native", Context.MODE_PRIVATE); }
    public String get(String k, String d) { return prefs.getString(k, d); }
    public void put(String k, String v) { prefs.edit().putString(k, v).apply(); }
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
    public synchronized void setSecret(String name, String value) throws Exception {
        if (value == null || value.isEmpty()) { prefs.edit().remove("secret_" + name).apply(); return; }
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, key());
        byte[] encrypted = c.doFinal(value.getBytes(StandardCharsets.UTF_8));
        String data = Base64.encodeToString(c.getIV(), Base64.NO_WRAP) + ":" +
                Base64.encodeToString(encrypted, Base64.NO_WRAP);
        prefs.edit().putString("secret_" + name, data).apply();
    }
    public synchronized String secret(String name) {
        String data = prefs.getString("secret_" + name, "");
        if (data == null || data.isEmpty()) return "";
        try {
            String[] split = data.split(":", 2);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Base64.decode(split[0], Base64.NO_WRAP)));
            return new String(c.doFinal(Base64.decode(split[1], Base64.NO_WRAP)), StandardCharsets.UTF_8);
        } catch (Exception e) { return ""; }
    }
}
