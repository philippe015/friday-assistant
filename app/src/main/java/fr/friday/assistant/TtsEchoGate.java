package fr.friday.assistant;

/**
 * Coupe la dictée Vosk générale pendant une réponse vocale et son écho résiduel.
 * Cette règle ne concerne pas le décodeur indépendant VoiceStopSpotter :
 * STOP et "j'ai une précision" restent reconnus pendant une réponse.
 *
 * Ne dépend ni de l'horloge Android ni du matériel : testable hors téléphone.
 */
public final class TtsEchoGate {
    public static final long TAIL_MS = 2500L;
    public static final long STOP_TAIL_MS = 900L;

    private TtsEchoGate() { }

    public static boolean suppressGeneralAsr(boolean responseActive, long nowMs, long untilMs) {
        return responseActive || nowMs < untilMs;
    }

    /** Première trame autorisée seulement après un délai sans haut-parleur. */
    public static long afterVoice(long nowMs) {
        return nowMs + TAIL_MS;
    }

    /** Après un STOP volontaire, libère rapidement la parole après l'écho du TTS. */
    public static long afterStop(long nowMs) {
        return nowMs + STOP_TAIL_MS;
    }
}
