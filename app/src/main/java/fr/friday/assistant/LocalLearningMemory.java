package fr.friday.assistant;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.UUID;

/**
 * JARVIS Memory Evolution+ — encrypted user-approved learning, schema-compatible
 * with v1. Conservative paraphrase retrieval; conflicting corrections are
 * quarantined pending an explicit choice on the phone.
 *
 * No embeddings, model training, Internet, auto-upload or plaintext fallback.
 */
public final class LocalLearningMemory {
    private LocalLearningMemory() { }
    private static final String KEY = "jarvis_learning_v1"; // preserve existing encrypted records
    private static final int MAX_ENTRIES = 100;

    public static boolean enabled(SecurePrefs prefs) {
        return "1".equals(prefs.get("local_learning_enabled", "0"));
    }

    public static void setEnabled(SecurePrefs prefs, boolean enabled) {
        prefs.put("local_learning_enabled", enabled ? "1" : "0");
    }

    private static JSONArray read(SecurePrefs prefs) throws Exception {
        if (!prefs.hasPrivateSecret(KEY)) return new JSONArray();
        String raw = prefs.privateSecret(KEY); // fails closed on Keystore errors
        if (raw.isEmpty()) return new JSONArray();
        JSONArray records = new JSONArray(raw);
        if (records.length() > MAX_ENTRIES)
            throw new IllegalStateException("Mémoire d'apprentissage anormalement volumineuse");
        // Migrate original 40-entry format without deleting existing corrections.
        boolean upgraded = false;
        for (int i = 0; i < records.length(); i++) {
            JSONObject item = records.getJSONObject(i);
            if (!item.has("question") || !item.has("correction"))
                throw new IllegalStateException("Entrée de mémoire incomplète");
            if (item.optString("id").isEmpty()) {
                item.put("id", UUID.randomUUID().toString());
                upgraded = true;
            }
        }
        if (upgraded) prefs.putPrivateSecret(KEY, records.toString());
        return records;
    }

    private static int findBest(JSONArray records, String question) {
        int best = -1;
        double bestScore = 0.0;
        double second = 0.0;
        for (int i = 0; i < records.length(); i++) {
            JSONObject item = records.optJSONObject(i);
            if (item == null) continue;
            String old = item.optString("question");
            double score = LocalLearningPolicy.sameQuestion(old, question)
                    ? 1.0 : LocalSemanticMemoryPolicy.similarity(old, question);
            if (score > bestScore) {
                second = bestScore; bestScore = score; best = i;
            } else if (score > second) {
                second = score;
            }
        }
        // Ambiguous multiple paraphrases must NOT receive an arbitrary answer.
        return bestScore >= 0.86 && (second < 0.86 || bestScore - second >= 0.09)
                ? best : -1;
    }

