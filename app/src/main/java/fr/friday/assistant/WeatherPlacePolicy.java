package fr.friday.assistant;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Do not infer a country from a first geocoding result.
 * No GPS or user locale is used as a silent location guess.
 */
public final class WeatherPlacePolicy {
    private WeatherPlacePolicy() {}

    public static String fold(String value) {
        String text = Normalizer.normalize(value == null ? "" : value,
                Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return text.toLowerCase(Locale.ROOT).replace('’', '\'')
                .replaceAll("[^a-z0-9 ]+", " ").replaceAll("\\s+", " ").trim();
    }

    // La dictée Android supprime souvent les virgules : « Paris France » doit
    // être reconnu comme ville + pays, jamais comme ville « Paris France ».
    private static final java.util.regex.Pattern COUNTRY_SUFFIX =
            java.util.regex.Pattern.compile(
                "(?iu)^(.*?)\\s+(?:(?:en|au|aux)\\s+)?"
                + "(france|fr|états[ -]unis|etats[ -]unis|usa|"
                + "canada|belgique|suisse|allemagne|espagne|italie|portugal|"
                + "royaume[ -]uni|angleterre|maroc|algérie|algerie|tunisie|"
                + "japon|chine|brésil|bresil|australie|mexique|inde|"
                + "turquie|grèce|grece|norvège|norvege|suède|suede|"
                + "pays[ -]bas|autriche|pologne|sénégal|senegal|"
                + "côte[ -]d['’]ivoire|cote[ -]d['’]ivoire)$");

    public static String city(String userPlace) {
        String text = userPlace == null ? "" : userPlace.trim();
        int comma = text.lastIndexOf(',');
        if (comma >= 0 && comma < text.length() - 2)
            return text.substring(0, comma).trim();
        java.util.regex.Matcher match = COUNTRY_SUFFIX.matcher(text);
        if (match.matches()) return match.group(1).trim();
        return text;
    }

    public static String country(String userPlace) {
        String text = userPlace == null ? "" : userPlace.trim();
        int comma = text.lastIndexOf(',');
        if (comma >= 0 && comma < text.length() - 2)
            return text.substring(comma + 1).trim();
        java.util.regex.Matcher match = COUNTRY_SUFFIX.matcher(text);
        if (match.matches()) return match.group(2).trim();
        return "";
    }

    private static String code(String raw) {
        String name = fold(raw);
        switch (name) {
            case "france": case "fr": return "fr";
            case "usa": case "us": case "etats unis": case "etats unis d amerique":
            case "united states": case "united states of america":
            case "amerique": return "us";
            case "canada": case "ca": return "ca";
            case "belgique": case "belgium": case "be": return "be";
            case "suisse": case "switzerland": case "ch": return "ch";
            case "royaume uni": case "angleterre": case "uk": case "gb":
            case "united kingdom": return "gb";
            case "allemagne": case "germany": case "de": return "de";
            case "espagne": case "spain": case "es": return "es";
            case "italie": case "italy": case "it": return "it";
            case "portugal": case "pt": return "pt";
            default: return name;
        }
    }

    public static boolean sameCountry(String userCountry, String apiCountry,
                                      String apiCountryCode) {
        String request = code(userCountry);
        if (request.isEmpty()) return false;
        return request.equals(code(apiCountry))
                || request.equals(code(apiCountryCode));
    }

    /** Reject fuzzy geocoding matches for a different place name. */
    public static boolean sameCity(String asked, String found) {
        String a = fold(asked), b = fold(found);
        return !a.isEmpty() && a.equals(b);
    }
}
