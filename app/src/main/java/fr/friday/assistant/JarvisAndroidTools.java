package fr.friday.assistant;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.StatFs;
import java.util.Locale;

/**
 * Whitelisted Android-native tools, inspired by OpenJarvis's tool primitive.
 * All diagnostics run on-device: no remote LLM, Web query, GPS, shell,
 * reflection, unreviewed plugins, account secrets or conversation export.
 */
public final class JarvisAndroidTools {
    private JarvisAndroidTools() { }

    public static String answer(Context context, SecurePrefs settings, String utterance) {
        if (utterance == null || utterance.trim().length() > 5000) return null;
        // L'utilisateur n'a pas indiqué quelle présidentielle. Ne pas faire
        // une longue recherche Web aveugle ni inventer des candidats.
        String clarification = ElectionQuestionPolicy.clarification(utterance);
        if (clarification != null) return clarification;
        // L'outil météo consulte un vrai service pour une ville explicite,
        // jamais la position Android ni une localisation déduite.
        String weather = JarvisWeather.answer(utterance, JarvisInternet.enabled(settings));
        if (weather != null) return weather;
        // « Et demain ? » reprend une ville déjà citée par notre propre
        // outil météo. Seul le nom de la ville entre dans la requête HTTPS.
        String weatherFollowUp = JarvisWeather.followUp(context, settings, utterance);
        if (weatherFollowUp != null) return weatherFollowUp;
        JarvisToolPolicy.Kind kind = JarvisToolPolicy.classify(utterance);
        if (kind == JarvisToolPolicy.Kind.NONE) {
            // Reuse existing deterministic on-device tools before HTTPS or
            // loading a multi-GB model. Do not answer a city-specific time
            // question with the phone's local time.
            String cleaned = VoiceText.withoutWakeWord(utterance);
            String arithmetic = LocalJarvisArithmetic.tryAnswer(cleaned);
            if (arithmetic != null) return arithmetic;
            return NativeAI.localAnswer(cleaned);
        }
        switch (kind) {
            case CAPABILITIES: return capabilities(context, settings);
            case MAPS: return mapsAccess(context, settings);
            case MICROPHONE: return microphone(context);
            case MODEL: return model(context, settings);
            case DEVICE: return device(context);
            case PRIVACY: return privacy(settings);
            case SYSTEM: return system(context, settings);
            default: return null;
        }
    }

    private static String capabilities(Context context, SecurePrefs cfg) {
        return "Je suis JARVIS, assistant local sur Android. "
                + "Mes outils réellement disponibles : reconnaissance vocale Vosk si "
                + "le modèle de français est installé, synthèse vocale Android, "
                + "calculs simples, date et heure du téléphone, état du micro, "
                + "du modèle IA et de la batterie, mémoire locale chiffrée, "
                + "météo vérifiée par ville nommée et recherche de pages Internet "
                + "publiques si elle est autorisée. "
                + (LocalJarvisEngine.ready(context)
                    ? "Un fichier modèle IA local est importé ; son fonctionnement doit "
                      + "être confirmé par un test de génération. "
                    : "Aucun modèle IA local compatible n'est importé : "
                      + "je ne peux pas générer de conversation libre pour le moment. ")
                + (JarvisInternet.enabled(cfg)
                    ? "La recherche Web est autorisée, mais son accès réel dépend du réseau. "
                    : "La recherche Web est désactivée. ")
                + "Je ne pilote pas le système Android, les autres applications ou "
                + "les appareils connectés sans outil spécifique autorisé.";
    }

    /** Réponse locale à une question d'accès à Maps, sans recherche de
     * mots-clés Web mal ciblée ni prétention d'avoir Google Maps en direct. */
    private static String mapsAccess(Context context, SecurePrefs cfg) {
        boolean gpsPermission = FridayLocation.hasPermission(context);
        boolean gpsEnabled = FridayLocation.providerEnabled(context);
        return "Je n'ai pas d'accès direct à l'application Google Maps, à sa carte "
                + "interactive ni à la navigation en temps réel. "
                + "Je peux utiliser la localisation fournie par Android "
                + "si vous l'autorisez ; permission de localisation "
                + (gpsPermission ? "accordée" : "non accordée")
                + ", service de localisation "
                + (gpsEnabled ? "activé" : "désactivé") + ". "
                + (JarvisInternet.enabled(cfg)
                    ? "Je peux aussi consulter certaines pages Web publiques, "
                      + "mais cela ne donne pas accès au contenu privé ni à l'API Google Maps."
                    : "Mes recherches Web sont actuellement désactivées.")
                + " Pour obtenir un itinéraire ou examiner une carte réelle, "
                + "ouvrez l'application Maps sur le téléphone.";
    }

