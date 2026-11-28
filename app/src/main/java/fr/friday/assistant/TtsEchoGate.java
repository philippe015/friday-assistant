package fr.friday.assistant;

/**
 * Coupe la dictée Vosk uniquement quand le haut-parleur restitue une voix
 * et pendant la queue d'écho. NE PAS lier l'interdiction à un appel réseau
 * Mistral encore en attente : l'utilisateur doit pouvoir dire "Friday"
 * pour interrompre une recherche qui ne produit aucun son.
 *
 * L'interruption vocale est désactivée par FRIDAY ; l'utilisateur dispose
 * du bouton manuel pendant la lecture de la réponse.
 *
 * Ne dépend ni de l'horloge Android ni du matériel : testable hors téléphone.
 */
public final class TtsEchoGate {
    public static final long TAIL_MS = 2500L;
    public static final long STOP_TAIL_MS = 900L;

    private TtsEchoGate() { }

    public static boolean suppressGeneralAsr(boolean voicePlaybackActive, long nowMs, long untilMs) {
        return voicePlaybackActive || nowMs < untilMs;
    }

    /** Décodeur STOP spécial uniquement quand la synthèse se fait entendre. */
    public static boolean useStopRecognizer(boolean voicePlaybackActive, boolean enabled) {
        return voicePlaybackActive && enabled;
    }

    /** Un tour réseau bloqué peut être relancé seulement s'il ne parle pas. */
    public static boolean staleNetworkTurn(boolean turnActive, boolean voicePlaybackActive,
                                          long nowMs, long turnStartedMs, long limitMs) {
        return turnActive && !voicePlaybackActive && turnStartedMs > 0 && limitMs > 0
                && nowMs >= turnStartedMs && (nowMs - turnStartedMs) >= limitMs;
    }

    /** Première trame autorisée seulement après un délai sans haut-parleur. */
    public static long afterVoice(long nowMs) {
        return nowMs + TAIL_MS;
    }

    /** Fenêtre de dialogue vocal après une réponse.
     * Le délai commence APRÈS le blocage anti-écho : le microphone continue
     * à écouter, sans redire Friday, pendant 20 secondes en mode continu.
     * Le mode une question choisi explicitement conserve son arrêt.
     * Le calcul est hors Android, pour les tests de non-régression. */
    public static long followupUntil(long finishedAtMs, boolean oneShot) {
        return oneShot ? 0L : afterVoice(finishedAtMs) + 20_000L;
    }

    public static boolean followupAccepts(long nowMs, long echoBlockUntilMs,
                                          long followupUntilMs) {
        return nowMs >= echoBlockUntilMs && nowMs < followupUntilMs;
    }

    /** Après un STOP volontaire, libère rapidement la parole après l'écho du TTS. */
    public static long afterStop(long nowMs) {
        return nowMs + STOP_TAIL_MS;
    }
}
