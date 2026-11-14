package fr.friday.assistant;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Identifie à la demande une commune ou rue via le géocodeur du téléphone.
 *  Aucun Google Maps/Places SDK, aucune clé API externe dans l'application.
 *  ATTENTION : le fournisseur du géocodeur Android peut utiliser son propre réseau.
 *  Appel limité à 5 secondes, jamais sur le fil principal. Ne pas stocker l'adresse.
 */
public final class AndroidPlaceResolver {
    private AndroidPlaceResolver() {}
    public static String resolve(Context context, FridayLocation.Fix fix) {
        if (fix == null) return "";
        if (!Geocoder.isPresent()) return "";
        try {
            Geocoder geocoder = new Geocoder(context, Locale.FRENCH);
            List<Address> list;
            if (Build.VERSION.SDK_INT >= 33) {
                CountDownLatch signal = new CountDownLatch(1);
                AtomicReference<List<Address>> result = new AtomicReference<>();
                geocoder.getFromLocation(fix.lat, fix.lon, 1, new Geocoder.GeocodeListener() {
                    @Override public void onGeocode(List<Address> addresses) {
                        result.set(addresses); signal.countDown();
                    }
                    @Override public void onError(String message) { signal.countDown(); }
                });
                if (!signal.await(5, TimeUnit.SECONDS)) return "";
                list = result.get();
            } else {
                // Android 12 ou antérieur : l'appel est exécuté par le thread secondaire du service.
                list = geocoder.getFromLocation(fix.lat, fix.lon, 1);
            }
            if (list == null || list.isEmpty()) return "";
            Address a = list.get(0);
            String city = first(a.getLocality(), a.getSubAdminArea(), a.getAdminArea());
            String region = clean(a.getAdminArea());
            String country = clean(a.getCountryName());
            String street = fix.accuracy <= 35 && !fix.approximate ? clean(a.getThoroughfare()) : "";
            String number = fix.accuracy <= 12 && !fix.approximate ? clean(a.getSubThoroughfare()) : "";
            StringBuilder b = new StringBuilder();
            if (!street.isEmpty()) { b.append(street); if (!number.isEmpty()) b.append(" ").append(number); }
            if (!city.isEmpty() && !equalsIgnoreCase(city, street)) append(b, city);
            if (b.length() == 0 && !region.isEmpty()) b.append(region);
            if (!country.isEmpty() && !equalsIgnoreCase(country, city)) append(b, country);
            return b.toString();
        } catch (IOException | InterruptedException | IllegalArgumentException | SecurityException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return "";
        }
    }
    private static String clean(String s) { return s == null ? "" : s.trim().replaceAll("[\\r\\n]", " "); }
    private static String first(String... values) { for (String v : values) if (!clean(v).isEmpty()) return clean(v); return ""; }
    private static void append(StringBuilder b, String v) { if (b.length() > 0) b.append(", "); b.append(v); }
    private static boolean equalsIgnoreCase(String x, String y) { return x.equalsIgnoreCase(y); }
    public static String userText(FridayLocation.Fix fix, String place) {
        if (fix == null) return "Position indisponible.";
        String text = place == null || place.trim().isEmpty() ?
                "Je dispose de vos coordonnées GPS, mais aucun nom de lieu fiable n'a été retrouvé." :
                "D'après la localisation Android, vous vous trouvez probablement à " + place + ". Cette adresse est une estimation.";
        return text + " " + fix.forUser();
    }
}
