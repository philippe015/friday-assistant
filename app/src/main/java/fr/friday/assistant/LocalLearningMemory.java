package fr.friday.assistant;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * On-device learning v1: user-confirmed corrections and coarse feedback.
 *
 * All learned content is encrypted using SecurePrefs/AES-GCM Android Keystore.
 * There is no automatic model replacement, silent upload, executable code,
 * remote synchronization, or model-weight training.
 */
public final class LocalLearningMemory {
    private LocalLearningMemory() { }
    private static final String KEY = "jarvis_learning_v1";
    private static final int MAX_ENTRIES = 40;

    public static boolean enabled(SecurePrefs prefs) {
        return "1".equals(prefs.get("local_learning_enabled", "0"));
    }

    public static void setEnabled(SecurePrefs prefs, boolean enabled) {
        prefs.put("local_learning_enabled", enabled ? "1" : "0");
    }

    private static JSONArray read(SecurePrefs prefs) throws Exception {
        if (!prefs.hasPrivateSecret(KEY)) return new JSONArray();
        String serialized = prefs.privateSecret(KEY);
        return serialized.isEmpty() ? new JSONArray() : new JSONArray(serialized);
    }

    /**
     * Accepted votes: good, bad, correction.
     * A thumbs-up/down does NOT train the model; only an explicit correction
     * can be offered as a bounded, unverified hint for the same next question.
     */
    public static synchronized String record(SecurePrefs prefs, String question,
                                             String vote, String correction)
            throws Exception {
        if (!enabled(prefs)) return "Activez d'abord l'apprentissage local.";
        String asked = question == null ? "" : question.trim();
        if (asked.length() < 4 || asked.length() > 260)
            return "Question trop courte ou trop longue pour une correction ciblée.";
        if (!("good".equals(vote) || "bad".equals(vote) || "correction".equals(vote)))
            return "Type d'évaluation invalide.";
        String note = "";
        if ("correction".equals(vote)) {
            note = LocalLearningPolicy.validCorrection(correction);
            if (note == null) return "Correction invalide : saisissez entre 5 et 600 caractères.";
        }
        JSONArray records = read(prefs);
        JSONObject entry = null;
        int prior = -1;
        for (int i = records.length() - 1; i >= 0; i--) {
            JSONObject candidate = records.optJSONObject(i);
            if (candidate != null
                    && LocalLearningPolicy.sameQuestion(candidate.optString("question"), asked)) {
                entry = candidate;
                prior = i;
                break;
            }
        }
        if (entry == null) entry = new JSONObject().put("question", asked);
        else records.remove(prior); // refresh the entry without duplicating it
        entry.put("good", Math.min(999, entry.optInt("good", 0)
                + ("good".equals(vote) ? 1 : 0)));
        entry.put("bad", Math.min(999, entry.optInt("bad", 0)
                + ("bad".equals(vote) ? 1 : 0)));
        if ("correction".equals(vote)) entry.put("correction", note);
        entry.put("updated_ms", System.currentTimeMillis());
        records.put(entry);
        while (records.length() > MAX_ENTRIES) records.remove(0);
        prefs.putPrivateSecret(KEY, records.toString()); // never plaintext
        return "correction".equals(vote)
                ? "Correction chiffrée enregistrée pour cette question."
                : "good".equals(vote)
                ? "Réponse évaluée positivement et comptabilisée en local."
                : "Réponse évaluée négativement. Proposez une correction pour progresser.";
    }

    /** Only from the Android local model prompt, never the Web query path. */
    public static synchronized String promptHint(SecurePrefs prefs, String question) {
        if (!enabled(prefs) || question == null || question.length() > 260) return "";
        try {
            JSONArray records = read(prefs);
            for (int i = records.length() - 1; i >= 0; i--) {
                JSONObject entry = records.optJSONObject(i);
                if (entry != null
                        && LocalLearningPolicy.sameQuestion(entry.optString("question"), question)) {
                    return LocalLearningPolicy.memoryInstruction(entry.optString("correction"));
                }
            }
        } catch (Exception unreadable) {
            // Fail closed: no plaintext fallback and no interruption of inference.
        }
        return "";
    }

    public static synchronized String status(SecurePrefs prefs) {
        if (!enabled(prefs)) return "Apprentissage local désactivé · aucun envoi Web.";
        try {
            JSONArray records = read(prefs);
            int good = 0, bad = 0, corrections = 0;
            for (int i = 0; i < records.length(); i++) {
                JSONObject item = records.optJSONObject(i);
                if (item == null) continue;
                good += item.optInt("good", 0);
                bad += item.optInt("bad", 0);
                if (!item.optString("correction").isEmpty()) corrections++;
            }
            return "Apprentissage local activé · " + good + " avis utiles, "
                    + bad + " avis négatifs, " + corrections + " corrections chiffrées ("
                    + records.length() + "/" + MAX_ENTRIES + " questions). "
                    + "Pas de réentraînement des poids ni de synchronisation.";
        } catch (Exception unreadable) {
            return "Mémoire d'apprentissage chiffrée indisponible : aucune donnée en clair.";
        }
    }

    public static synchronized String clear(SecurePrefs prefs) throws Exception {
        prefs.removePrivateSecret(KEY);
        return "Toutes les évaluations et corrections d'apprentissage ont été effacées.";
    }
}