    /** Feedback votes do not themselves train the model or override facts. */
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
            if (note == null)
                return "Correction invalide : saisissez entre 5 et 600 caractères.";
        }
        JSONArray records = read(prefs);
        int index = findBest(records, asked);
        JSONObject item = index < 0 ? new JSONObject()
                .put("id", UUID.randomUUID().toString()).put("question", asked)
                .put("correction", "") : records.getJSONObject(index);
        if (index < 0) {
            if (records.length() >= MAX_ENTRIES)
                return "Mémoire pleine (" + MAX_ENTRIES
                        + "). Supprimez d'abord une correction ancienne.";
            records.put(item);
        }
        item.put("good", Math.min(999, item.optInt("good", 0)
                + ("good".equals(vote) ? 1 : 0)));
        item.put("bad", Math.min(999, item.optInt("bad", 0)
                + ("bad".equals(vote) ? 1 : 0)));
        String status;
        if ("correction".equals(vote)) {
            String prior = item.optString("correction", "");
            if (!prior.isEmpty() && !LocalSemanticMemoryPolicy.sameCorrection(prior, note)) {
                // Never overwrite or surface contradictory knowledge as trusted.
                item.put("pending_correction", note);
                item.put("pending_question", asked);
                status = "Conflit détecté : ancienne et nouvelle corrections "
                        + "suspendues. Choisissez laquelle conserver dans la mémoire.";
            } else {
                item.put("correction", note);
                item.remove("pending_correction");
                item.remove("pending_question");
                status = "Correction chiffrée enregistrée pour cette question "
                        + "et ses reformulations proches.";
            }
        } else {
            status = "good".equals(vote)
                    ? "Réponse évaluée positivement et comptabilisée en local."
                    : "Réponse évaluée négativement. Proposez une correction.";
        }
        item.put("updated_ms", System.currentTimeMillis());
        prefs.putPrivateSecret(KEY, records.toString());
        return status;
    }

    /** Never inject unresolved or ambiguously matched notes into a prompt. */
    public static synchronized String promptHint(SecurePrefs prefs, String question) {
        if (!enabled(prefs) || question == null || question.length() > 260) return "";
        try {
            JSONArray records = read(prefs);
            int best = findBest(records, question);
            if (best < 0) return "";
            JSONObject item = records.getJSONObject(best);
            if (!item.optString("pending_correction", "").isEmpty()) return "";
            String note = item.optString("correction", "");
            if (note.isEmpty()) return "";
            return LocalLearningPolicy.memoryInstruction(note);
        } catch (Exception unavailable) {
            // Do not fall back to an unencrypted local file or external model.
            return "";
        }
    }

    /** Structured local UI listing. The Android WebView is bundled with the APK. */
    public static synchronized String listJson(SecurePrefs prefs) throws Exception {
        JSONArray records = read(prefs);
        JSONArray out = new JSONArray();
        for (int i = records.length() - 1; i >= 0; i--) {
            JSONObject item = records.getJSONObject(i);
            out.put(new JSONObject()
                    .put("id", item.optString("id"))
                    .put("question", item.optString("question"))
                    .put("correction", item.optString("correction"))
                    .put("pendingCorrection", item.optString("pending_correction"))
                    .put("pendingQuestion", item.optString("pending_question"))
                    .put("good", item.optInt("good", 0))
                    .put("bad", item.optInt("bad", 0)));
        }
        return out.toString();
    }

    public static synchronized String resolve(SecurePrefs prefs, String id,
                                               boolean acceptNew) throws Exception {
        if (id == null || !id.matches("[a-fA-F0-9-]{36}"))
            return "Identifiant de correction invalide.";
        JSONArray records = read(prefs);
        for (int i = 0; i < records.length(); i++) {
            JSONObject item = records.getJSONObject(i);
            if (!id.equals(item.optString("id"))) continue;
            String pending = item.optString("pending_correction", "");
            if (pending.isEmpty()) return "Aucun conflit à résoudre sur cette entrée.";
            if (acceptNew) {
                item.put("correction", pending);
                String proposedQuestion = item.optString("pending_question", "");
                if (!proposedQuestion.isEmpty()) item.put("question", proposedQuestion);
            }
            item.remove("pending_correction");
            item.remove("pending_question");
            item.put("updated_ms", System.currentTimeMillis());
            prefs.putPrivateSecret(KEY, records.toString());
            return acceptNew
                    ? "Conflit résolu : la nouvelle correction est approuvée."
                    : "Conflit résolu : l'ancienne correction est conservée.";
        }
        return "Correction introuvable.";
    }

    public static synchronized String delete(SecurePrefs prefs, String id) throws Exception {
        if (id == null || !id.matches("[a-fA-F0-9-]{36}"))
            return "Identifiant de correction invalide.";
        JSONArray records = read(prefs);
        for (int i = 0; i < records.length(); i++) {
            if (id.equals(records.getJSONObject(i).optString("id"))) {
                records.remove(i);
                if (records.length() == 0) prefs.removePrivateSecret(KEY);
                else prefs.putPrivateSecret(KEY, records.toString());
                return "Correction supprimée du stockage chiffré.";
            }
        }
        return "Correction introuvable.";
    }

    public static synchronized String status(SecurePrefs prefs) {
        if (!enabled(prefs)) return "Apprentissage local désactivé · aucun envoi Web.";
        try {
            JSONArray records = read(prefs);
            int good = 0, bad = 0, corrections = 0, conflicts = 0;
            for (int i = 0; i < records.length(); i++) {
                JSONObject item = records.getJSONObject(i);
                good += item.optInt("good", 0);
                bad += item.optInt("bad", 0);
                if (!item.optString("correction", "").isEmpty()) corrections++;
                if (!item.optString("pending_correction", "").isEmpty()) conflicts++;
            }
            return "Memory Evolution+ · " + corrections + " corrections chiffrées ("
                    + records.length() + "/" + MAX_ENTRIES + " entrées), "
                    + conflicts + " conflit(s) à résoudre · "
                    + good + " avis utiles, " + bad + " avis négatifs. "
                    + "Aucun réentraînement de modèle ni synchronisation.";
        } catch (Exception unavailable) {
            return "Mémoire d'apprentissage chiffrée indisponible : aucun stockage en clair.";
        }
    }

    public static synchronized String clear(SecurePrefs prefs) throws Exception {
        prefs.removePrivateSecret(KEY);
        return "Toutes les évaluations et corrections d'apprentissage ont été effacées.";
    }
}
