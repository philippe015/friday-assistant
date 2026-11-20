package fr.friday.assistant;

/**
 * Autorise explicitement une nouvelle question lorsqu'une réponse existe :
 * - pendant une génération SILENCIEUSE, le mot-clé peut remplacer la demande ;
 * - pendant la voix TTS, seul le réglage d'interruption l'autorise ;
 * - une transcription sans nom confirmé ou un écho est toujours rejeté.
 * Règle pure, sans microphone ni réseau.
 */
public final class VoiceTurnPolicy {
    private VoiceTurnPolicy() {}

    public static boolean canReplace(boolean responseActive, boolean speakingNow,
                                     boolean bargeEnabled, boolean confirmedWakeNotEcho) {
        return responseActive && confirmedWakeNotEcho && (!speakingNow || bargeEnabled);
    }
}
