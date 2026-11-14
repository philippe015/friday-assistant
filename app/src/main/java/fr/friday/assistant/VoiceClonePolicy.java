package fr.friday.assistant;

import java.util.List;
/** Paramètres indépendants d'Android ; testables sur Manjaro. */
public final class VoiceClonePolicy {
    public static final String MODEL = "voxtral-mini-tts-3-0";
    public static final String ENDPOINT = "https://api.mistral.ai/v1/audio/speech";
    public static final int SECONDS = 12;
    private VoiceClonePolicy() {}
    public static List<String> chunks(String text) { return VoiceText.ttsChunks(text, 340); }
    public static boolean hasConsent(boolean checkbox, boolean recorded) { return checkbox && recorded; }
    public static boolean isValidWavSize(long bytes) { return bytes >= 44 + 16000L * 2 * 5 && bytes <= 2_000_000L; }
}
