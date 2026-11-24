package fr.friday.assistant;

import java.util.Locale;

/**
 * Six synthetic turns for an explicitly started ON-DEVICE continuity test.
 * No conversation, contacts, profile, history, location or Web access.
 */
public final class DialogueTestPolicy {
    private DialogueTestPolicy() {}

    private static final String[] QUESTIONS = {
        "Je crée une application Android pour suivre mes plantes d'intérieur. Elle doit fonctionner sans Internet. Résume le projet en une phrase.",
        "Quel premier écran proposerais-tu pour cette application ?",
        "Pourquoi ce choix ?",
        "Donne une fonctionnalité supplémentaire adaptée à ce même projet.",
        "Quel problème pratique peut survenir quand on utilise l'application sans connexion ?",
        "Résume nos décisions depuis le début en distinguant ce que nous avons décidé de ce que tu as seulement suggéré."
    };

    public static int count() { return QUESTIONS.length; }
    public static String question(int index) { return QUESTIONS[index]; }

    public static String evaluation(int index, String response) {
        if (response == null || response.trim().isEmpty()) return "SANS_REPONSE";
        String normalized = ConversationTurnPolicy.fold(response);
        if (index == 0 || index == 1 || index == 3) {
            return normalized.contains("plante") || normalized.contains("vegetal")
                    ? "INDICE_THEME_REPRIS" : "A_VERIFIER";
        }
        if (index == 4) {
            return normalized.contains("connexion") || normalized.contains("ligne")
                    || normalized.contains("synchronis")
                    ? "INDICE_CONTEXTE_REPRIS" : "A_VERIFIER";
        }
        return "LECTURE_MANUELLE";
    }

    /** Raw prompt never includes real user history or model preferences. */
    public static String prefix() {
        return "Banc d'essai local de six tours sur un projet FICTIF. "
             + "Réponds en français à la dernière demande. "
             + "Appuie-toi seulement sur cette conversation de test ; "
             + "ne consulte ni Internet ni mémoire personnelle. "
             + "Les réponses précédentes peuvent être imparfaites. "
             + "N'invente aucune décision de l'utilisateur. "
             + "Si une référence est ambiguë, demande une clarification. "
             + "Réponse concise (une à quatre phrases).\n";
    }

    public static String appendTurn(String history, int index, String answer) {
        String response = answer == null ? "" : answer.replaceAll("\\s+", " ").trim();
        if (response.length() > 270) response = response.substring(0, 270) + "…";
        return history + "Utilisateur test : " + question(index) + "\n"
                + "Jarvis test : " + response + "\n";
    }

    public static String prompt(String history, int index) {
        return prefix() + history + "\nUtilisateur test : " + question(index)
                + "\nJarvis test :";
    }
}
