package fr.friday.assistant;

/** JNI Rust : recherche du mot d'activation dans une transcription Vosk. */
public final class RustWake {
    private static final boolean LOADED;
    static {
        boolean ok;
        try { System.loadLibrary("friday_core"); ok = true; }
        catch (UnsatisfiedLinkError err) { ok = false; }
        LOADED = ok;
    }
    private RustWake() {}
    private static native boolean hasWakeWord(String normalized);
    public static boolean detect(String text) {
        String normalized = VoiceText.normalize(text == null ? "" : text);
        // La bibliothèque historique peut encore accepter Friday : la règle
        // Java Jarvis-seul prime toujours sur le détecteur natif.
        if (!VoiceText.hasWakeWord(text)) return false;
        if (LOADED) {
            try { return hasWakeWord(normalized) || VoiceText.hasWakeWord(text); }
            catch (UnsatisfiedLinkError err) { /* Mode de secours Java. */ }
        }
        return VoiceText.hasWakeWord(text);
    }
}
