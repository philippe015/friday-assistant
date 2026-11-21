package fr.friday.assistant;

import android.os.SystemClock;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Prévisions météorologiques depuis Open-Meteo : géocodage et données de modèles.
 * Le nom de la ville demandé par l'utilisateur seul est envoyé à Open-Meteo.
 * Aucune coordonnée GPS, conversation ou mémoire privée n'est transmise.
 */
public final class WeatherForecast {
    private WeatherForecast() { }
    public static final String SOURCE = "https://open-meteo.com/en/docs";

    public static final class Result {
        public final String text;
        public final String speech;
        Result(String text, String speech) {
            this.text = text; this.speech = speech;
        }
    }

    /** Le fournisseur a retrouvé plusieurs pays : demander confirmation. */
    public static final class NeedCountryException extends IOException {
        public final String city;
        public NeedCountryException(String city, String countries) {
            super("Pour « " + city + " », je dois vérifier le pays. "
                    + "Pays trouvés : " + countries + ". Quel pays souhaitez-vous ?");
            this.city = city;
        }
    }

    private static JSONObject fetch(String url, NativeAI.CancelToken cancellation)
            throws Exception {
        if (cancellation != null) cancellation.check();
        URI uri = URI.create(url);
        if (!"https".equalsIgnoreCase(uri.getScheme()) ||
                !("api.open-meteo.com".equalsIgnoreCase(uri.getHost())
                    || "geocoding-api.open-meteo.com".equalsIgnoreCase(uri.getHost())))
            throw new IOException("Source météo non autorisée.");
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(5_000);
        connection.setReadTimeout(7_000);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent",
                "JARVIS-Android/3.13.18 (+https://github.com/philippe015/friday-assistant)");
        try {
            int status = connection.getResponseCode();
            if (status != 200) throw new IOException("Service météo HTTP " + status);
            String type = connection.getContentType();
            if (type != null && !type.toLowerCase(Locale.ROOT).contains("json"))
                throw new IOException("Réponse météo non JSON.");
            if (connection.getContentLength() > 120_000)
                throw new IOException("Réponse météo trop volumineuse.");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (InputStream in = connection.getInputStream()) {
                byte[] bytes = new byte[4096];
                int n;
                while ((n = in.read(bytes)) != -1) {
                    if (out.size() + n > 120_000)
                        throw new IOException("Réponse météo trop longue.");
                    out.write(bytes, 0, n);
                }
            }
            if (cancellation != null) cancellation.check();
            return new JSONObject(new String(out.toByteArray(), StandardCharsets.UTF_8));
        } finally {
            connection.disconnect();
        }
    }

    private static String conditions(int code) {
        switch (code) {
            case 0: return "ciel dégagé";
            case 1: return "globalement ensoleillé";
            case 2: return "partiellement nuageux";
            case 3: return "couvert";
            case 45: case 48: return "brouillard";
            case 51: case 53: case 55: return "bruine";
            case 56: case 57: return "bruine verglaçante";
            case 61: case 63: case 65: return "pluie";
            case 66: case 67: return "pluie verglaçante";
            case 71: case 73: case 75: case 77: return "neige";
            case 80: case 81: case 82: return "averses";
            case 85: case 86: return "averses de neige";
            case 95: case 96: case 99: return "orages";
            default: return "conditions variables";
        }
    }

    private static int rounded(double value) {
        return (int) Math.round(value);
    }

    public static Result lookup(String city, int dayOffset,
                                NativeAI.CancelToken cancellation) throws Exception {
        if (city == null || city.trim().length() < 2 || dayOffset < 0 || dayOffset > 2)
            throw new IOException("Ville ou période météo invalide.");
        String requestedCity = WeatherPlacePolicy.city(city);
        String requestedCountry = WeatherPlacePolicy.country(city);
        // Le moteur ASR peut supprimer la virgule de « Paris France ».
        // La recherche géographique ne porte que sur la ville ; le pays
        // déclaré est validé séparément sur la réponse Open-Meteo.
        String q = URLEncoder.encode(requestedCity.trim(), "UTF-8");
        JSONObject geocode = fetch(
                "https://geocoding-api.open-meteo.com/v1/search?name=" + q
                + "&count=40&language=fr&format=json", cancellation);
        JSONArray places = geocode.optJSONArray("results");
        if (places == null || places.length() == 0)
            throw new IOException("Ville introuvable. Précisez la ville et son pays.");

        // Le premier résultat Open-Meteo peut être une ville d'un AUTRE pays.
        // Ne sélectionner que le nom de ville exact, puis vérifier le pays.
        // Si plusieurs pays existent et qu'aucun n'a été demandé, ne choisir
        // ni le premier ni le pays présumé de l'utilisateur : lui demander.
        JSONObject place = null;
        java.util.LinkedHashSet<String> countries = new java.util.LinkedHashSet<>();
        for (int i = 0; i < places.length(); i++) {
            JSONObject candidate = places.optJSONObject(i);
            if (candidate == null || !WeatherPlacePolicy.sameCity(
                    requestedCity, candidate.optString("name", ""))) continue;
            String country = candidate.optString("country", "");
            if (!country.isEmpty()) countries.add(country);
            if (!requestedCountry.isEmpty()
                    && WeatherPlacePolicy.sameCountry(
                            requestedCountry, country,
                            candidate.optString("country_code", ""))) {
                place = candidate;
                break;
            }
            if (requestedCountry.isEmpty() && place == null)
                place = candidate;
        }
        // Ne présumer ni la France ni le pays du premier résultat :
        // sans pays explicite, demander confirmation avant tout bulletin.
        if (requestedCountry.isEmpty()) {
            StringBuilder options = new StringBuilder();
            int shown = 0;
            for (String country : countries) {
                if (shown++ >= 4) break;
                if (options.length() > 0) options.append(", ");
                options.append(country);
            }
            throw new NeedCountryException(requestedCity, options.toString());
        }
        if (place == null)
            throw new IOException("Aucune ville exacte « " + requestedCity
                    + " » trouvée dans le pays « " + requestedCountry
                    + " ». Vérifiez la ville et le pays.");
        double lat = place.getDouble("latitude");
        double lon = place.getDouble("longitude");
        if (!Double.isFinite(lat) || !Double.isFinite(lon)
                || Math.abs(lat) > 90 || Math.abs(lon) > 180)
            throw new IOException("Coordonnées renvoyées par le service invalides.");
        String display = place.getString("name");
        String country = place.optString("country", "");
        String administrative = place.optString("admin1", "");
        String location = display
                + (administrative.isEmpty() || administrative.equalsIgnoreCase(display)
                    ? "" : ", " + administrative)
                + (country.isEmpty() ? "" : ", " + country);
        String forecastUrl = "https://api.open-meteo.com/v1/forecast?latitude="
                + lat + "&longitude=" + lon
                + "&current=temperature_2m,weather_code,wind_speed_10m,apparent_temperature"
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min,"
                + "precipitation_probability_max&forecast_days=3&timezone=auto";
        JSONObject forecast = fetch(forecastUrl, cancellation);
        JSONObject daily = forecast.getJSONObject("daily");
        JSONArray codes = daily.getJSONArray("weather_code");
        JSONArray highs = daily.getJSONArray("temperature_2m_max");
        JSONArray lows = daily.getJSONArray("temperature_2m_min");
        JSONArray rain = daily.optJSONArray("precipitation_probability_max");
        JSONArray dates = daily.getJSONArray("time");
        if (codes.length() <= dayOffset || highs.length() <= dayOffset
                || lows.length() <= dayOffset || dates.length() <= dayOffset)
            throw new IOException("Prévision indisponible pour la journée demandée.");
        String date = dates.getString(dayOffset);
        StringBuilder spoken = new StringBuilder();
        spoken.append("Selon Open-Meteo, pour ").append(location).append(", ")
                .append(WeatherRequestPolicy.dayLabel(dayOffset))
                .append(" (").append(date).append("), ")
                .append(conditions(codes.getInt(dayOffset))).append(". ");
        if (dayOffset == 0 && forecast.has("current")) {
            JSONObject current = forecast.getJSONObject("current");
            if (current.has("temperature_2m") && !current.isNull("temperature_2m"))
                spoken.append("Température estimée actuellement : ")
                        .append(rounded(current.getDouble("temperature_2m")))
                        .append(" degrés. ");
            if (current.has("wind_speed_10m") && !current.isNull("wind_speed_10m"))
                spoken.append("Vent d'environ ")
                        .append(rounded(current.getDouble("wind_speed_10m")))
                        .append(" kilomètres par heure. ");
        }
        spoken.append("Températures prévues entre ")
                .append(rounded(lows.getDouble(dayOffset))).append(" et ")
                .append(rounded(highs.getDouble(dayOffset))).append(" degrés. ");
        if (rain != null && rain.length() > dayOffset && !rain.isNull(dayOffset)) {
            spoken.append("Probabilité maximale de précipitations : ")
                    .append(rounded(rain.getDouble(dayOffset))).append(" pour cent. ");
        }
        spoken.append("Il s'agit de prévisions de modèles météorologiques, "
                + "pas d'un relevé garanti.");
        String result = spoken.toString();
        return new Result(result + "\n\nSource météorologique : Open-Meteo — "
                + SOURCE + "\nLieu géocodé : " + location
                + "\nDate locale de prévision : " + date, result);
    }
}
