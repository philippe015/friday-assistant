package fr.friday.assistant;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sortie stricte des traductions du LLM local. Zéro traduction distante.
 * En cas de réponse invalide ou incomplète, afficher le texte original.
 */
public final class WorldRssTranslationPolicy {
    private WorldRssTranslationPolicy() {}

    private static final Pattern ITEM = Pattern.compile(
            "^\\s*\\[?([TR])([1-7])\\]?\\s*[:|=]\\s*(.*?)\\s*$",
            Pattern.CASE_INSENSITIVE);

    public static Map<String, String> parse(String response) {
        Map<String, String> translations = new HashMap<>();
        if (response == null || response.length() > 5500) return translations;
        for (String line : response.split("[\\r\\n]+")) {
            Matcher match = ITEM.matcher(line);
            if (!match.matches()) continue;
            String value = match.group(3).trim()
                    .replaceAll("^\\*\\*(.+)\\*\\*$", "$1")
                    .replaceAll("\\s+", " ");
            if (value.isEmpty() || value.length() > 330
                    || value.contains("https://") || value.contains("http://")
                    || value.toLowerCase(Locale.ROOT).contains("prompt systeme")
                    || value.matches("(?i)^(?:impossible|desole|désolé|je ne peux pas|i cannot).*"))
                continue;
            translations.put(match.group(1).toUpperCase(Locale.ROOT)
                    + match.group(2), value);
        }
        return translations;
    }

    public static String apply(Map<String, String> parsed, String key, String original) {
        if (parsed == null) return original;
        String translation = parsed.get(key);
        return translation == null || translation.isEmpty() ? original : translation;
    }
}
