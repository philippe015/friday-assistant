package fr.friday.assistant;

import java.text.Normalizer;
import java.util.Locale;

/** Route FRIDAY's on-device Jarvis without relying on any desktop or network. */
public final class LocalJarvisPolicy {
    private LocalJarvisPolicy() { }

    private static String fold(String question) {
        if (question == null) return "";
        return Normalizer.normalize(question.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ").trim();
    }

    public static boolean explicit(String question) {
        String normalized = fold(question)
                .replaceFirst("^(?:friday|fraday|frida|freiday|freday|freddy|fredi)[\\s,.:;!?-]*", "");
        return normalized.matches("^(?:jarvis|open ?jarvis|jarvis local|ia locale)(?:\\b.*)?")
                || normalized.matches("^(?:demande|pose la question) (?:a |au )?(?:jarvis|open ?jarvis)\\b.*");
    }

    public static boolean shouldRoute(String question, boolean enabled,
                                      boolean allQuestions) {
        return enabled && question != null && !question.trim().isEmpty()
                && (allQuestions || explicit(question));
    }

    public static String cleanQuestion(String question) {
        if (question == null) return "";
        String q = question.trim()
                .replaceFirst("(?iu)^(?:friday|fraday|frida|freiday|freday|freddy|fredi|jarvis|jarvise|jervis|jervi)[\\s,.:;!?-]*", "");
        q = q.replaceFirst("(?iu)^(?:demande|pose la question) (?:à |a |au )?(?:open ?)?jarvis[\\s,.:;!?-]*", "");
        q = q.replaceFirst("(?iu)^(?:open ?jarvis|jarvis local|jarvis|ia locale)[\\s,.:;!?-]*", "");
        return q.trim();
    }

    public static String state(boolean enabled, boolean modelReady) {
        if (!enabled) return "Jarvis local désactivé";
        if (!modelReady) return "Jarvis local : modèle absent ou non importé";
        return "Jarvis local sur ce téléphone, sans ordinateur";
    }
}
