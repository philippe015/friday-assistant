package fr.friday.assistant;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Détection stricte de météo demandée explicitement et nom de ville fourni.
 * Jamais de déduction de la position GPS ou de la ville personnelle.
 */
public final class WeatherQueryPolicy {
    private WeatherQueryPolicy() {}

    private static final Pattern WEATHER = Pattern.compile(
        "\\b(?:meteo|previsions? meteo|temperature|temperatures|"
      + "quel temps fait|quel temps va faire|temps qu il fait)\\b");
    private static final Pattern DESTINATION = Pattern.compile(
        "(?:^|\\s)(?:a|pour|sur|de|dans)\\s+"
      + "([a-z][a-z0-9-]{1,}(?:\\s+[a-z][a-z0-9-]{1,}){0,3}"
      + "(?:\\s*,\\s*[a-z][a-z-]{1,})?)$");
    private static final Pattern INVALID_CITY = Pattern.compile(
        "^(?:moi|nous|ma ville|mon village|mes alentours|chez moi|"
      + "la maison|ma position|proximite|ici|ma localisation|"
      + "quel endroit|quel pays|maintenant|aujourd hui|demain)$");

    public static String fold(String input) {
        return Normalizer.normalize(input == null ? "" : input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.FRENCH)
                .replaceAll("[^a-z0-9,-]+", " ").replaceAll("\\s+", " ").trim();
    }

    public static boolean isWeather(String question) {
        String text = fold(question);
        if (text.length() > 250) return false;
        return WEATHER.matcher(text).find();
    }

    /** Retourne null pour autre intention, "" pour météo sans ville. */
    public static String city(String question) {
        if (!isWeather(question)) return null;
        String text = fold(question)
                .replaceFirst("^(?:jarvis|jervis|friday|fraday)\\s+", "")
                .replaceAll("\\b(?:maintenant|aujourd hui|demain|ce soir|"
                        + "cet apres midi|ce matin)\\s*$", "").trim();
        Matcher match = DESTINATION.matcher(text);
        if (!match.find()) return "";
        String found = match.group(1).trim();
        if (found.length() < 3 || found.length() > 65
                || INVALID_CITY.matcher(found).matches()) return "";
        return found;
    }

    public static boolean tomorrow(String question) {
        return fold(question).matches(".*\\bdemain\\b.*");
    }
}
