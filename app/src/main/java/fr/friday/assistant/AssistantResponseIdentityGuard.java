package fr.friday.assistant;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Guard against a small on-device model announcing its weight/model name as
 * the application's identity (e.g. "Je suis Gemma 4").
 *
 * Does not deny the underlying model: direct questions ABOUT a model are
 * untouched. Only a self-identification at the very beginning of a freeform
 * response is corrected to the configured assistant persona.
 */
public final class AssistantResponseIdentityGuard {
    private AssistantResponseIdentityGuard() {}

    private static final Pattern MODEL_SELF_INTRO = Pattern.compile(
        "(?iu)^\\s*(?:bonjour[.!]\\s*)?"
      + "(?:je\\s+suis|je\\s+m['’]appelle|mon\\s+nom\\s+est)\\s+"
      + "(?:gemma|mistral|qwen|deepseek|chatgpt)"
      + "(?:\\s+[0-9][a-z0-9.\\-]*)?"
      + "(?:\\s*[,;:]\\s*[^.!?\\n]{0,110})?"
      + "[.!?](?:\\s*|$)");

    private static final Pattern TECHNICAL_MODEL_QUESTION = Pattern.compile(
        "\\b(?:gemma|mistral|qwen|deepseek|chatgpt|modele|moteur|"
      + "quelle ia|quelle intelligence artificielle)\\b");

    private static String normalize(String raw) {
        return Normalizer.normalize(raw == null ? "" : raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.FRENCH)
                .replaceAll("[^a-z0-9]+", " ").trim();
    }

    public static String correct(String question, String response, String assistantName) {
        if (response == null || response.isEmpty()) return response;
        if (TECHNICAL_MODEL_QUESTION.matcher(normalize(question)).find())
            return response;
        Matcher opening = MODEL_SELF_INTRO.matcher(response);
        if (!opening.find()) return response;
        String name = assistantName == null || assistantName.trim().isEmpty()
                ? "F.R.I.D.A.Y." : assistantName.trim();
        // User-controlled profile names are rendered as text, not instructions.
        if (name.length() > 80) name = name.substring(0, 80);
        String rest = response.substring(opening.end()).trim();
        return "Je suis " + name + ", votre assistant personnel."
                + (rest.isEmpty() ? "" : " " + rest);
    }
}
