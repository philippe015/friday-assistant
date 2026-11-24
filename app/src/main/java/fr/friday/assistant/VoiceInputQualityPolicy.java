package fr.friday.assistant;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Very conservative guard for long Vosk hallucinations before ANY search,
 * history write or speech synthesis. Never used on typed input.
 *
 * A low confidence score is handled separately by the existing Kotlin
 * confidence filter; an absent confidence score is not fabricated.
 */
public final class VoiceInputQualityPolicy {
    private VoiceInputQualityPolicy() {}

    private static final Pattern CLEAR_INTENT_START = Pattern.compile(
            "^(?:(?:jarvis|jarvise|jervis|friday|fraday)\\s+)?"
          + "(?:donne(?:z)?|cherche(?:z)?|recherche(?:z)?|trouve(?:z)?|"
          + "explique(?:z)?|decris|decrivez|lis|lisez|ouvre(?:z)?|"
          + "montre(?:z)?|raconte(?:z)?|resume(?:z)?|traduis|traduisez|"
          + "quelle?s?|quels?|comment|pourquoi|combien|ou|quand|"
          + "est ce que|peux tu|pourrais tu|pouvez vous|"
          + "je voudrais|j aimerais|je veux|sais tu|tu peux)\\b");

    public static String fold(String text) {
        return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.FRENCH)
                .replaceAll("[^a-z0-9 ]+", " ").replaceAll("\\s+", " ").trim();
    }

    /**
     * If ASR returns a very long, unstructured utterance without any
     * identifiable request, ask to repeat rather than spend 30-45 s on Web.
     * Does not guess whether the user deliberately dictated long prose.
     */
    public static boolean requestRepeat(String recognized) {
        if (recognized == null) return false;
        String q = fold(recognized);
        if (q.isEmpty()) return false;
        int words = q.split(" ").length;
        if (words < 62 || q.length() < 360) return false;
        String opening = q.substring(0, Math.min(115, q.length()));
        if (CLEAR_INTENT_START.matcher(opening).find()) return false;
        // An explicit request later in a very long sentence is still too
        // ambiguous to choose an external search; request a shorter question.
        return true;
    }

    public static String clarification() {
        return "J'ai entendu une longue phrase difficile à interpréter. "
                + "Pouvez-vous reformuler votre demande en une question courte ? "
                + "Je n'ai lancé aucune recherche Internet.";
    }
}
