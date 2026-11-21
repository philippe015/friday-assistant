package fr.friday.assistant;

/**
 * Startup microphone policy. Only MainActivity.onResume may call startListening:
 * no BOOT_COMPLETED receiver, no background microphone activation.
 */
public final class AutoListenPolicy {
    private AutoListenPolicy() { }

    /** Default-on for assistant-first behavior, reversible in Android settings. */
    public static boolean enabled(String stored) {
        return !"0".equals(stored);
    }

    /**
     * One automatic attempt per visible Activity instance. A user-initiated
     * Stop must not be undone by a delayed onResume or a pending UI callback.
     */
    public static boolean shouldStart(String setting, boolean attempted,
                                      boolean manuallyStopped, boolean active) {
        return enabled(setting) && !attempted && !manuallyStopped && !active;
    }
    /** Réessai au prochain onResume visible après un échec initial.
     * Pas de boucle rapide, pas de démarrage après Stop ou désactivation. */
    public static boolean retryOnResume(String setting, boolean manuallyStopped,
                                        boolean active, long lastAttempt, long nowMs) {
        return enabled(setting) && !manuallyStopped && !active
                && lastAttempt > 0 && nowMs >= lastAttempt
                && nowMs - lastAttempt >= 8000L;
    }
    /**
     * Watchdog uniquement lorsque l'activité est au premier plan.
     * Jamais depuis BOOT_COMPLETED, jamais après Stop, et anti-boucle.
     */
    public static boolean retryWhileVisible(String setting, boolean visible,
                                            boolean manuallyStopped, boolean active,
                                            long lastAttempt, long nowMs,
                                            int previousAttempts) {
        return enabled(setting) && visible && !manuallyStopped && !active
                && previousAttempts < 3 && lastAttempt > 0
                && nowMs >= lastAttempt && nowMs - lastAttempt >= 10_000L;
    }

}
