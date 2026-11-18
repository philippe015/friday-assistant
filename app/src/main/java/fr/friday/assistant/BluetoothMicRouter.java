package fr.friday.assistant;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Route la capture vocale vers le microphone mains libres du casque Bluetooth.
 * Ne pas confondre A2DP (lecture audio seule) avec SCO/HFP ou BLE_HEADSET.
 * Ne fait jamais de scan Bluetooth ni de recherche de nouveaux appareils.
 */
public final class BluetoothMicRouter {
    private final Context context;
    private final AudioManager audio;
    private final AtomicBoolean routeChanged = new AtomicBoolean(false);
    private AudioDeviceCallback callback;
    private BroadcastReceiver scoReceiver;
    private boolean communicationSelected;
    private volatile boolean legacyScoRequested;
    private volatile boolean captureActive;
    private volatile int legacyScoState = AudioManager.SCO_AUDIO_STATE_DISCONNECTED;
    private boolean modeChanged;
    private int originalMode;
    private boolean enabled;
    private volatile String status = "Micro du téléphone";

    public BluetoothMicRouter(Context context) {
        this.context = context.getApplicationContext();
        this.audio = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
    }

    public static boolean isBluetoothVoiceType(int type) {
        return type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                || type == AudioDeviceInfo.TYPE_BLE_HEADSET;
    }

