package fr.friday.assistant;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayDeque;

/**
 * Récupère, uniquement sur le téléphone, le contexte de dialogue chiffré.
 * Ce texte n'est JAMAIS transmis aux moteurs de recherche ou à une API d'IA.
 * L'historique affichable reste dans PrivateConversationHistory.
 */
public final class DialogueContext {
    private DialogueContext() { }

    // Une nouvelle question ne doit pas ralentir le modèle 0,6 B en lui
    // donnant tout l'historique. Pour une vraie relance, récupérer davantage
    // de tours chiffrés mais respecter la fenêtre courte du modèle local.
    private static final int MAX_DIALOGUE_MESSAGES = 14;
    private static final int MAX_DIALOGUE_CHARACTERS = 2600;
    private static final int NORMAL_DIALOGUE_MESSAGES = 6;
    private static final int NORMAL_DIALOGUE_CHARACTERS = 1250;

    /** Évite d'utiliser deux fois la question que le service vient d'enregistrer. */
    private static int previousEnd(JSONArray events, String currentQuestion) {
        int end = events.length();
        if (end == 0) return end;
        JSONObject last = events.optJSONObject(end - 1);
        if (last != null && "user".equals(last.optString("kind"))) {
            String lastQuestion = VoiceText.normalize(last.optString("text", "")).trim();
            if (!lastQuestion.isEmpty() && lastQuestion.equals(
                    VoiceText.normalize(currentQuestion == null ? "" : currentQuestion).trim()))
                end--;
        }
        return end;
    }

    private static String clip(String text, int limit) {
        if (text == null) return "";
        // Les références visibles demeurent dans le journal chiffré, mais ne
        // doivent pas évincer les réponses elles-mêmes du contexte limité.
        String cleaned = text.replaceAll("(?s)\\n\\s*Sources Internet consultées\\s*:.*$", "")
                .replaceAll("\\s+", " ").trim();
        if (cleaned.length() <= limit) return cleaned;
        return cleaned.substring(0, limit - 1).trim() + "…";
    }

    /**
     * Conserve les tours récents en ordre chronologique, y compris après
     * redémarrage du service ou de l'application.
     */
    public static String recent(Context context, SecurePrefs prefs, String currentQuestion)
            throws Exception {
        JSONArray events = new JSONArray(PrivateConversationHistory.read(context, prefs));
        int end = previousEnd(events, currentQuestion);
        boolean followup = DialogueMemoryPolicy.isContextualFollowUp(currentQuestion)
                || DialogueMemoryPolicy.isPrivateDialogue(currentQuestion);
        int maxMessages = followup ? MAX_DIALOGUE_MESSAGES : NORMAL_DIALOGUE_MESSAGES;
        int maxCharacters = followup ? MAX_DIALOGUE_CHARACTERS
                : NORMAL_DIALOGUE_CHARACTERS;
        ArrayDeque<String> chosen = new ArrayDeque<>();
        int chars = 0;
        for (int i = end - 1; i >= 0 && chosen.size() < maxMessages; i--) {
            JSONObject obj = events.optJSONObject(i);
            if (obj == null) continue;
            String kind = obj.optString("kind", "");
            if (!"user".equals(kind) && !"assistant".equals(kind)) continue;
            String text = clip(obj.optString("text", ""), "user".equals(kind) ? 440 : 580);
            if (text.isEmpty()) continue;
            String line = ("user".equals(kind) ? "Utilisateur : " : "Jarvis : ")
                    + text;
            if (chars + line.length() + 1 > maxCharacters) {
                if (!chosen.isEmpty()) break;
                // Une seule réponse très longue ne doit pas effacer la
                // dernière intervention : clipper seulement cet extrait.
                int available = maxCharacters - chars - 6;
                if (available <= 15) break;
                line = line.substring(0, Math.min(available, line.length())) + "…";
            }
            chosen.addFirst(line);
            chars += line.length() + 1;
        }
        if (chosen.isEmpty()) return "";
        StringBuilder result = new StringBuilder(chars + 250);
        result.append("[CONTEXTE PRIVÉ DES ÉCHANGES PRÉCÉDENTS — TÉLÉPHONE UNIQUEMENT]\n");
        for (String line : chosen) result.append(line).append('\n');
        result.append("[FIN CONTEXTE PRIVÉ]\n");
        return result.toString();
    }

    /** Diagnostics factuels, sans prétendre que le modèle a tout retenu. */
    public static String memoryStatus(Context context, SecurePrefs prefs, String currentQuestion) {
        try {
            JSONArray events = new JSONArray(PrivateConversationHistory.read(context, prefs));
            int end = previousEnd(events, currentQuestion);
            int questions = 0, answers = 0;
            for (int i = 0; i < end; i++) {
                JSONObject obj = events.optJSONObject(i);
                if (obj == null) continue;
                if ("user".equals(obj.optString("kind"))) questions++;
                else if ("assistant".equals(obj.optString("kind"))) answers++;
            }
            if (questions == 0 && answers == 0)
                return "Je dispose d'un historique de dialogue chiffré sur ce téléphone, "
                        + "mais aucun échange précédent n'est disponible actuellement. "
                        + "Les notes personnelles enregistrées explicitement sont distinctes.";
            return "Oui. J'ai accès sur ce téléphone à " + questions
                    + " question(s) précédentes et " + answers
                    + " réponse(s) dans un historique chiffré. "
                    + "Je peux réutiliser les derniers échanges pour poursuivre "
                    + "et approfondir le même sujet, même après redémarrage. "
                    + "Ce contexte est limité : il ne garantit pas de retrouver "
                    + "toutes les conversations anciennes. Pour conserver un fait "
                    + "important plus durablement, dites « Jarvis, retiens que… ».";
        } catch (Exception unreadable) {
            return "Je ne peux pas lire mon historique local chiffré actuellement. "
                    + "Je ne vais pas prétendre me souvenir d'échanges inaccessibles. "
                    + "Vérifiez le stockage sécurisé Android et les réglages de mémoire.";
        }
    }
}
