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
}
