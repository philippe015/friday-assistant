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
    private static final Pattern OTHER_CITY = Pattern.compile(
        "^et\\s+(?:pour|a|sur)\\s+([a-z][a-z0-9-]{2,}"
      + "(?:\\s+[a-z][a-z0-9-]{2,}){0,2}(?:\\s*,\\s*[a-z-]{2,})?)$");

    public static String enrich(String question, String previousAssistant) {
        String q = WeatherQueryPolicy.fold(question).replaceFirst(
                "^(?:jarvis|jervis|friday|fraday)\\s+", "").trim();
        if (q.isEmpty() || q.length() > 90 || previousAssistant == null)
            return null;
        // Réponse à la question directe « Pour quelle ville ? »
        if (previousAssistant.startsWith("Pour quelle ville souhaitez-vous la météo")) {
            Matcher city = CITY_REPLY.matcher(q);
            if (city.matches() && !city.group(1).matches(
                    "(?i)(merci|bonjour|aujourd hui|demain|oui|non|je sais|chez moi)"))
                return "météo pour " + city.group(1);
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
