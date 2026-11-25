package fr.friday.assistant;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Intelligence+ : outil météo déterministe avec Open-Meteo.
 * On ne transmet que le NOM DE VILLE prononcé par l'utilisateur, jamais son GPS,
 * ses notes, ses questions précédentes ou son historique chiffré.
 * Pas d'agent arbitraire, de clé API ni de modèle externe.
 */
public final class JarvisWeather {
    private JarvisWeather() {}

    /** Extrait seulement la ville d'une précédente RÉPONSE MÉTÉO publique.
     * L'historique chiffré reste sur Android, jamais transmis au fournisseur. */
    public static String followUp(Context context, SecurePrefs prefs, String question) {
        if (question == null || question.trim().length() > 90) return null;
        try {
            JSONArray history = new JSONArray(PrivateConversationHistory.read(context, prefs));
            int end = history.length();
            if (end == 0) return null;
            JSONObject last = history.optJSONObject(end - 1);
            if (last != null && "user".equals(last.optString("kind"))
                    && VoiceText.normalize(question).equals(
                            VoiceText.normalize(last.optString("text"))))
                end--;
            if (end < 1) return null;
            JSONObject prior = history.optJSONObject(end - 1);
            if (prior == null || !"assistant".equals(prior.optString("kind"))) return null;
            String lastAssistant = prior.optString("text", "");
            boolean awaitingCity = WeatherCitySpeechPolicy.awaitingWeatherCity(lastAssistant);
            // A previous weather request can specify "demain", while the
            // user's subsequent reply contains only the city. This local scan
            // never exports the encrypted history to Open-Meteo.
            String previousWeatherRequest = "";
            if (awaitingCity) {
                for (int i = end - 2; i >= Math.max(0, end - 8); i--) {
                    JSONObject item = history.optJSONObject(i);
                    if (item != null && "user".equals(item.optString("kind"))
                            && WeatherQueryPolicy.isWeather(item.optString("text", ""))) {
                        previousWeatherRequest = item.optString("text", "");
                        break;
                    }
                }
                // A Vosk fragment like "scène France", "ii sur scène" or
                // "sur scène" is NOT enough to choose Vitry-sur-Seine.
                if (WeatherCitySpeechPolicy.missingCityInAudio(question)) {
                    DiagnosticLog.record(context, "WEATHER_CITY_ASR_CLARIFY");
                    return WeatherCitySpeechPolicy.askCityAgain();
                }
                String resolved = WeatherCitySpeechPolicy.explicitWeatherQuestion(
                        question, previousWeatherRequest);
                if (resolved != null) {
                    DiagnosticLog.record(context, "WEATHER_CITY_PHONETIC_ACCEPTED");
                    return answer(resolved, enabledWeb(prefs));
                }
            }
            String query = WeatherFollowUpPolicy.enrich(question, lastAssistant);
            if (query != null) {
                // Keep the previous explicit date when the response is a city
                // alone. Explicit "ce soir/demain" in the reply wins.
                if (awaitingCity && !WeatherQueryPolicy.tomorrow(query)
                        && !WeatherEveningPolicy.requested(query)
                        && WeatherQueryPolicy.tomorrow(previousWeatherRequest))
                    query = query.replaceFirst("^météo\\s+", "météo demain ");
                return answer(query, enabledWeb(prefs));
            }
            if (awaitingCity && WeatherFollowUpPolicy.clearSpokenCityReply(question)) {
                DiagnosticLog.record(context, "WEATHER_CITY_ASR_CLARIFY");
                return WeatherCitySpeechPolicy.askCityAgain();
            }
            if (WeatherFollowUpPolicy.uncertainCityAfterWeatherPrompt(
                    question, lastAssistant)) {
                return "Je n'ai pas compris le nom de la commune. "
                        + "Précisez la ville et le pays, par exemple "
                        + "« météo ce soir pour Vitry-sur-Seine, France ». "
                        + "Je ne lance pas de recherche Internet sur une ville incertaine.";
            }
            return null;
        } catch (Exception localHistoryUnavailable) {
            // Aucun repli vers la recherche en ligne de l'historique personnel.
            return null;
        }
    }

    private static boolean enabledWeb(SecurePrefs prefs) {
        return JarvisInternet.enabled(prefs);
    }