    public static boolean allowed(Context context) {
        return Build.VERSION.SDK_INT < 31 || context.checkSelfPermission(
                Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    public String status() { return status; }
    public boolean isCommunicationRequested() {
        return communicationSelected || (enabled && (legacyScoRequested
                || legacyScoState == AudioManager.SCO_AUDIO_STATE_CONNECTED));
    }

    /** En cas de refus répété d'AudioRecord Bluetooth, conserver l'écoute du téléphone. */
    public void fallbackToPhone() {
        close();
        status = "Échec de capture Bluetooth : micro du téléphone pour cette session";
    }

    /**
     * Sur Android 8-11, la source TYPE_BLUETOOTH_SCO n'est souvent exposée
     * qu'APRES l'activation du lien SCO. Vérifier d'abord le profil HFP,
     * pas la liste des microphones (sinon la connexion n'est jamais tentée).
     */
    public static boolean legacyHandsfreeConnected(Context context) {
        if (Build.VERSION.SDK_INT >= 31) return false;
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            return adapter != null && adapter.isEnabled()
                    && adapter.getProfileConnectionState(BluetoothProfile.HEADSET)
                            == BluetoothProfile.STATE_CONNECTED;
        } catch (RuntimeException denied) {
            return false;
        }
    }

    private boolean hasHeadsetInput() {
        for (AudioDeviceInfo info : audio.getDevices(AudioManager.GET_DEVICES_INPUTS)) {
            if (info.isSource() && isBluetoothVoiceType(info.getType())) return true;
        }
        return false;
    }

    private void setCommunicationMode() {
        if (modeChanged) return;
        originalMode = audio.getMode();
        if (originalMode != AudioManager.MODE_IN_COMMUNICATION) {
            audio.setMode(AudioManager.MODE_IN_COMMUNICATION);
            modeChanged = true;
        }
    }

    /** Démarrer depuis le service de premier plan, après consentement Android. */
    public void start(boolean preferBluetooth) {
        enabled = preferBluetooth;
        if (!enabled || audio == null) {
            status = "Micro du téléphone (Bluetooth désactivé dans FRIDAY)";
            return;
        }
        if (!allowed(context)) {
            status = "Autorisation Appareils à proximité nécessaire pour le micro Bluetooth";
            return;
        }
        try {
            callback = new AudioDeviceCallback() {
                @Override public void onAudioDevicesAdded(AudioDeviceInfo[] devices) {
                    for (AudioDeviceInfo d : devices)
                        if (isBluetoothVoiceType(d.getType()) && captureActive)
                            routeChanged.set(true);
                }
                @Override public void onAudioDevicesRemoved(AudioDeviceInfo[] devices) {
                    for (AudioDeviceInfo d : devices)
                        if (isBluetoothVoiceType(d.getType()) && captureActive)
                            routeChanged.set(true);
                }
            };
            audio.registerAudioDeviceCallback(callback, new Handler(Looper.getMainLooper()));
            if (Build.VERSION.SDK_INT < 31) {
                // SCO peut mettre plusieurs secondes à s'activer sur Android 8-11.
                scoReceiver = new BroadcastReceiver() {
                    @Override public void onReceive(Context c, Intent intent) {
                        if (AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED.equals(intent.getAction())) {
                            int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
                            int previous = legacyScoState;
                            legacyScoState = state;
                            if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED
                                    && previous != state) {
                                // Première connexion SCO avant AudioRecord : ce n'est
                                // pas un changement de périphérique à reconstruire.
                                if (captureActive) routeChanged.set(true);
                                status = "Bluetooth SCO connecté : préparation du microphone";
                            } else if (state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED
                                    && previous == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                                legacyScoRequested = false;
                                if (captureActive) routeChanged.set(true);
                                status = "Bluetooth SCO déconnecté : reprise de la capture";
                            }
                        }
                    }
                };
                Intent sticky = context.registerReceiver(scoReceiver,
                        new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));
                if (sticky != null)
                    legacyScoState = sticky.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE,
                            AudioManager.SCO_AUDIO_STATE_DISCONNECTED);
            }
            selectRoute();
        } catch (RuntimeException ex) {
            status = "Micro du téléphone : Bluetooth indisponible ("
                    + ex.getClass().getSimpleName() + ")";
        }
    }

    private void selectRoute() {
        if (!enabled || !allowed(context) || audio == null) return;
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                AudioDeviceInfo headset = null;
                for (AudioDeviceInfo info : audio.getAvailableCommunicationDevices()) {
                    if (info.isSink() && isBluetoothVoiceType(info.getType())) {
                        headset = info;
                        break;
                    }
                }
                if (headset == null) {
                    status = "Pas de casque mains libres Bluetooth disponible : micro du téléphone";
                    return;
                }
                setCommunicationMode();
                communicationSelected = audio.setCommunicationDevice(headset);
                status = communicationSelected
                        ? "Casque Bluetooth sélectionné, vérification du microphone en cours"
                        : "Android refuse le profil mains libres : micro du téléphone";
            } else if ((legacyHandsfreeConnected(context) || hasHeadsetInput())
                    && audio.isBluetoothScoAvailableOffCall()) {
                setCommunicationMode();
                if (legacyScoState != AudioManager.SCO_AUDIO_STATE_CONNECTED
                        && !legacyScoRequested) {
                    legacyScoRequested = true;
                    audio.startBluetoothSco();
                    audio.setBluetoothScoOn(true);
                }
                status = legacyScoState == AudioManager.SCO_AUDIO_STATE_CONNECTED
                        ? "Profil HFP/SCO connecté, micro du casque à vérifier"
                        : "Profil HFP détecté, établissement de la liaison SCO...";
            } else {
                status = "Casque absent, profil Appels HFP désactivé ou SCO non disponible : micro du téléphone";
            }
        } catch (SecurityException denied) {
            status = "Autorisation Bluetooth refusée : micro du téléphone";
        } catch (RuntimeException unavailable) {
            status = "Profil mains libres indisponible : micro du téléphone";
        }
    }

    /**
     * Le lien SCO est asynchrone. Attendre sa confirmation AVANT de créer
     * AudioRecord sur Android <= 11. En cas d'échec, libérer le mode appel
     * et conserver le microphone du téléphone sans boucle de redémarrage.
     * Appel exclusivement depuis le thread de capture, jamais l'UI.
     */
    public boolean awaitBluetoothRoute(long timeoutMs) {
        if (!enabled || Build.VERSION.SDK_INT >= 31 || !legacyScoRequested)
            return true;
        long until = SystemClock.elapsedRealtime() + Math.max(1000L, timeoutMs);
        while (enabled && legacyScoState != AudioManager.SCO_AUDIO_STATE_CONNECTED
                && SystemClock.elapsedRealtime() < until
                && !Thread.currentThread().isInterrupted()) {
            SystemClock.sleep(100L);
        }
        if (legacyScoState == AudioManager.SCO_AUDIO_STATE_CONNECTED) return true;
        fallbackToPhone();
        status = "Casque HFP détecté mais liaison SCO impossible : micro du téléphone. "
                + "Vérifier Appels pour ce casque, puis redémarrer l'écoute.";
        return false;
    }

    /** À appeler avant startRecording pour orienter la capture vers la source Bluetooth. */
    public void preferInput(AudioRecord recorder) {
        if (!enabled || !allowed(context) || audio == null || recorder == null) return;
        selectRoute();
        try {
            for (AudioDeviceInfo info : audio.getDevices(AudioManager.GET_DEVICES_INPUTS)) {
                if (info.isSource() && isBluetoothVoiceType(info.getType())) {
                    if (recorder.setPreferredDevice(info)) {
                        status = "Micro Bluetooth demandé ; routage Android à confirmer";
                        return;
                    }
                }
            }
            status = "Micro Bluetooth pas encore prêt : micro du téléphone";
        } catch (RuntimeException failure) {
            status = "Impossible de sélectionner le micro Bluetooth : micro du téléphone";
        }
    }

    /** Retourne le périphérique réellement utilisé et non simplement demandé. */
    public String routedStatus(AudioRecord recorder) {
        if (recorder == null || recorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING)
            return status;
        try {
            AudioDeviceInfo actual = recorder.getRoutedDevice();
            if (actual != null && isBluetoothVoiceType(actual.getType())) {
                status = "Microphone du casque Bluetooth actif (capture confirmée par Android)";
            } else if (enabled && allowed(context)) {
                status = "Microphone du téléphone actif ; casque Bluetooth non routé";
            } else if (!enabled) {
                status = "Microphone du téléphone actif";
            }
        } catch (RuntimeException ignored) {
            // Le routage n'est pas toujours interrogable sur certains fabricants.
        }
        return status;
    }

    /** Acquitte la connexion SCO initiale après ouverture effective d'AudioRecord. */
    public void captureStarted() {
        routeChanged.set(false);
        captureActive = true;
    }

    /** Un changement après la capture initiale impose seul une reprise. */
    public void captureStopped() { captureActive = false; }
    public boolean consumeRouteChange() { return routeChanged.getAndSet(false); }

    /** Android peut annoncer SCO connecté tout en enregistrant sur le téléphone. */
    public boolean wrongInputRouted(AudioRecord recorder) {
        if (!isCommunicationRequested() || recorder == null
                || recorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING)
            return false;
        AudioDeviceInfo actual = recorder.getRoutedDevice();
        return actual != null && !isBluetoothVoiceType(actual.getType());
    }

    public void close() {
        enabled = false;
        captureActive = false;
        if (audio != null) {
            if (callback != null) {
                try { audio.unregisterAudioDeviceCallback(callback); }
                catch (RuntimeException ignored) { }
                callback = null;
            }
            if (scoReceiver != null) {
                try { context.unregisterReceiver(scoReceiver); }
                catch (RuntimeException ignored) { }
                scoReceiver = null;
            }
            if (Build.VERSION.SDK_INT >= 31 && communicationSelected) {
                try { audio.clearCommunicationDevice(); }
                catch (RuntimeException ignored) { }
            }
            if (legacyScoRequested) {
                try { audio.setBluetoothScoOn(false); audio.stopBluetoothSco(); }
                catch (RuntimeException ignored) { }
            }
            if (modeChanged) {
                try { audio.setMode(originalMode); }
                catch (RuntimeException ignored) { }
            }
        }
        communicationSelected = false;
        legacyScoRequested = false;
        legacyScoState = AudioManager.SCO_AUDIO_STATE_DISCONNECTED;
        modeChanged = false;
        routeChanged.set(false);
        status = "Microphone arrêté";
    }
}