    private static String microphone(Context context) {
        boolean permission = context.checkSelfPermission(
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        boolean model = ModelInstaller.installed(context);
        boolean running = FridayService.isActive();
        return "Diagnostic microphone local : permission Android "
                + (permission ? "autorisée" : "absente")
                + " ; modèle vocal Vosk français " + (model ? "installé" : "manquant")
                + " ; service d'écoute JARVIS " + (running ? "actif" : "arrêté")
                + ". " + (running ? "État déclaré par le service : "
                        + FridayService.voiceReadiness()
                        : "Ouvrez JARVIS et autorisez le micro pour démarrer l'écoute.")
                + " Ces indicateurs ne prouvent pas à eux seuls qu'une phrase "
                + "a été correctement transcrite.";
    }

    private static String model(Context context, SecurePrefs cfg) {
        boolean imported = LocalJarvisEngine.ready(context);
        return "Diagnostic du moteur IA local : " + LocalJarvisEngine.status(context, cfg)
                + ". " + (imported
                    ? "Le fichier est présent, mais seul un test de génération "
                      + "permet de confirmer la compatibilité du modèle et du GPU."
                    : "Pour répondre librement, importez dans Réglages un modèle "
                      + ".task ou .litertlm compatible avec le téléphone.")
                + " " + LocalModelAdvisor.advice(context)
                + " Aucune requête d'inférence n'est envoyée à un modèle cloud.";
    }

    private static String device(Context context) {
        StringBuilder response = new StringBuilder(
                "Diagnostic de mon environnement Android, réalisé localement. ");
        try {
            Intent battery = context.registerReceiver(null,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (battery != null) {
                int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                if (level >= 0 && scale > 0)
                    response.append("Batterie : ")
                            .append(Math.round(level * 100f / scale)).append(" %. ");
                int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                if (status == BatteryManager.BATTERY_STATUS_CHARGING
                        || status == BatteryManager.BATTERY_STATUS_FULL)
                    response.append("En charge. ");
            }
        } catch (RuntimeException unavailable) {
            response.append("Batterie : information indisponible. ");
        }
        try {
            ActivityManager manager = (ActivityManager) context.getSystemService(
                    Context.ACTIVITY_SERVICE);
            if (manager != null) {
                ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
                manager.getMemoryInfo(info);
                response.append("RAM disponible estimée : ")
                        .append(String.format(Locale.FRANCE, "%.1f",
                                info.availMem / 1_073_741_824d))
                        .append(" Go. ")
                        .append(info.lowMemory ? "Android signale une mémoire basse. " : "");
            }
        } catch (RuntimeException unavailable) {
            response.append("RAM : indisponible. ");
        }
        try {
            StatFs storage = new StatFs(context.getFilesDir().getAbsolutePath());
            response.append("Stockage interne disponible : ")
                    .append(String.format(Locale.FRANCE, "%.1f",
                            storage.getAvailableBytes() / 1_073_741_824d))
                    .append(" Go. ");
        } catch (RuntimeException unavailable) {
            response.append("Stockage : indisponible. ");
        }
        return response.append("Je n'ai consulté ni GPS, ni données personnelles, ni Internet.")
                .toString();
    }

    private static String privacy(SecurePrefs cfg) {
        return "Les dialogues enregistrés par JARVIS et ses notes privées sont "
                + "stockés localement avec chiffrement AES-GCM par Android Keystore. "
                + "La mémoire explicite est "
                + ("1".equals(cfg.get("memory_enabled", "0"))
                    ? "activée" : "désactivée")
                + ". Seules les recherches Internet publiques peuvent transmettre "
                + "les termes de la question aux moteurs consultés. "
                + "L'historique des échanges, les notes privées et le GPS ne sont "
                + "pas envoyés aux moteurs de recherche. "
                + "Consultez les réglages pour effacer l'historique ou les notes. "
                + "Un historique ancien absent ou illisible ne peut pas être retrouvé "
                + "magiquement.";
    }

    private static String system(Context context, SecurePrefs cfg) {
        return "JARVIS, diagnostic sur ce téléphone. "
                + "Micro : " + (FridayService.isActive() ? "écoute active" : "arrêté")
                + ", Vosk : " + (ModelInstaller.installed(context)
                    ? "installé" : "manquant")
                + ", IA locale : " + (LocalJarvisEngine.ready(context)
                    ? "modèle importé (inférence non testée)" : "modèle absent")
                + ", Web : " + (JarvisInternet.enabled(cfg)
                    ? "autorisé, connectivité non vérifiée" : "désactivé")
                + ". Pour les détails, dites « Jarvis, diagnostic microphone », "
                + "« Jarvis, quel est ton modèle IA ? » ou "
                + "« Jarvis, état de mon téléphone ».";
    }
}