    /** Retourne null si la question ne porte pas sur la météo. */
    public static String answer(String question, boolean webEnabled) {
        // A department or a region must NOT be sent to Open-Meteo's
        // commune geocoder as if it were a city.
        String region = WeatherQueryPolicy.region(question);
        if (region != null) {
            if (!webEnabled) return "La météo de " + region
                    + " nécessite Internet : activez Internet Jarvis.";
            String representative = "Val-de-Marne".equals(region)
                    ? "Créteil" : "Paris";
            String forecast = answer("météo "
                    + (WeatherQueryPolicy.tomorrow(question) ? "demain " : "")
                    + "pour " + representative + ", France", true);
            if (forecast == null) return "Impossible d'obtenir les données météo "
                    + "pour " + region + " à cet instant.";
            return "Pour " + region + ", je consulte " + representative
                    + " comme point de référence uniquement : les conditions "
                    + "peuvent varier ailleurs dans le département ou la région. "
                    + forecast + " Pour une prévision précise, indiquez une commune.";
        }
        String city = WeatherQueryPolicy.city(question);
        if (city == null) return null;
        if (city.isEmpty())
            return "Pour quelle ville souhaitez-vous la météo ? "
                    + "Dites par exemple « météo pour Paris, France ».";
        if (!webEnabled)
            return "La recherche météo nécessite Internet. "
                    + "Activez « Autoriser Jarvis à rechercher sur Internet » dans les réglages.";
        try {
            boolean department94 = WeatherQueryPolicy.department94(question);
            String lookup = city.split(",")[0].trim();
            String countryHint = city.contains(",")
                    ? normalizedCity(city.substring(city.indexOf(',') + 1)) : "";
            String countryCode = department94 || "france".equals(countryHint) ? "FR"
                    : "belgique".equals(countryHint) ? "BE"
                    : "suisse".equals(countryHint) ? "CH"
                    : "canada".equals(countryHint) ? "CA" : "";
            // Open-Meteo attend le nom de la commune dans « name » ;
            // un pays explicitement indiqué devient un filtre countryCode.
            JSONObject places = readJson("https://geocoding-api.open-meteo.com/v1/search"
                    + "?name=" + URLEncoder.encode(lookup, "UTF-8")
                    + (countryCode.isEmpty() ? "" : "&countryCode=" + countryCode)
                    + "&count=10&language=fr&format=json");
            JSONArray results = places.optJSONArray("results");
            if (results == null || results.length() == 0)
                return "Aucune commune correspondant à « " + city
                        + " » n'a été trouvée par le géocodeur Open-Meteo. "
                        + "Vérifiez l'orthographe ou précisez son pays.";
            String expected = normalizedCity(lookup);
            JSONObject place = null;
            // On refuse de remplacer Joinville-le-Pont par « Joinville »
            // simplement parce que le géocodeur classe ce dernier en tête.
            for (int i = 0; i < results.length(); i++) {
                JSONObject candidate = results.optJSONObject(i);
                if (candidate == null || !normalizedCity(candidate.optString("name", ""))
                        .equals(expected)) continue;
                if (!countryCode.isEmpty()
                        && !countryCode.equalsIgnoreCase(candidate.optString("country_code", "")))
                    continue;
                if (department94 && !normalizedCity(candidate.optString("admin2", ""))
                        .equals("valdemarne")) continue;
                place = candidate;
                break;
            }
            if (place == null) return department94
                    ? "La commune « " + city + " » n'a pas pu être vérifiée "
                      + "dans le Val-de-Marne (94). Je ne vais pas choisir une autre ville."
                    : "Les résultats Open-Meteo ne correspondent pas exactement à « "
                      + city + " ». Précisez la commune ou le pays.";
            String name = place.optString("name", city);
            String country = place.optString("country", "pays non précisé");
            // Hors précision départementale, demander le pays pour les
            // homonymes de pays différents au lieu d'inventer un choix.
            if (countryCode.isEmpty() && !city.contains(",") && !department94) {
                for (int i = 0; i < results.length(); i++) {
                    JSONObject alternative = results.optJSONObject(i);
                    if (alternative != null
                            && normalizedCity(name).equals(
                                normalizedCity(alternative.optString("name", "")))
                            && !country.equalsIgnoreCase(alternative.optString("country"))) {
                        return "Plusieurs villes portent ce nom. Précisez le pays, par exemple "
                                + "« météo pour " + name + ", " + country + " ».";
                    }
                }
            }
            double lat = place.optDouble("latitude", Double.NaN);
            double lon = place.optDouble("longitude", Double.NaN);
            if (!Double.isFinite(lat) || !Double.isFinite(lon))
                throw new IOException("coordonnées météorologiques indisponibles");
            JSONObject forecast = readJson("https://api.open-meteo.com/v1/forecast"
                    + "?latitude=" + String.format(Locale.ROOT, "%.5f", lat)
                    + "&longitude=" + String.format(Locale.ROOT, "%.5f", lon)
                    + "&current=temperature_2m,weather_code,wind_speed_10m"
                    + "&hourly=temperature_2m,precipitation_probability,weather_code"
                    + "&daily=temperature_2m_max,temperature_2m_min,"
                    + "precipitation_probability_max,weather_code"
                    + "&forecast_days=2&timezone=auto");
            JSONObject daily = forecast.optJSONObject("daily");
            if (daily == null) throw new IOException("prévisions journalières absentes");
            boolean tomorrow = WeatherQueryPolicy.tomorrow(question);
            int day = tomorrow ? 1 : 0;
            JSONArray days = daily.optJSONArray("time");
            if (days == null || days.length() <= day)
                throw new IOException("date de prévision indisponible");
            // « ce soir » / « demain soir » must use hourly data in the
            // destination's local timezone, never today's daily min/max.
            if (WeatherEveningPolicy.requested(question))
                return eveningAnswer(name, country, forecast, days.optString(day),
                        tomorrow);
            JSONArray high = daily.optJSONArray("temperature_2m_max");
            JSONArray low = daily.optJSONArray("temperature_2m_min");
            JSONArray rain = daily.optJSONArray("precipitation_probability_max");
            JSONArray conditions = daily.optJSONArray("weather_code");
            if (high == null || low == null || conditions == null
                    || high.isNull(day) || low.isNull(day) || conditions.isNull(day))
                throw new IOException("données météo incomplètes");
            StringBuilder out = new StringBuilder("Prévisions météo pour ")
                    .append(name).append(", ").append(country)
                    .append(", ").append(tomorrow ? "demain" : "aujourd'hui")
                    .append(" (").append(days.optString(day)).append(") : ")
                    .append(description(conditions.optInt(day, -1))).append(". ")
                    .append("Minimum ").append(Math.round(low.optDouble(day)))
                    .append(" degrés, maximum ").append(Math.round(high.optDouble(day)))
                    .append(" degrés. ");
            if (rain != null && !rain.isNull(day))
                out.append("Probabilité maximale de précipitations : ")
                    .append(Math.round(rain.optDouble(day))).append(" %. ");
            JSONObject current = forecast.optJSONObject("current");
            if (!tomorrow && current != null
                    && !current.isNull("temperature_2m"))
                out.append("Température actuelle estimée : ")
                    .append(Math.round(current.optDouble("temperature_2m")))
                    .append(" degrés. ");
            out.append("Source : Open-Meteo (prévision météorologique, "
                    + "non une mesure directe sur votre téléphone).");
            return out.toString();
        } catch (Exception unavailable) {
            return "La prévision météorologique pour « " + city
                    + " » n'a pas pu être vérifiée actuellement. "
                    + "Le service Open-Meteo ou la connexion HTTPS est indisponible. "
                    + "Je ne vais pas inventer de température.";
        }
    }

