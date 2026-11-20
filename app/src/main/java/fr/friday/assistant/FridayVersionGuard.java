package fr.friday.assistant;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import java.util.ArrayList;
import java.util.List;

/** Vérifie uniquement les identifiants connus, sans permission QUERY_ALL_PACKAGES. */
final class FridayVersionGuard {
    static final class Installed {
        final String packageName;
        final String versionName;
        Installed(String pkg, String version) {
            packageName = pkg;
            versionName = version == null ? "version inconnue" : version;
        }
        String display() { return versionName + " — " + packageName; }
    }

    private FridayVersionGuard() { }

    static List<Installed> installed(Context context) {
        List<Installed> result = new ArrayList<>();
        PackageManager pm = context.getPackageManager();
        String own = context.getPackageName();
        for (String pkg : FridayVersionPolicy.KNOWN_PACKAGES) {
            if (!FridayVersionPolicy.isOtherVersion(own, pkg)) continue;
            try {
                @SuppressWarnings("deprecation")
                PackageInfo info = pm.getPackageInfo(pkg, 0);
                result.add(new Installed(pkg, info.versionName));
            } catch (PackageManager.NameNotFoundException ignored) {
                // L'édition n'est pas installée (ou sa visibilité est limitée).
            } catch (SecurityException ignored) {
                // Ne jamais prétendre qu'une application non visible est arrêtée.
            }
        }
        return result;
    }

    static String thisVersion(Context context) {
        try {
            @SuppressWarnings("deprecation")
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return "v" + info.versionName + " · " + context.getPackageName();
        } catch (Exception ignored) { return context.getPackageName(); }
    }

    static void openAppDetails(Activity activity, String packageName) {
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + packageName));
        activity.startActivity(i);
    }

    static void showManager(Activity activity) {
        List<Installed> others = installed(activity);
        if (others.isEmpty()) {
            new AlertDialog.Builder(activity)
                .setTitle("Versions JARVIS")
                .setMessage("Édition actuelle : " + thisVersion(activity) + "\n\n" +
                    "Aucune autre édition connue n'est visible. Cela ne prouve pas qu'aucune autre application utilise le micro.")
                .setPositiveButton("Fermer", null).show();
            return;
        }
        String[] labels = new String[others.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = others.get(i).display();
        new AlertDialog.Builder(activity)
            .setTitle("Autres éditions installées")
            .setItems(labels, (d, index) -> openAppDetails(activity, others.get(index).packageName))
            .setNegativeButton("Fermer", null).show();
    }
}
