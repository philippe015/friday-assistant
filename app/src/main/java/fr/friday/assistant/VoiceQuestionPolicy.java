package fr.friday.assistant;

import java.util.regex.Pattern;

/**
 * Une fenêtre de dialogue sans mot-clé n'autorise pas les bribes Vosk sans
 * intention identifiable. Le mot-clé explicite conserve la liberté de parole.
 * Pas de stockage, d'IA ou d'analyse du microphone ici.
 */
public final class VoiceQuestionPolicy {
    private VoiceQuestionPolicy() {}

    private static final Pattern INTENT = Pattern.compile(
            "^(?:qui|quoi|que|quand|comment|pourquoi|ou|quel|quelle|quels|"
            + "quelles|combien|est ce que|qu est ce que|peux tu|"
            + "pourrais tu|dis moi|donne moi|cherche|recherche|trouve|"
            + "verifie|explique|raconte|parle moi|montre|calcule|"
            + "donne|quelle heure|actualites?|actus?|news|meteo|"
            + "informations?|infos?|date|heure|horaires?|"
            + "quelles sont|qu est|fais moi|"
            + "sais tu|sais tu me dire|je veux|je voudrais|"
            + "j aimerais|j ai besoin de|est il possible de|"
            + "fais|resume|definis|traduis)\\b.*");

    private static final Pattern FOLLOWUP = Pattern.compile(
            "^(?:et|mais|pour|non|plutot|avec|sans|demain|hier|"
            + "je voulais dire|je veux dire|en fait|a propos de|"
            + "c est|precise|donne plus|explique plus)\\b.*");

    public static boolean plausibleDirectQuestion(String phrase, boolean priorContext) {
        String folded = VoiceText.normalize(phrase).replaceAll("[^a-z0-9 ]+", " ")
                .replaceAll("\\s+", " ").trim();
        if (folded.isEmpty()) return false;
        int wordCount = folded.split(" ").length;
        if (wordCount < 2) return false;
        if (INTENT.matcher(folded).matches()) return true;
        if (priorContext && FOLLOWUP.matcher(folded).matches()) return true;
        // Une longue phrase déclarative n'est pas toujours une question ;
        // sans verbe de demande, demander le mot d'activation reste plus sûr.
        return false;
    }
}
