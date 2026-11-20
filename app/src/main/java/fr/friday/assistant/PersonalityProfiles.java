package fr.friday.assistant;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.Set;

/** Profils utilisateur chiffrés dans Android Keystore, lus à chaque requête native. */
public final class PersonalityProfiles {
    private PersonalityProfiles() {}
    private static final String KEY = "personality_profiles_202";
    public static final int MAX_PROFILES = 10;
    public static final int MAX_PROMPT = 16000;
    public static final String DEFAULT_PROMPT =
        "Tu incarnes JARVIS, un assistant vocal personnel francophone.\n" +
        "Adopte un ton posé, réactif et précis.\n" +
        "Réponds oralement avec des phrases fluides, sans longs préambules.\n" +
        "Pour les tâches complexes, donne des étapes pratiques et vérifie les hypothèses.\n" +
        "Distingue les faits vérifiés des suppositions et des capacités indisponibles.";

    private static JSONObject defaultData() {
        JSONObject root = new JSONObject();
        try {
            JSONArray profiles = new JSONArray();
            profiles.put(new JSONObject().put("id", "friday").put("name", "JARVIS").put("prompt", DEFAULT_PROMPT));
            root.put("active", "friday").put("profiles", profiles);
        } catch (JSONException ignored) { }
        return root;
    }
    /** Valide le JSON, borne la taille, interdit les IDs en double et conserve uniquement les champs attendus. */
    public static JSONObject validated(String json) throws JSONException {
        if (json == null || json.length() > 180000) throw new JSONException("Sauvegarde trop longue");
        JSONObject incoming = new JSONObject(json);
        JSONArray profiles = incoming.getJSONArray("profiles");
        if (profiles.length() < 1 || profiles.length() > MAX_PROFILES)
            throw new JSONException("Nombre de profils invalide (1 à " + MAX_PROFILES + ")");
        JSONArray cleaned = new JSONArray();
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < profiles.length(); i++) {
            JSONObject p = profiles.getJSONObject(i);
            String id = p.getString("id");
            String name = p.getString("name").trim();
            String prompt = p.getString("prompt").trim();
            if (!id.matches("[a-zA-Z0-9_-]{1,64}") || !ids.add(id)) throw new JSONException("ID invalide ou dupliqué");
            if (name.isEmpty() || name.length() > 70) throw new JSONException("Nom invalide : 1 à 70 caractères");
            if (prompt.isEmpty() || prompt.length() > MAX_PROMPT) throw new JSONException("Prompt invalide : 1 à " + MAX_PROMPT + " caractères");
            cleaned.put(new JSONObject().put("id", id).put("name", name).put("prompt", prompt));
        }
        String active = incoming.getString("active");
        if (!ids.contains(active)) throw new JSONException("Profil actif introuvable");
        return new JSONObject().put("active", active).put("profiles", cleaned);
    }
    /** Conserve la clé de chiffrement, les identifiants de profils et les
     * profils personnalisés. Renomme uniquement le profil historique par
     * défaut. Ne modifie aucun secret en stockage lors d'une simple lecture. */
    private static JSONObject migratedDefault(JSONObject root) throws JSONException {
        JSONArray profiles = root.getJSONArray("profiles");
        for (int i = 0; i < profiles.length(); i++) {
            JSONObject p = profiles.getJSONObject(i);
            if (!"friday".equals(p.optString("id", ""))) continue;
            if ("F.R.I.D.A.Y.".equalsIgnoreCase(p.optString("name", "")))
                p.put("name", "JARVIS");
            String text = p.optString("prompt", "").trim();
            String oldNative = "Tu incarnes F.R.I.D.A.Y., un assistant vocal personnel francophone.\\n"
                    + "Adopte un ton posé, réactif et précis.\\n"
                    + "Réponds oralement avec des phrases fluides, sans longs préambules.\\n"
                    + "Pour les tâches complexes, donne des étapes pratiques et vérifie les hypothèses.\\n"
                    + "Distingue les faits vérifiés des suppositions et des capacités indisponibles.";
            String oldWeb = "Tu es F.R.I.D.A.Y., une assistante vocale personnelle inspirée des interfaces futuristes. "
                    + "Tu parles en français, avec précision, calme et intelligence. "
                    + "Donne des réponses orales naturelles et utiles. "
                    + "Différencie faits vérifiés et hypothèses, et cite les sources lorsque tu utilises Internet.";
            if (text.equals(oldNative) || text.equals(oldWeb))
                p.put("prompt", DEFAULT_PROMPT);
        }
        return root;
    }

    public static String read(SecurePrefs prefs) {
        try {
            String stored = prefs.secret(KEY);
            return stored.isEmpty() ? defaultData().toString()
                    : migratedDefault(validated(stored)).toString();
        } catch (Exception ignored) { return defaultData().toString(); }
    }
    public static void save(SecurePrefs prefs, String json) throws Exception {
        String expected = validated(json).toString();
        prefs.setSecret(KEY, expected);
        String persisted = prefs.secret(KEY);
        if (!expected.equals(persisted))
            throw new IllegalStateException("Échec de l'enregistrement du profil actif. Vérifiez le stockage Android.");
    }
    /** Nom du profil réellement sélectionné et enregistré, pour la télémétrie. */
    public static String activeName(SecurePrefs prefs) {
        try {
            JSONObject root = new JSONObject(read(prefs));
            String activeId = root.getString("active");
            JSONArray profiles = root.getJSONArray("profiles");
            for (int i = 0; i < profiles.length(); i++) {
                JSONObject profile = profiles.getJSONObject(i);
                if (activeId.equals(profile.optString("id", "")))
                    return profile.getString("name");
            }
        } catch (Exception ignored) { }
        return "JARVIS";
    }
    public static String activePrompt(SecurePrefs prefs) {
        try {
            JSONObject root = new JSONObject(read(prefs));
            String id = root.getString("active");
            JSONArray arr = root.getJSONArray("profiles");
            for (int i=0; i<arr.length(); i++) {
                JSONObject p = arr.getJSONObject(i);
                if (id.equals(p.getString("id"))) return p.getString("prompt");
            }
        } catch (Exception ignored) { }
        return DEFAULT_PROMPT;
    }
}
