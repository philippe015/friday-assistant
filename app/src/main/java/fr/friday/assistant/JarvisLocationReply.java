package fr.friday.assistant;

import android.content.Context;

/** Réponses géolocalisées aux questions écrites, sur un thread secondaire. */
public final class JarvisLocationReply {
    private JarvisLocationReply() {}

    public static String answer(Context ctx, SecurePrefs prefs, String question) {
        if (!LocationIntent.nearby(question)) return null;
        DiagnosticLog.record(ctx, "GPS_TEXT_REQUEST");
        if (!"1".equals(prefs.get("location_enabled", "0"))) {
            DiagnosticLog.record(ctx, "GPS_DISABLED");
            return "La localisation JARVIS est désactivée. Activez-la dans les réglages, "
                    + "puis autorisez le GPS Android. Les coordonnées restent locales "
                    + "sauf si vous activez séparément la recherche de proximité en ligne.";
        }
        if (!FridayLocation.hasPermission(ctx)) {
            DiagnosticLog.record(ctx, "GPS_PERMISSION_MISSING");
            return "La permission GPS Android manque. Ouvrez Réglages puis "
                    + "« Tester précision GPS » pour accorder cette permission.";
        }
        if (!FridayLocation.providerEnabled(ctx)) {
            DiagnosticLog.record(ctx, "GPS_PROVIDER_OFF");
            return "La localisation est désactivée dans les paramètres Android. "
                    + "Activez le service de localisation puis réessayez.";
        }
        DiagnosticLog.record(ctx, "GPS_ACQUIRING");
        FridayLocation.Fix fix = FridayLocation.locate(ctx);
        if (fix == null) {
            DiagnosticLog.record(ctx, "GPS_FIX_MISSING");
            return "Android n'a pas fourni de position récente en 16 secondes. "
                    + "Vérifiez la localisation précise et recommencez dehors.";
        }
        DiagnosticLog.record(ctx, "GPS_FIX_REPORTED");
        if (LocationIntent.direct(question)) {
            String place = "1".equals(prefs.get("address_lookup", "0"))
                    ? AndroidPlaceResolver.resolve(ctx, fix) : "";
            return AndroidPlaceResolver.userText(fix, place)
                    + " Coordonnées non transmises à un modèle d'IA distant.";
        }
        if (!"1".equals(prefs.get("nearby_online_enabled", "0"))) {
            return "Position Android obtenue (précision estimée : "
                    + Math.round(fix.accuracy) + " m). Pour trouver les services réels "
                    + "autour de vous, activez dans Réglages « Rechercher les services "
                    + "à proximité avec OpenStreetMap ». Cette option partage uniquement "
                    + "une position arrondie à environ 100 mètres, sur demande.";
        }
        if (!JarvisInternet.enabled(prefs)) {
            return "Position Android obtenue, mais Internet JARVIS est désactivé. "
                    + "Activez Internet JARVIS pour rechercher les services OpenStreetMap.";
        }
        return NearbyPlaceSearch.search(ctx, fix);
    }
}
