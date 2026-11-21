package fr.friday.assistant;

/**
 * En mode assistant mains libres, une vraie question française clairement
 * reconnaissable peut être envoyée sans répéter « Jarvis » ni toucher l'écran.
 * Les bribes, réponses du haut-parleur et propos sans intention sont ignorés.
 * Cette règle ne démarre aucun micro : elle ne traite que la dictée reçue.
 */
public final class HandsFreeQuestionPolicy {
    private HandsFreeQuestionPolicy() { }

    public static boolean acceptsDirectQuestion(String heard, boolean enabled,
                                                boolean voiceSafe) {
        if (!enabled || !voiceSafe || heard == null) return false;
        String normalized = VoiceText.normalize(heard).replaceAll("[^a-z0-9 ]+", " ")
                .replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty()) return false;
        // « la météo » / « météo » sont des demandes autonomes :
        // Jarvis demandera le nom de la ville plutôt que rester silencieux.
        if (WeatherRequestPolicy.isWeatherRequest(heard)) return true;
        if (normalized.split(" ").length < 3) return false;
        return VoiceQuestionPolicy.plausibleDirectQuestion(heard, false);
    }
}
