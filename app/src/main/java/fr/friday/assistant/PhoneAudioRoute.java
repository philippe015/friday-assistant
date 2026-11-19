package fr.friday.assistant;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.os.Build;
import android.util.Log;

/**
 * FRIDAY : microphone et haut-parleur intégrés du téléphone uniquement.
 * Ne crée pas de lien HFP/SCO et ne demande pas d'autorisation Bluetooth.
 * Une application tierce ou une politique Android peut reprendre le routage ;
 * l'état réel de l'entrée est vérifié avec AudioRecord.getRoutedDevice().
 */
public final class PhoneAudioRoute {
    private PhoneAudioRoute() { }

    /** Préférer le haut-parleur intégré avant chaque sortie vocale. */
    public static void usePhoneSpeaker(Context context) {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audio == null) return;
        try {
            if (audio.getMode() != AudioManager.MODE_IN_COMMUNICATION)
                audio.setMode(AudioManager.MODE_IN_COMMUNICATION);
            if (Build.VERSION.SDK_INT >= 31) {
                for (AudioDeviceInfo device : audio.getAvailableCommunicationDevices()) {
                    if (device.isSink()
                            && device.getType() == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                        if (!audio.setCommunicationDevice(device)) {
                            // Secours pour les périphériques refusant la nouvelle API.
                            audio.setSpeakerphoneOn(true);
                            Log.w("FRIDAY_PHONE_AUDIO", "Android a refusé le routage de communication : repli haut-parleur");
                        }
                        return;
                    }
                }
                audio.setSpeakerphoneOn(true);
                Log.w("FRIDAY_PHONE_AUDIO", "Haut-parleur non listé : repli vers sortie téléphone");
            } else {
                audio.setSpeakerphoneOn(true);
            }
        } catch (RuntimeException denied) {
            Log.w("FRIDAY_PHONE_AUDIO", "Routage haut-parleur refusé", denied);
        }
    }

    /** Selection explicite de la source interne : jamais de micro HFP ni BLE. */
    public static boolean preferBuiltInMicrophone(Context context, AudioRecord recorder) {
        if (recorder == null) return false;
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audio == null) return false;
        try {
            for (AudioDeviceInfo input : audio.getDevices(AudioManager.GET_DEVICES_INPUTS)) {
                if (input.isSource() && input.getType() == AudioDeviceInfo.TYPE_BUILTIN_MIC)
                    return recorder.setPreferredDevice(input);
            }
        } catch (RuntimeException denied) {
            Log.w("FRIDAY_PHONE_AUDIO", "Sélection du micro interne impossible", denied);
        }
        return false;
    }

    /** Etat vérifié pour le diagnostic ; ne jamais afficher une confirmation fictive. */
    public static String inputStatus(AudioRecord recorder) {
        if (recorder == null) return "Microphone intégré non démarré";
        AudioDeviceInfo actual = recorder.getRoutedDevice();
        if (actual == null) return "Entrée microphone non confirmée par Android";
        return actual.getType() == AudioDeviceInfo.TYPE_BUILTIN_MIC
                ? "Microphone intégré du téléphone actif (Android confirmé)"
                : "Android utilise une autre entrée microphone (type " + actual.getType() + ")";
    }

    public static AudioAttributes speechAttributes() {
        return new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).build();
    }

    /** Rend la route communication à Android lors de l'arrêt du service. */
    public static void release(Context context) {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audio == null) return;
        try {
            if (Build.VERSION.SDK_INT >= 31) audio.clearCommunicationDevice();
            else audio.setSpeakerphoneOn(false);
            if (audio.getMode() == AudioManager.MODE_IN_COMMUNICATION)
                audio.setMode(AudioManager.MODE_NORMAL);
        } catch (RuntimeException ignored) {
            Log.w("FRIDAY_PHONE_AUDIO", "Routage Android conservé après arrêt");
        }
    }
}
