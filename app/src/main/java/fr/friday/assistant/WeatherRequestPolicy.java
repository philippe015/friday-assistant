package fr.friday.assistant;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reconnaissance déterministe de la demande météo, sans modèle ni GPS. */
public final class WeatherRequestPolicy {
    private WeatherRequestPolicy() { }

    private static final Pattern WEATHER = Pattern.compile(
            "\\b(?:meteo|previsions? meteo|quel temps|temps fait|temps fera|"
            + "va t il pleuvoir|va pleuvoir|pleuvra|temperature(?:s)?)\\b");
    private static final Pattern PLACE = Pattern.compile(
            "(?iu)\\b(?:à|a|sur|pour|dans|de)\\s+"
            + "([\\p{L}\\p{M}][\\p{L}\\p{M}'’\\- ,]{1,85})");
    private static final Pattern AFTER_METEO = Pattern.compile(
            "(?iu)\\bm[ée]t[ée]o\\s+([\\p{L}\\p{M}][\\p{L}\\p{M}'’\\- ,]{1,80})");
    private static final Pattern TEMPORAL = Pattern.compile(
            "(?iu)\\b(?:après[ -]?demain|apres[ -]?demain|aujourd['’ ]?hui|"
            + "demain|ce soir|ce matin|cet après-midi|cet apres midi)\\b");

    private static String fold(String value) {
        return VoiceText.normalize(value).replaceAll("[^a-z0-9 ]+", " ")
                .replaceAll("\\s+", " ").trim();
    }

    public static boolean isWeatherRequest(String question) {
        return WEATHER.matcher(fold(VoiceText.withoutWakeWord(question))).find();
    }

    /** 0 = aujourd'hui, 1 = demain, 2 = après-demain. */
    public static int dayOffset(String question) {
        String q = fold(question);
        if (q.matches(".*\\b(?:apres demain|apresdemain)\\b.*")) return 2;
        return q.matches(".*\\bdemain\\b.*") ? 1 : 0;
    }

    private static String validCity(String candidate) {
        if (candidate == null) return "";
        String city = candidate.trim().replaceAll("^[\\s,.:;!?]+|[\\s,.:;!?]+$", "")
                .replaceAll("\\s+", " ").trim();
        String folded = fold(city);
        if (city.length() < 2 || city.length() > 72 || city.split(" ").length > 7)
            return "";
        if (!city.matches("[\\p{L}\\p{M}'’\\- ,]+")) return "";
        if (folded.matches("(?:(?:aujourdhui|aujourd hui|demain|apres demain|"
                + "ce soir|ce matin|maintenant|ici|chez moi|ma ville|"
                + "quelle ville|ou je suis|je sais pas|aucune|non|oui|merci)(?: .*)?)"))
            return "";
        if (folded.matches("^(?:qui|quoi|que|quel|quelle|comment|pourquoi|"
                + "cherche|recherche|donne|explique|dis|salut|bonjour|"
                + "est ce|peux tu|je voudrais)\\b.*")) return "";
        return city;
    }

    public static String cityFromWeatherQuestion(String question) {
        if (!isWeatherRequest(question)) return "";
        String clean = VoiceText.withoutWakeWord(question).trim();
        clean = TEMPORAL.matcher(clean).replaceAll(" ").replaceAll("\\s+", " ");
        String result = "";
        Matcher place = PLACE.matcher(clean);
        while (place.find()) {
            String candidate = validCity(place.group(1));
            if (!candidate.isEmpty()) result = candidate;
        }
        if (!result.isEmpty()) return result;
        Matcher after = AFTER_METEO.matcher(clean);
        if (after.find()) {
            String candidate = after.group(1).trim();
            if (!fold(candidate).matches("^(?:du|de|la|le|des|en|"
                    + "dans|est|fait|pour|sur|a|à)\\b.*"))
                return validCity(candidate);
        }
        return "";
    }

    /** La réponse courte « Paris » doit être permise après « Pour quelle ville ? ». */
    public static String cityReply(String input) {
        if (input == null || isWeatherRequest(input)) return "";
        String spoken = VoiceText.withoutWakeWord(input).trim();
        spoken = spoken.replaceFirst("(?iu)^(?:(?:c['’]est|la ville de|"
                + "pour|à|a|dans|sur)\\s+)", "").trim();
        return validCity(spoken);
    }

    public static String dayLabel(int offset) {
        return offset == 2 ? "après-demain" : offset == 1 ? "demain" : "aujourd'hui";
    }
}
