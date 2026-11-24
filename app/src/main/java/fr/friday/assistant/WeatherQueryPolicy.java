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
      + "quel temps fait|quel temps va faire|temps qu il fait|"
      + "la metaux (?:(?:sur|dans|en|pour|a)))\\b");
    // « Météo Joinville-le-Pont 94 » contient un numéro départemental,
    // non une partie du nom de la commune.
    private static final Pattern DESTINATION = Pattern.compile(
            "^([a-z][a-z-]{1,}(?:\\s+[a-z][a-z-]{1,}){0,4})"
          + "(?:\\s+(\\d{2}|\\d{5}))?"
          + "(?:\\s*,\\s*([a-z][a-z -]{1,}))?$");
    private static final Pattern PERIOD = Pattern.compile(
            "\\b(?:maintenant|aujourd hui|demain soir|demain|ce soir|cet apres midi|ce matin)\\b");

    private static final Pattern INVALID_CITY = Pattern.compile(
        "^(?:moi|nous|ma ville|mon village|mes alentours|chez moi|"
      + "la maison|ma position|proximite|ici|ma localisation|"
      + "quel endroit|quel pays|maintenant|aujourd hui|demain|journee|la journee|"
      + "toute la journee|dans la journee|pour la journee|la matinee|le matin|"
      + "le soir|en ce moment|les prochaines heures|"
      + "(?:le )?temps que fait il|(?:le )?temps qu il fait|"
      + "quel temps fait il|quelle temperature|quel temps|"
      + "le temps qu il fait)$");

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

    /** Retourne null hors météo, "" si le lieu n'est pas fourni. */
    public static String city(String question) {
        if (!isWeather(question)) return null;
        String normalized = fold(question)
                .replaceFirst("^(?:jarvis|jervis|friday|fraday)\\s+", "");
        Matcher weather = WEATHER.matcher(normalized);
        if (!weather.find()) return "";
        String tail = normalized.substring(weather.end());
        // La ville peut précéder « la météo » : « Joinville le pont France la météo ».
        // Ne jamais prendre « la journée » ou « France » pour une nouvelle ville.
        if (tail.trim().isEmpty()) {
            tail = normalized.substring(0, weather.start()).trim()
                    .replaceFirst("\\s+(?:la|de la|pour la)$", "")
                    .replaceFirst("^(?:bonjour|salut)\\s+", "").trim();
        }
        tail = PERIOD.matcher(tail).replaceAll(" ")
                .replaceAll("\\s+", " ").trim()
                .replaceFirst("^-?il\\s+", "")
                .replaceFirst("^(?:(?:a|pour|sur|de|dans)\\s+)+", "")
                .replaceFirst("^(?:du jour|de la journee)\\s+", "")
                .replaceFirst("\\s+(?:pour )?la journee$", "")
                .trim();
        if (!tail.contains(","))
            tail = tail.replaceFirst("\\s+(france|belgique|suisse|canada)$", ", $1");
        if (tail.matches("^(?:pour|a|sur|de|dans|en|il)$")) return "";
        Matcher match = DESTINATION.matcher(tail);
        if (!match.matches()) return "";
        String name = match.group(1).trim();
        if (name.length() < 3 || name.length() > 65
                || INVALID_CITY.matcher(name).matches()) return "";
        String country = match.group(3);
        return country == null ? name : name + ", " + country.trim();
    }

    /** Public geographic units are not communes: never geocode them by name. */
    public static String region(String question) {
        if (!isWeather(question)) return null;
        String text = fold(question).replace('-', ' ').replace(',', ' ')
                .replaceAll("\\s+", " ");
        if (text.matches(".*\\b(?:val de marne|departement du val de marne|"
                + "departement 94)\\b.*")) return "Val-de-Marne";
        if (text.matches(".*\\b(?:ile de france|region parisienne)\\b.*"))
            return "Île-de-France";
        return null;
    }

    public static boolean department94(String question) {
        return isWeather(question)
                && fold(question).matches(".*\\b(?:94|94340)\\b.*");
    }

    public static boolean tomorrow(String question) {
        return fold(question).matches(".*\\bdemain\\b.*");
    }
}
