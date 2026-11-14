package fr.friday.assistant;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Looper;
import android.os.SystemClock;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Position ponctuelle GPS Android, sans Google Places, Geocoder ni API cartographique.
 *  Cible 3 m : objectif de qualité, jamais une précision garantie.
 *  Cette méthode est bloquante et doit être appelée depuis un thread secondaire.
 */
public final class FridayLocation {
    private FridayLocation() {}
    public static final float TARGET_METERS = 3f;
    private static final long MAX_WAIT_MS = 16_000L;
    private static final long MAX_AGE_SEC = 90L;

    public static boolean hasPermission(Context ctx) {
        return ctx.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ctx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    public static boolean isFine(Context ctx) {
        return ctx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    public static boolean providerEnabled(Context ctx) {
        LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) return false;
        try { return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER); }
        catch (RuntimeException e) { return false; }
    }
    public static final class Fix {
        public final double lat, lon;
        public final float accuracy;
        public final boolean approximate;
        public final long ageSeconds;
        public final String provider;
        Fix(double lat, double lon, float accuracy, boolean approximate, long ageSeconds, String provider) {
            this.lat=lat; this.lon=lon; this.accuracy=accuracy; this.approximate=approximate;
            this.ageSeconds=ageSeconds; this.provider=provider;
        }
        public boolean targetReached() { return !approximate && LocationManager.GPS_PROVIDER.equals(provider) && accuracy <= TARGET_METERS; }
        public String forUser() {
            int decimals = approximate ? 3 : 5;
            return "Position " + (approximate ? "approximative" : "GPS précise demandée") +
                " : latitude " + String.format(Locale.FRENCH, decimals == 3 ? "%.3f" : "%.5f", lat) +
                ", longitude " + String.format(Locale.FRENCH, decimals == 3 ? "%.3f" : "%.5f", lon) +
                ". Précision estimée par Android : " + Math.round(accuracy) + " mètres" +
                (targetReached() ? ". Objectif 3 mètres atteint." : ". Objectif 3 mètres non atteint, valeur non garantie.") +
                " Source : " + provider + ", position datant de " + ageSeconds + " secondes.";
        }
        /** Envoyé uniquement si le consentement location_share est actif pour cette question. */
        public String forAi() {
            String format = approximate ? "%.3f" : "%.5f";
            return "Coordonnées GPS Android autorisées pour cette question seulement : latitude " +
                String.format(Locale.US,format,lat) + ", longitude " + String.format(Locale.US,format,lon) +
                ". Précision mesurée : +/-" + Math.round(accuracy) + " m ; âge " + ageSeconds +
                " s ; " + (approximate ? "localisation approximative" : "autorisation précise") +
                ". Ne considère pas ces coordonnées comme une adresse vérifiée.";
        }
    }
    private static long ageSeconds(Location l) {
        return Math.max(0, TimeUnit.NANOSECONDS.toSeconds(SystemClock.elapsedRealtimeNanos() - l.getElapsedRealtimeNanos()));
    }
    private static boolean acceptable(Location l) {
        return l != null && l.getLatitude() >= -90 && l.getLatitude() <= 90 &&
            l.getLongitude() >= -180 && l.getLongitude() <= 180 && ageSeconds(l) <= MAX_AGE_SEC;
    }
    private static boolean better(Location candidate, Location current) {
        if (!acceptable(candidate)) return false;
        if (!acceptable(current)) return true;
        float a = candidate.hasAccuracy() ? candidate.getAccuracy() : 100_000f;
        float b = current.hasAccuracy() ? current.getAccuracy() : 100_000f;
        // Favorise fortement une meilleure précision, sans utiliser de position ancienne.
        return a < b - 0.5f || (Math.abs(a-b) <= 0.5f && ageSeconds(candidate) < ageSeconds(current));
    }
    public static Fix locate(Context ctx) {
        if (!hasPermission(ctx)) return null;
        LocationManager lm=(LocationManager)ctx.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) return null;
        boolean fine=isFine(ctx);
        String[] providers=fine ? new String[]{LocationManager.GPS_PROVIDER,LocationManager.NETWORK_PROVIDER} :
            new String[]{LocationManager.NETWORK_PROVIDER};
        AtomicReference<Location> best=new AtomicReference<>();
        for(String p:providers) {
            try {
                if (!lm.isProviderEnabled(p)) continue;
                Location l=lm.getLastKnownLocation(p);
                if (better(l,best.get())) best.set(l);
            } catch (SecurityException | IllegalArgumentException ignored) {}
        }
        Location previous=best.get();
        if (previous != null && previous.hasAccuracy() && previous.getAccuracy() <= TARGET_METERS && LocationManager.GPS_PROVIDER.equals(previous.getProvider()) && ageSeconds(previous) <= 15) return toFix(previous,fine);

        // GPS hautes précisions demandées ponctuellement; fin automatique après 16 s.
        LocationListener listener=new LocationListener() {
            @Override public void onLocationChanged(Location value) {
                if (better(value,best.get())) best.set(value);
            }
            @Override public void onProviderEnabled(String provider) {}
            @Override public void onProviderDisabled(String provider) {}
            @Override public void onStatusChanged(String provider,int status,android.os.Bundle extras) {}
        };
        boolean registered=false;
        try {
            for(String p:providers) {
                try {
                    if (!lm.isProviderEnabled(p)) continue;
                    lm.requestLocationUpdates(p, 1000L, 0f, listener, Looper.getMainLooper());
                    registered=true;
                } catch(SecurityException | IllegalArgumentException ignored) {}
            }
            if (registered) {
                long end=SystemClock.elapsedRealtime()+MAX_WAIT_MS;
                while(SystemClock.elapsedRealtime()<end) {
                    Location candidate=best.get();
                    if(candidate!=null && candidate.hasAccuracy() && candidate.getAccuracy() <= TARGET_METERS && LocationManager.GPS_PROVIDER.equals(candidate.getProvider())) break;
                    try { Thread.sleep(500L); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); break; }
                }
            }
        } finally {
            if (registered) try { lm.removeUpdates(listener); } catch(SecurityException ignored) {}
        }
        return toFix(best.get(),fine);
    }
    private static Fix toFix(Location l,boolean fine) {
        if(!acceptable(l)) return null;
        double scale=fine ? 100_000d : 1000d;
        return new Fix(Math.round(l.getLatitude()*scale)/scale,
            Math.round(l.getLongitude()*scale)/scale,
            l.hasAccuracy()?l.getAccuracy():1500f,!fine,ageSeconds(l),l.getProvider()==null?"inconnu":l.getProvider());
    }
}