    /** Sources horaires Open-Meteo, sélectionnées dans le fuseau du lieu nommé. */
    private static String eveningAnswer(String city, String country,
                                        JSONObject forecast, String date,
                                        boolean tomorrow) throws Exception {
        String timezone = forecast.optString("timezone", "");
        java.time.ZoneId zone;
        try {
            zone = java.time.ZoneId.of(timezone);
        } catch (java.time.DateTimeException invalidZone) {
            throw new IOException("fuseau de la commune non vérifiable", invalidZone);
        }
        JSONObject hourly = forecast.optJSONObject("hourly");
        JSONArray times = hourly == null ? null : hourly.optJSONArray("time");
        JSONArray temps = hourly == null ? null : hourly.optJSONArray("temperature_2m");
        JSONArray rains = hourly == null ? null : hourly.optJSONArray("precipitation_probability");
        JSONArray codes = hourly == null ? null : hourly.optJSONArray("weather_code");
        if (times == null || temps == null || codes == null)
            throw new IOException("météo horaire indisponible");
        String[] stamps = new String[times.length()];
        for (int i = 0; i < stamps.length; i++) stamps[i] = times.optString(i, "");
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now(zone);
        int earliest = 18;
        // For this evening, do not present yesterday's or already-passed hours.
        if (!tomorrow && now.toLocalDate().toString().equals(date))
            earliest = now.getHour() + (now.getMinute() > 0 ? 1 : 0);
        int[] indexes = WeatherEveningPolicy.indices(stamps, date, earliest);
        if (indexes.length == 0) {
            return "La soirée du " + date + " est passée, ou le service météo "
                    + "ne fournit plus de prévisions horaires futures pour "
                    + city + ", " + country + ". Précisez une autre date.";
        }
        double minimum = Double.POSITIVE_INFINITY;
        double maximum = Double.NEGATIVE_INFINITY;
        int maxPrecip = -1;
        int representative = -1;
        for (int i : indexes) {
            if (i >= temps.length() || i >= codes.length()
                    || temps.isNull(i) || codes.isNull(i))
                throw new IOException("prévision horaire incomplète");
            double temperature = temps.optDouble(i, Double.NaN);
            if (!Double.isFinite(temperature)) throw new IOException("température non vérifiable");
            minimum = Math.min(minimum, temperature);
            maximum = Math.max(maximum, temperature);
            if (representative < 0) representative = codes.optInt(i, -1);
            if (rains != null && i < rains.length() && !rains.isNull(i)) {
                double precipitation = rains.optDouble(i, Double.NaN);
                if (Double.isFinite(precipitation) && precipitation >= 0
                        && precipitation <= 100)
                    maxPrecip = Math.max(maxPrecip, (int) Math.round(precipitation));
            }
        }
        String first = stamps[indexes[0]];
        String last = stamps[indexes[indexes.length - 1]];
        StringBuilder out = new StringBuilder("Prévisions horaires pour ")
                .append(city).append(", ").append(country).append(", ")
                .append(tomorrow ? "demain soir" : "ce soir")
                .append(" (").append(date).append(", ")
                .append(first.substring(11, 13)).append(" h à ")
                .append(last.substring(11, 13)).append(" h, heure locale) : ")
                .append(description(representative)).append(". ")
                .append("Températures prévues entre ")
                .append(Math.round(minimum)).append(" et ")
                .append(Math.round(maximum)).append(" degrés. ");
        if (maxPrecip >= 0)
            out.append("Probabilité maximale de précipitations : ")
               .append(maxPrecip).append(" %. ");
        return out.append("Source : Open-Meteo, prévisions horaires ; "
                + "la météo peut évoluer.").toString();
    }

