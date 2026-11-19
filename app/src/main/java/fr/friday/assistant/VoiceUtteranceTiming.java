package fr.friday.assistant;

/**
 * Détection de fin de dictée : ne coupe pas une phrase encore grammaticalement
 * incomplète lors d'une petite pause. Sans audio, sans réseau ni stockage.
 */
public final class VoiceUtteranceTiming {
    private VoiceUtteranceTiming() { }

    public static int timeoutMs(String completed, String partial, int configuredMs,
                                boolean interruptingResponse) {
        int base = Math.max(900, Math.min(6000, configuredMs));
        // Une commande STOP explicite pendant le TTS ne doit pas patienter.
        if (interruptingResponse) return Math.min(base, 1300);
        String words = VoiceText.normalize(VoiceText.join(completed, partial))
                .replaceAll("[^a-z0-9 ]+", " ").replaceAll("\\s+", " ").trim();
        if (words.isEmpty()) return base;
        // "Friday, cherche les dernières actualités sur..." : la phrase continue.
        if (words.matches(".*\\b(?:et|ou|de|du|des|le|la|les|un|une|pour|sur|dans|avec|"
                + "que|qui|dont|parce|par|a|au|aux|en|vers|chez|concernant|"
                + "ainsi|ensuite)\\b$"))
            return Math.min(6000, base + 1000);
        // Les longues questions contiennent naturellement plus de pauses.
        if (words.split(" ").length >= 10)
            return Math.min(6000, base + 450);
        return base;
    }
}
