package fr.friday.assistant;

import java.util.regex.Pattern;

/**
 * On-device speech-repair policy for a short, explicitly requested weather
 * city. Never infers a city from an orphaned phonetic fragment ("scène").
 * No GPS, network access, secret data, or automatically learned dictionary.
 */
public final class WeatherCitySpeechPolicy {
    private WeatherCitySpeechPolicy() {}

    private static String fold(String question) {
        return WeatherQueryPolicy.fold(question).replace('-', ' ')
                .replaceAll("\\s+", " ").trim();
    }

    // Only explicit "Vitry + sur/du/dur + Seine/scène" identifies this city.
    // "sur scène" or "scène France" must NOT be silently rewritten.
    private static final Pattern VITRY_SEINE = Pattern.compile(
            "\\b(?:vitry|vitri)\\s+(?:sur|dur|du)\\s+"
          + "(?:seine|scene|senne|sene)\\b");

    public static String canonicalCity(String question) {
        if (question == null || question.length() > 145) return null;
        String q = fold(question);
        return VITRY_SEINE.matcher(q).find() ? "vitry-sur-seine, france" : null;
    }

    /**
     * Vosk may remove "Vitry" completely; at that point it is unsafe to
     * assume the intended town, even if the expected town was previously
     * named in a *different* chat.
     */
    public static boolean missingCityInAudio(String question) {
        if (question == null || question.length() > 85) return false;
        String q = fold(question)
                .replaceFirst("^(?:jarvis|jervis|friday|service)\\s+", "");
        if (canonicalCity(q) != null) return false;
        return q.matches("^(?:(?:ii|i|c est|ce|c|la|le|pour|a|sur|en|"
                + "oui|non|je dis)\\s+){0,3}(?:sur\\s+)?"
                + "(?:scene|seine|senne|sene)(?:\\s+france)?$")
                || q.matches("^(?:sur|a|pour|ii sur)\\s+"
                + "(?:scene|seine|senne|sene)(?:\\s+france)?$");
    }

    /** Check only assistant messages that we own, never arbitrary Wikipedia. */
    public static boolean awaitingWeatherCity(String lastAssistant) {
        if (lastAssistant == null || lastAssistant.length() > 550) return false;
        return lastAssistant.startsWith("Pour quelle ville souhaitez-vous la météo")
            || lastAssistant.startsWith("Aucune commune correspondant à")
            || lastAssistant.startsWith("Les résultats Open-Meteo ne correspondent")
            || lastAssistant.startsWith("La commune «")
            || lastAssistant.startsWith("Je n'ai pas compris le nom de la commune")
            || (lastAssistant.startsWith("Vous indiquez «")
                && lastAssistant.contains("souhaitez la météo de cette commune"));
    }

    public static String askCityAgain() {
        return "Je n'ai pas compris le nom complet de la commune. "
            + "Veuillez répéter lentement le nom et le pays, "
            + "ou écrire la ville dans le dialogue. "
            + "Je ne vais ni deviner la commune ni lancer de recherche générale.";
    }

    /** Keep yesterday/tomorrow qualifier only within a current weather flow. */
    public static String weatherPeriod(String cityReply, String previousWeatherRequest) {
        String reply = fold(cityReply);
        if (reply.matches(".*\\bdemain soir\\b.*")) return "demain soir ";
        if (reply.matches(".*\\bce soir\\b.*")) return "ce soir ";
        if (reply.matches(".*\\bdemain\\b.*")) return "demain ";
        if (reply.matches(".*\\baujourd hui\\b.*")) return "aujourd'hui ";
        if (WeatherQueryPolicy.tomorrow(previousWeatherRequest)) return "demain ";
        return "";
    }

    public static String explicitWeatherQuestion(String reply, String previousWeatherRequest) {
        String city = canonicalCity(reply);
        if (city == null) return null;
        return "météo " + weatherPeriod(reply, previousWeatherRequest)
                + "pour " + city;
    }
}