    /** Correspondance stricte des noms sans espaces, tirets ou accents. */
    private static String normalizedCity(String input) {
        return WeatherQueryPolicy.fold(input).replaceAll("[^a-z0-9]", "");
    }

    private static String description(int code) {
        if (code == 0) return "ciel dégagé";
        if (code <= 3 && code > 0) return "ciel partiellement nuageux";
        if (code == 45 || code == 48) return "brouillard";
        if (code >= 51 && code <= 57) return "bruine";
        if (code >= 61 && code <= 67) return "pluie";
        if (code >= 71 && code <= 77) return "neige";
        if (code >= 80 && code <= 82) return "averses";
        if (code >= 85 && code <= 86) return "averses de neige";
        if (code >= 95) return "orages";
        return "conditions variables";
    }

    /** Requêtes HTTPS vers les deux serveurs fixés dans le code, sans redirection. */
    private static JSONObject readJson(String endpoint) throws Exception {
        if (!endpoint.startsWith("https://api.open-meteo.com/v1/forecast?")
                && !endpoint.startsWith("https://geocoding-api.open-meteo.com/v1/search?"))
            throw new IOException("Domaine HTTPS non autorisé");
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setConnectTimeout(6000);
        connection.setReadTimeout(6000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("Accept", "application/json");
        try {
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
                throw new IOException("service météo HTTP " + connection.getResponseCode());
            try (InputStream stream = connection.getInputStream();
                 ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int n;
                while ((n = stream.read(buffer)) >= 0) {
                    if (bytes.size() + n > 65536)
                        throw new IOException("réponse météo trop volumineuse");
                    bytes.write(buffer, 0, n);
                }
                JSONObject value = new JSONObject(
                        new String(bytes.toByteArray(), StandardCharsets.UTF_8));
                if (value.optBoolean("error"))
                    throw new IOException("erreur météorologique");
                return value;
            }
        } finally {
            connection.disconnect();
        }
    }
}
