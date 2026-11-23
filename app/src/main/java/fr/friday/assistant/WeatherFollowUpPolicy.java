package fr.friday.assistant;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Relances météo sans fuite d'historique : seules les villes d'une réponse
 * météo publique déjà citée, ou la ville maintenant explicitement donnée,
 * peuvent devenir des termes de recherche.
 */
public final class WeatherFollowUpPolicy {
    private WeatherFollowUpPolicy() {}

    private static final Pattern VERIFIED_FORECAST = Pattern.compile(
        "^Prévisions météo pour ([\\p{L} -]{2,65}), ([\\p{L} -]{2,45}), "
      + "(?:aujourd'hui|demain) \\(");
    private static final Pattern CITY_REPLY = Pattern.compile(
        "^(?:pour\\s+)?([a-z][a-z0-9-]{2,}(?:\\s+[a-z][a-z0-9-]{2,}){0,2}"
      + "(?:\\s*,\\s*[a-z-]{2,})?)$");
    // The speaker may answer with a city AND the time requested by Jarvis:
    // « Vitry-sur-Seine ce soir ». This time is not part of the city name.
    private static final Pattern REPLY_PERIOD = Pattern.compile(
        "\\s+(?:ce soir|demain soir|demain matin|demain|ce matin|cet apres midi|aujourd hui)$");
    // ASR may split « Seine » into « scène »; never geocode a guessed city.
    private static final Pattern UNCERTAIN_CITY_VOSK = Pattern.compile(
        "^(?:(?:c est|c|ce|sur)\\s+){1,3}.*\\b(?:scene|seine|vitry|france)\\b.*$");

    private static final Pattern OTHER_CITY = Pattern.compile(
        "^et\\s+(?:pour|a|sur)\\s+([a-z][a-z0-9-]{2,}"
      + "(?:\\s+[a-z][a-z0-9-]{2,}){0,2}(?:\\s*,\\s*[a-z-]{2,})?)$");

    /** An uncertain city correction immediately after a local weather prompt
     * should request clarification instead of launching arbitrary web search.
     * Restricted to likely ASR deformation; ordinary new questions are free. */
    public static boolean uncertainCityAfterWeatherPrompt(
            String question, String previousAssistant) {
        if (previousAssistant == null
                || !previousAssistant.startsWith("Pour quelle ville souhaitez-vous la météo"))
            return false;
        String q = WeatherQueryPolicy.fold(question);
        if (q.length() > 65 || q.isEmpty()) return false;
        return UNCERTAIN_CITY_VOSK.matcher(q).matches();
    }

    /** Accept an unmistakable spoken city reply for a short, explicit
     * follow-up window. A one-word background sound NEVER counts as a place. */
    public static boolean clearSpokenCityReply(String question) {
        String text = WeatherQueryPolicy.fold(question);
        if (text.isEmpty() || text.length() > 75
                || WeatherQueryPolicy.isWeather(question)) return false;
        text = text.replaceFirst("^(?:jarvis|jervis|friday|fraday)\\s+", "");
        text = REPLY_PERIOD.matcher(text).replaceFirst("").trim();
        Matcher match = CITY_REPLY.matcher(text);
        if (!match.matches()) return false;
        String city = match.group(1).trim();
        if (city.matches("^(?:bonjour|merci|bonne|bonsoir|comment|pourquoi|qui|"
                + "quelle|quel|combien|ceci|cela|je|tu|nous|on|oui|non)\\b.*"))
            return false;
        // A city with a country/comma, hyphen, at least two place components,
        // or an explicit « pour ... » prefix is intentional enough to accept.
        return text.startsWith("pour ") || city.contains("-")
                || city.contains(",") || city.split("\\s+").length >= 2;
    }

    public static String enrich(String question, String previousAssistant) {
        String q = WeatherQueryPolicy.fold(question).replaceFirst(
                "^(?:jarvis|jervis|friday|fraday)\\s+", "").trim();
        if (q.isEmpty() || q.length() > 90 || previousAssistant == null)
            return null;
        // A complete explicit weather question belongs to the regular tool,
        // not to a city-only follow-up. Avoid « météo pour météo pour Paris ».
        if (WeatherQueryPolicy.isWeather(q)) return null;
        // Réponse à la question directe « Pour quelle ville ? »
        if (previousAssistant.startsWith("Pour quelle ville souhaitez-vous la météo")) {
            Matcher period = REPLY_PERIOD.matcher(q);
            boolean hasPeriod = period.find();
            String periodText = hasPeriod ? period.group().trim() : "";
            String cityOnly = hasPeriod ? period.replaceFirst("").trim() : q;
            Matcher city = CITY_REPLY.matcher(cityOnly);
            if (city.matches() && !city.group(1).matches(
                    "(?i)(merci|bonjour|aujourd hui|demain|oui|non|je sais|chez moi)"))
                return "météo " + (periodText.isEmpty() ? "" : periodText + " ")
                        + "pour " + city.group(1);
            return null;
        }
        if (!previousAssistant.contains("Source : Open-Meteo"))
            return null;
        Matcher forecast = VERIFIED_FORECAST.matcher(previousAssistant);
        if (!forecast.find()) return null;
        String lastCity = forecast.group(1) + ", " + forecast.group(2);
        Matcher other = OTHER_CITY.matcher(q);
        if (other.matches()) return "météo pour " + other.group(1);
        if (q.matches("^(?:et\\s+)?demain(?:\\s+(?:alors|pour cette ville))?$"))
            return "météo demain pour " + lastCity;
        if (q.matches("^(?:et\\s+)?aujourd hui(?:\\s+(?:alors|pour cette ville))?$"))
            return "météo aujourd'hui pour " + lastCity;
        return null;
    }
}
