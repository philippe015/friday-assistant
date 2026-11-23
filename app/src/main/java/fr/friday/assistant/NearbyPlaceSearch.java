package fr.friday.assistant;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;

/**
 * Recherche de services OpenStreetMap à la demande uniquement.
 * Jamais appelée sans permission Android, position valide, connexion Web
 * et consentement distinct nearby_online_enabled. Les coordonnées envoyées
 * à Overpass sont arrondies à 3 décimales (~100 m), pas le relevé GPS exact.
 * Aucun historique, prompt ou identifiant utilisateur n'est transmis.
 */
public final class NearbyPlaceSearch {
    private NearbyPlaceSearch() {}
    private static final String ENDPOINT = "https://overpass-api.de/api/interpreter";
    private static final int MAX_BYTES = 180_000;
    private static final int RADIUS_METERS = 900;

    private static final class Place {
        final String name, category;
        final int distance;
        Place(String n, String c, int d) { name=n; category=c; distance=d; }
    }

    private static String category(String value) {
        switch (value) {
            case "pharmacy": return "pharmacie";
            case "hospital": return "hôpital";
            case "clinic": return "clinique";
            case "doctors": return "médecin";
            case "police": return "police";
            case "fire_station": return "pompiers";
            case "post_office": return "bureau de poste";
            case "bank": return "banque";
            case "atm": return "distributeur";
            case "restaurant": return "restaurant";
            case "cafe": return "café";
            case "fuel": return "station-service";
            case "library": return "bibliothèque";
            default: return "";
        }
    }

    private static int meters(double lat1, double lon1, double lat2, double lon2) {
        double a = Math.sin(Math.toRadians(lat2-lat1)/2);
        double b = Math.sin(Math.toRadians(lon2-lon1)/2);
        double h = a*a + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))*b*b;
        return (int)Math.round(12742000d*Math.asin(Math.min(1d, Math.sqrt(h))));
    }

    public static String search(Context ctx, FridayLocation.Fix fix) {
        if (fix == null) return "Position Android indisponible : recherche de proximité impossible.";
        DiagnosticLog.record(ctx, "NEARBY_REMOTE_QUERY");
        // Arrondi spatial et requête bornée pour protéger l'utilisateur et le serveur.
        String lat = String.format(Locale.US, "%.3f", fix.lat);
        String lon = String.format(Locale.US, "%.3f", fix.lon);
        String query = "[out:json][timeout:9];"
                + "nwr(around:" + RADIUS_METERS + "," + lat + "," + lon + ")"
                + "[amenity~\"^(pharmacy|hospital|clinic|doctors|police|fire_station|post_office|bank|atm|restaurant|cafe|fuel|library)$\"];"
                + "out center 100;";
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection)new URL(ENDPOINT).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(12000);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "JARVIS-Android/3.14.18 (github.com/philippe015/friday-assistant)");
            byte[] form = ("data=" + URLEncoder.encode(query, "UTF-8"))
                    .getBytes(StandardCharsets.UTF_8);
            try (OutputStream out = conn.getOutputStream()) { out.write(form); }
            int status = conn.getResponseCode();
            if (status == 429 || status == 502 || status == 503 || status == 504)
                throw new java.io.IOException("service OpenStreetMap momentanément saturé (HTTP " + status + ")");
            if (status != 200) throw new java.io.IOException("HTTP " + status);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (InputStream input = conn.getInputStream()) {
                byte[] buffer = new byte[4096];
                int n;
                while ((n = input.read(buffer)) >= 0) {
                    if (bytes.size() + n > MAX_BYTES)
                        throw new java.io.IOException("réponse de proximité trop volumineuse");
                    bytes.write(buffer, 0, n);
                }
            }
            JSONArray items = new JSONObject(bytes.toString("UTF-8")).optJSONArray("elements");
            ArrayList<Place> places = new ArrayList<>();
            HashSet<String> seen = new HashSet<>();
            if (items != null) for (int i=0; i<items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                if (item == null) continue;
                JSONObject tags = item.optJSONObject("tags");
                if (tags == null) continue;
                String name = tags.optString("name:fr", tags.optString("name", "")).trim();
                String type = category(tags.optString("amenity", ""));
                if (type.isEmpty() || name.length() < 2 || name.length() > 90) continue;
                JSONObject center = item.optJSONObject("center");
                double pLat = center == null ? item.optDouble("lat", Double.NaN)
                        : center.optDouble("lat", Double.NaN);
                double pLon = center == null ? item.optDouble("lon", Double.NaN)
                        : center.optDouble("lon", Double.NaN);
                if (!Double.isFinite(pLat) || !Double.isFinite(pLon)) continue;
                int distance = meters(fix.lat, fix.lon, pLat, pLon);
                if (distance > RADIUS_METERS+150) continue;
                if (seen.add(name.toLowerCase(Locale.ROOT)+":"+type))
                    places.add(new Place(name.replaceAll("[\r\n]", " "), type, distance));
            }
            places.sort(Comparator.comparingInt(p -> p.distance));
            DiagnosticLog.record(ctx, "NEARBY_REMOTE_RESULT");
            if (places.isEmpty())
                return "Aucun service vérifiable dans les données OpenStreetMap disponibles "
                        + "dans un rayon d'environ 900 mètres. Cela ne prouve pas qu'il n'en existe pas.";
            StringBuilder answer = new StringBuilder("Services répertoriés par OpenStreetMap autour de la position Android : ");
            for (int i=0; i<places.size() && i<6; i++) {
                Place p = places.get(i);
                answer.append(i+1).append(". ").append(p.name)
                        .append(" (").append(p.category).append(", environ ")
                        .append(Math.round(p.distance/10f)*10).append(" m). ");
            }
            return answer.append("Données collaboratives potentiellement incomplètes : "
                    + "vérifiez horaires et disponibilité avant déplacement. "
                    + "Source : OpenStreetMap via Overpass.").toString();
        } catch (Exception unavailable) {
            DiagnosticLog.record(ctx, "NEARBY_REMOTE_FAILED");
            // Diagnostic catégoriel uniquement : jamais d'IP, d'URL, de
            // latitude/longitude, ni de message brut d'exception.
            if (unavailable instanceof java.net.SocketTimeoutException) {
                DiagnosticLog.record(ctx, "NEARBY_REMOTE_TIMEOUT");
            } else if (unavailable instanceof java.net.UnknownHostException) {
                DiagnosticLog.record(ctx, "NEARBY_REMOTE_DNS_FAILED");
            } else if (unavailable instanceof javax.net.ssl.SSLException) {
                DiagnosticLog.record(ctx, "NEARBY_REMOTE_TLS_FAILED");
            } else if (unavailable instanceof java.io.IOException) {
                DiagnosticLog.record(ctx, "NEARBY_REMOTE_HTTP_OR_IO");
            } else {
                DiagnosticLog.record(ctx, "NEARBY_REMOTE_UNEXPECTED");
            }
            return "Votre GPS est disponible, mais la recherche OpenStreetMap n'a pas abouti "
                    + "(" + unavailable.getClass().getSimpleName() + "). "
                    + "Réessayez ultérieurement. Aucun établissement n'a été inventé.";
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
