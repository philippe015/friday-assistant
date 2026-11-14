package fr.friday.assistant;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.media.*;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;
import android.os.*;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import org.json.JSONArray;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import android.util.Log;
import android.os.SystemClock;

/** Service réellement indépendant du WebView : micro Vosk 16 kHz, Mistral et TTS écran éteint. */
public class FridayService extends Service {
    public static final String ACTION_START = "fr.friday.assistant.START";
    public static final String ACTION_STOP = "fr.friday.assistant.STOP";
    public static final String ACTION_EVENT = "fr.friday.assistant.EVENT";
    public static final String ACTION_INTERRUPT = "fr.friday.assistant.INTERRUPT";
    private static final String CHANNEL = "friday_ecoute";
    private static final int NOTIFICATION_ID = 9717;
    private static volatile boolean active = false;
    private static volatile boolean micSilenced = false;
    private volatile boolean running = false;
    private Thread worker;
    private AudioRecord recorder;
    private TextToSpeech tts;
    private PowerManager.WakeLock wakeLock;
    private final ArrayDeque<JSONObject> history = new ArrayDeque<>();
    private volatile long lastReplyAt = 0;
    private int silenceMs = 2100;
    private long followupMs = 75000;
    private volatile boolean wakeArmed = false;  // shared with the Mistral response thread
    private volatile long bargePendingUntil = 0;
    private volatile long lastSpeechEndedAt = 0;
    private volatile String spokenText = "";
    private volatile boolean speakingNow = false;
    private volatile boolean echoProtection = false;
    private volatile ResponseTurn currentTurn;
    private AcousticEchoCanceler echoCanceler;
    private NoiseSuppressor noiseSuppressor;
    private SecurePrefs settings;
    public static final String ACTION_ASK_TEXT = "fr.friday.assistant.ASK_TEXT";
    private final ConcurrentLinkedQueue<String> textQuestions = new ConcurrentLinkedQueue<>();
    private final AtomicInteger queuedQuestions = new AtomicInteger(0);
    private volatile boolean ttsReady = false;
    private volatile String ttsDiagnostic = "non initialisé";

    public static boolean isActive() { return active; }
    public static boolean isMicSilenced() { return micSilenced; }
    private static final class ResponseTurn {
        final NativeAI.CancelToken api = new NativeAI.CancelToken();
        final MistralVoice.Playback audio = new MistralVoice.Playback();
        volatile Thread thread;
        volatile boolean cancelled;
        void cancel() {
            cancelled = true;
            audio.cancel(); // Couper le haut-parleur avant la fermeture éventuelle du réseau.
            api.cancel();
            Thread t = thread;
            if (t != null) t.interrupt();
        }
    }
    private boolean live(ResponseTurn turn) { return running && currentTurn == turn && !turn.cancelled; }
    public void onCreate() {
        super.onCreate();
        settings = new SecurePrefs(this);
        NotificationChannel ch = new NotificationChannel(CHANNEL, "F.R.I.D.A.Y. · écoute vocale", NotificationManager.IMPORTANCE_LOW);
        ch.setDescription("Microphone actif tant que vous avez démarré le mode écran éteint");
        getSystemService(NotificationManager.class).createNotificationChannel(ch);
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            shutdown(); stopSelf(); return START_NOT_STICKY;
        }
        if (intent != null && ACTION_INTERRUPT.equals(intent.getAction())) {
            if (running) interruptResponse("Réponse interrompue. Je vous écoute.");
            return START_NOT_STICKY;
        }
        if (intent != null && ACTION_ASK_TEXT.equals(intent.getAction())) {
            if (running) {
                String question = intent.getStringExtra("question");
                if (question != null && !question.trim().isEmpty()) {
                    String clean = VoiceText.withoutWakeWord(question.trim());
                    if (clean.length() > 5000) {
                        event("error", "Question trop longue (maximum 5 000 caractères).");
                    } else if (queuedQuestions.incrementAndGet() > 3) {
                        queuedQuestions.decrementAndGet();
                        event("error", "Trop de questions en attente. Réessayez après la réponse.");
                    } else {
                        // Un texte explicite a priorité sur la réponse vocale en cours.
                        if (currentTurn != null) interruptResponse("Réponse interrompue par une nouvelle question.");
                        textQuestions.offer("Friday " + clean);
                        event("status", "Question texte reçue, écoute maintenue active…");
                    }
                }
            } else {
                event("error", "Démarrez d'abord l'écoute native pour tester une question.");
                stopSelf();
            }
            return START_NOT_STICKY;
        }
        if (running) return START_NOT_STICKY;
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                !ModelInstaller.installed(this)) {
            event("error", "Autorisez le micro et installez le modèle vocal français.");
            stopSelf(); return START_NOT_STICKY;
        }
        try {
            // Démarré quand l'activité est visible : le service garde micro et, si autorisée,
            // localisation à la demande pendant que l'écran est verrouillé.
            boolean useLocation = "1".equals(settings.get("location_enabled", "0")) &&
                    FridayLocation.hasPermission(this) && FridayLocation.providerEnabled(this);
            if (Build.VERSION.SDK_INT >= 29) {
                int types = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;
                if (useLocation) types |= ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
                startForeground(NOTIFICATION_ID, notification("Écoute native · écran allumé ou verrouillé"), types);
            } else startForeground(NOTIFICATION_ID, notification("Écoute native · écran allumé ou verrouillé"));
        } catch (Exception ex) {
            event("error", "Service microphone refusé : démarrer l'écoute depuis l'application ouverte.");
            stopSelf(); return START_NOT_STICKY;
        }
        running = true;
        active = true;
        micSilenced = false;
        silenceMs = settingInt("silence_ms", 2100, 900, 6000);
        followupMs = settingInt("followup_sec", 75, 15, 180) * 1000L;
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FRIDAY:MicroService");
        wakeLock.setReferenceCounted(false);
        wakeLock.acquire();
        event("status", "Écoute native active, écran allumé ou verrouillé. Dites Friday.");
        worker = new Thread(this::runLoop, "friday-voice-core");
        worker.start();
        return START_NOT_STICKY; // Pas de redémarrage clandestin après arrêt par le système.
    }
    private int settingInt(String name, int fallback, int min, int max) {
        try { return Math.max(min, Math.min(max, Integer.parseInt(settings.get(name, "" + fallback)))); }
        catch (Exception ignored) { return fallback; }
    }
    private Notification notification(String message) {
        Intent stop = new Intent(this, FridayService.class).setAction(ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getService(this, 2, stop, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent interruptPi = PendingIntent.getService(this, 3,
            new Intent(this, FridayService.class).setAction(ACTION_INTERRUPT),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent show = new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent showPi = PendingIntent.getActivity(this, 1, show, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("F.R.I.D.A.Y. · écoute activée")
                .setContentText(message)
                .setContentIntent(showPi)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_media_pause, "Couper la réponse", interruptPi)
                .addAction(android.R.drawable.ic_delete, "Arrêter le micro", stopPi)
                .build();
    }
    private void event(String kind, String value) {
        Intent msg = new Intent(ACTION_EVENT).setPackage(getPackageName());
        msg.putExtra("kind", kind);
        msg.putExtra("text", value);
        if (kind.equals("user") || kind.equals("assistant")) {
            try {
                android.content.SharedPreferences sp = getSharedPreferences("friday_voice_history", MODE_PRIVATE);
                long seq = sp.getLong("seq", 0) + 1;
                JSONArray list = new JSONArray(sp.getString("events", "[]"));
                list.put(new JSONObject().put("seq", seq).put("kind", kind).put("text", value));
                while (list.length() > 40) list.remove(0);
                sp.edit().putLong("seq", seq).putString("events", list.toString()).apply();
                msg.putExtra("seq", seq);
            } catch (Exception ignored) { }
        }
        sendBroadcast(msg);
        if (kind.equals("status") || kind.equals("error")) {
            try { getSystemService(NotificationManager.class).notify(NOTIFICATION_ID, notification(value)); }
            catch (Exception ignored) { }
        }
    }
    private static String parse(String json, String key) {
        try { return new JSONObject(json).optString(key, "").trim(); }
        catch (Exception ignored) { return ""; }
    }
    /** Capture Vosk continue pendant les appels Mistral et la lecture audio. */
    private void runLoop() {
        try (Model model = new Model(ModelInstaller.modelDir(this).getAbsolutePath())) {
            event("status", "Reconnaissance française prête : vous pouvez interrompre les réponses avec « Friday stop ». ");
            // Ne pas bloquer 15 secondes le micro en attendant le moteur TTS Android.
            new Thread(this::initTts, "friday-tts-setup").start();
            int consecutiveErrors = 0;
            while (running) {
                AudioRecord ar = null;
                Recognizer recognizer = null;
                try {
                    int minimum = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT);
                    if (minimum <= 0) throw new IllegalStateException("Microphone 16 kHz indisponible");
                    int bytes = Math.max(8192, minimum * 2);
                    boolean bargeEnabled = "1".equals(settings.get("barge_in_enabled", "1"));
                    int src = bargeEnabled ? MediaRecorder.AudioSource.VOICE_COMMUNICATION : MediaRecorder.AudioSource.VOICE_RECOGNITION;
                    ar = new AudioRecord(src, 16000, AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, bytes);
                    if (ar.getState() != AudioRecord.STATE_INITIALIZED && bargeEnabled) {
                        ar.release();
                        ar = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, 16000,
                            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bytes);
                    }
                    if (ar.getState() != AudioRecord.STATE_INITIALIZED)
                        throw new IllegalStateException("Échec ouverture microphone");
                    recorder = ar;
                    echoProtection = false;
                    if (bargeEnabled) {
                        try {
                            if (AcousticEchoCanceler.isAvailable()) {
                                echoCanceler = AcousticEchoCanceler.create(ar.getAudioSessionId());
                                if (echoCanceler != null) echoProtection = (echoCanceler.setEnabled(true) == 0 && echoCanceler.getEnabled());
                            }
                            if (NoiseSuppressor.isAvailable()) {
                                noiseSuppressor = NoiseSuppressor.create(ar.getAudioSessionId());
                                if (noiseSuppressor != null) noiseSuppressor.setEnabled(true);
                            }
                        } catch (Exception effectError) { Log.w("FRIDAY_MIC", "Effets audio non disponibles", effectError); }
                    }
                    final java.util.concurrent.atomic.AtomicBoolean silencedByPolicy =
                        new java.util.concurrent.atomic.AtomicBoolean(false);
                    if (Build.VERSION.SDK_INT >= 29) {
                        final AudioRecord monitored = ar;
                        monitored.registerAudioRecordingCallback(getMainExecutor(), new AudioManager.AudioRecordingCallback() {
                            @Override public void onRecordingConfigChanged(java.util.List<AudioRecordingConfiguration> c) {
                                try {
                                    AudioRecordingConfiguration own = monitored.getActiveRecordingConfiguration();
                                    if (own != null) silencedByPolicy.set(own.isClientSilenced());
                                } catch (Exception ignored) { }
                            }
                        });
                    }
                    recognizer = new Recognizer(model, 16000.0f);
                    ar.startRecording();
                    event("status", "Micro actif pendant les réponses. Anti-écho " +
                        (echoProtection ? "activé." : "non garanti : préférez des écouteurs."));
                    byte[] buffer = new byte[4096];
                    long lastPolicyCheck = 0, lastVoice = 0, lastPartialPublished = 0;
                    String completed = "", interim = "";
                    boolean earlyCancelled = false;
                    boolean sawSpeakerFeedback = false;
                    while (running) {
                        // Une question tapée n'attend pas la fin de l'appel réseau précédent.
                        String typed = textQuestions.poll();
                        if (typed != null) {
                            queuedQuestions.updateAndGet(n -> Math.max(0, n - 1));
                            handlePhrase(typed);
                            recognizer.close(); recognizer = new Recognizer(model, 16000.0f);
                            completed = interim = ""; lastVoice = 0;
                            earlyCancelled = false; sawSpeakerFeedback = false;
                        }
                        int count = ar.read(buffer, 0, buffer.length);
                        if (count <= 0) {
                            if (running) throw new IllegalStateException("Lecture microphone interrompue : " + count);
                            break;
                        }
                        consecutiveErrors = 0;
                        if (Build.VERSION.SDK_INT >= 29) {
                            long tick = SystemClock.elapsedRealtime();
                            if (tick - lastPolicyCheck >= 500) {
                                lastPolicyCheck = tick;
                                try {
                                    AudioRecordingConfiguration own = ar.getActiveRecordingConfiguration();
                                    if (own != null) silencedByPolicy.set(own.isClientSilenced());
                                } catch (Exception ignored) { }
                            }
                        }
                        if (silencedByPolicy.get()) {
                            if (!micSilenced) {
                                micSilenced = true;
                                event("mic_conflict", "Android a rendu le micro muet. Vérifiez les applications vocales concurrentes.");
                            }
                            recognizer.close(); recognizer = new Recognizer(model, 16000.0f);
                            completed = interim = ""; lastVoice = 0;
                            earlyCancelled = false; sawSpeakerFeedback = false;
                            continue;
                        }
                        if (micSilenced) {
                            micSilenced = false;
                            event("status", "Microphone de nouveau accessible. Dites Friday.");
                        }
                        boolean endpoint = recognizer.acceptWaveForm(buffer, count);
                        long now = SystemClock.elapsedRealtime();
                        if (endpoint) {
                            String segment = parse(recognizer.getResult(), "text");
                            if (!segment.isEmpty()) {
                                completed = VoiceText.join(completed, segment);
                                lastVoice = now;
                                if (now - lastPartialPublished >= 250) {
                                    event("partial", completed); lastPartialPublished = now;
                                }
                            }
                            interim = "";
                        } else {
                            String next = parse(recognizer.getPartialResult(), "partial");
                            if (!next.isEmpty() && !next.equals(interim)) {
                                interim = next;
                                lastVoice = now;
                                if (now - lastPartialPublished >= 250) {
                                    event("partial", VoiceText.join(completed, interim)); lastPartialPublished = now;
                                }
                            }
                        }
                        String candidate = VoiceText.join(completed, interim);
                        if (currentTurn != null && !BargeInPolicy.startsWithWake(candidate))
                            sawSpeakerFeedback = true;
                        if (!earlyCancelled && currentTurn != null && bargeEnabled &&
                                BargeInPolicy.earlyBarge(candidate, spokenText)) {
                            earlyCancelled = true;
                            interruptResponse("Interruption détectée. Je vous écoute pour la suite de la question.");
                        }
                        int timeout = (currentTurn != null || earlyCancelled ||
                            now < bargePendingUntil) ? Math.min(silenceMs, 1300) : silenceMs;
                        if (lastVoice > 0 && now - lastVoice >= timeout &&
                                (!completed.isEmpty() || !interim.isEmpty())) {
                            String tail = parse(recognizer.getFinalResult(), "text");
                            String phrase = VoiceText.join(completed, tail);
                            if (phrase.isEmpty()) phrase = interim;
                            // Vosk fermé/réinitialisé, mais le micro reste ouvert sans interruption.
                            recognizer.close(); recognizer = new Recognizer(model, 16000.0f);
                            boolean dropSpeaker = sawSpeakerFeedback &&
                                !BargeInPolicy.startsWithWake(phrase) &&
                                SystemClock.elapsedRealtime() - lastSpeechEndedAt < 3000L;
                            completed = interim = ""; lastVoice = 0;
                            earlyCancelled = false; sawSpeakerFeedback = false;
                            if (!dropSpeaker && !phrase.trim().isEmpty()) handlePhrase(phrase.trim());
                        }
                    }
                } catch (Exception captureError) {
                    if (!running) break;
                    consecutiveErrors++;
                    event("error", "Microphone interrompu : nouvelle tentative " + consecutiveErrors + "/3.");
                    if (consecutiveErrors >= 3) throw captureError;
                    try { Thread.sleep(400L * consecutiveErrors); }
                    catch (InterruptedException ignored) { Thread.currentThread().interrupt(); break; }
                } finally {
                    releaseAudioEffects();
                    if (ar != null) {
                        try { if (ar.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) ar.stop(); }
                        catch (Exception ignored) { }
                        ar.release();
                    }
                    recorder = null;
                    if (recognizer != null) recognizer.close();
                }
            }
        } catch (Exception e) {
            if (running) event("error", "Moteur vocal : " + e.getMessage());
        } finally { shutdown(); stopSelf(); }
    }
    private void releaseAudioEffects() {
        try { if (echoCanceler != null) echoCanceler.release(); } catch (Exception ignored) { }
        try { if (noiseSuppressor != null) noiseSuppressor.release(); } catch (Exception ignored) { }
        echoCanceler = null; noiseSuppressor = null; echoProtection = false;
    }
    /** Rend le micro disponible immédiatement après une interruption. */
    private synchronized void interruptResponse(String status) {
        ResponseTurn previous = currentTurn;
        currentTurn = null;
        speakingNow = false;
        spokenText = "";
        if (previous != null) previous.cancel();
        if (tts != null) { try { tts.stop(); } catch (Exception ignored) { } }
        wakeArmed = true;
        lastReplyAt = SystemClock.elapsedRealtime();
        bargePendingUntil = lastReplyAt + 15000L;
        event("interrupted", status);
        event("status", status);
    }
    /** L'ancien tour annulé ne peut plus ni parler ni remplacer la nouvelle réponse. */
    private void handlePhrase(String heard) {
        if (!running || heard == null || heard.trim().isEmpty()) return;
        long now = SystemClock.elapsedRealtime();
        if (BargeInPolicy.isStopListening(heard)) {
            event("status", "Arrêt complet de l'écoute demandé.");
            shutdown(); return;
        }
        if (BargeInPolicy.isStopAnswer(heard)) {
            interruptResponse("Réponse stoppée. Je reste à l'écoute.");
            return;
        }
        boolean activated = RustWake.detect(heard);
        boolean activeResponse = currentTurn != null;
        if (activeResponse) {
            if (!"1".equals(settings.get("barge_in_enabled", "1")) ||
                !BargeInPolicy.canBarge(heard, spokenText)) return;
            interruptResponse("Nouvelle question détectée, ancienne réponse arrêtée.");
        }
        String question;
        if (activated) {
            wakeArmed = true;
            question = VoiceText.withoutWakeWord(heard);
            if (question.isEmpty()) {
                lastReplyAt = now;
                event("status", "Friday à l'écoute : posez votre question.");
                return;
            }
        } else if (wakeArmed && now - lastReplyAt <= followupMs &&
                now - lastSpeechEndedAt >= 1200L) {
            // Court filtrage pour éviter de réinterpréter la fin du haut-parleur.
            question = heard;
        } else {
            // N'acceptez pas les mots provenant du haut-parleur sans mot-clé.
            return;
        }
        if (BargeInPolicy.wantsToAddDetails(question)) {
            lastReplyAt = now; bargePendingUntil = now + 20000L;
            event("status", "Je vous écoute. Donnez votre précision ou posez une nouvelle question.");
            return;
        }
        bargePendingUntil = 0;
        final String request = question;
        ResponseTurn turn = new ResponseTurn();
        currentTurn = turn;
        Thread task = new Thread(() -> answerTurn(request, turn), "friday-mistral-response");
        turn.thread = task;
        task.start();
    }
    private void answerTurn(String question, ResponseTurn turn) {
        if (!live(turn)) return;
        boolean memoryAction = MemoryIntent.kind(question) != MemoryIntent.NONE;
        try {
            if (!memoryAction && live(turn)) event("user", question);
            if (live(turn)) event("status", memoryAction ? "Commande mémoire locale…" : "Réflexion Mistral… Vous pouvez m'interrompre.");
            String local = PersonalMemory.command(settings, question);
            if (local == null) local = PythonBrain.localAnswer(question);
            if (local == null) local = deviceAnswer(question);
            String locationForApi = null;
            if (local == null && NearbyIntent.wantsPlaces(question)) {
                if (!"1".equals(settings.get("location_enabled", "0")) || !FridayLocation.hasPermission(this)) {
                    local = "Activez le GPS Android et autorisez la localisation pour analyser les alentours.";
                } else {
                    if (live(turn)) event("location", "Mesure GPS ponctuelle…");
                    FridayLocation.Fix fix = FridayLocation.locate(this);
                    if (!live(turn)) return;
                    if (fix == null) local = "Je n'ai pas obtenu de position GPS récente. Réessayez à l'extérieur.";
                    else if (!"1".equals(settings.get("location_share", "0")))
                        local = fix.forUser() + " Autorisez le partage GPS avec Mistral pour la recherche Web.";
                    else if (!WebPolicy.enabled(settings.get("web_mode", "off")))
                        local = "Activez la recherche Web de Mistral pour analyser les alentours.";
                    else {
                        String place = "1".equals(settings.get("address_lookup", "0")) ? AndroidPlaceResolver.resolve(this, fix) : "";
                        locationForApi = fix.forAi() + (place.isEmpty() ? "" : "\nLieu estimé par Android : " + place) +
                            "\nRECHERCHE_ALENTOURS_WEB : Vérifie les sources Web récentes pour " +
                            NearbyIntent.categoryForSearch(question) + ", rayon indicatif 800 m. " +
                            "Ne fabrique pas les commerces, distances, horaires ni adresses. " +
                            "GPS avec incertitude +/-" + Math.round(fix.accuracy) + " m.";
                    }
                }
            }
            if (local == null && locationForApi == null && LocationIntent.nearby(question)) {
                if (!"1".equals(settings.get("location_enabled", "0")) || !FridayLocation.hasPermission(this))
                    local = "Activez la localisation Android dans les réglages pour obtenir votre position.";
                else {
                    FridayLocation.Fix fix = FridayLocation.locate(this);
                    if (!live(turn)) return;
                    if (fix == null) local = "Position GPS indisponible. Vérifiez le GPS et réessayez.";
                    else if (LocationIntent.direct(question)) {
                        String place = "1".equals(settings.get("address_lookup", "0")) ? AndroidPlaceResolver.resolve(this, fix) : "";
                        boolean web = WebPolicy.enabled(settings.get("web_mode", "off"));
                        if (place.isEmpty() && "1".equals(settings.get("location_share", "0")) && web)
                            locationForApi = fix.forAi() + "\nRECHERCHE_ALENTOURS_WEB : Trouve le nom de la commune avec une source. N'invente pas la rue.";
                        else local = AndroidPlaceResolver.userText(fix, place);
                    } else if ("1".equals(settings.get("location_share", "0"))) {
                        String place = "1".equals(settings.get("address_lookup", "0")) ? AndroidPlaceResolver.resolve(this, fix) : "";
                        locationForApi = fix.forAi() + (place.isEmpty() ? "" : "\nAdresse approximative Android : " + place);
                    } else local = "La position GPS est disponible, mais le partage avec Mistral est désactivé.";
                }
            }
            if (!live(turn)) return;
            String answer = local != null ? local : NativeAI.answer(settings, PythonBrain.prepareQuestion(question),
                historySnapshot(), locationForApi, turn.api);
            if (!live(turn)) return;
            boolean privateLocation = LocationIntent.nearby(question) || NearbyIntent.wantsPlaces(question);
            if (!privateLocation && !memoryAction) {
                addHistory("user", question); addHistory("assistant", answer);
            }
            if (memoryAction) event("status", "Commande mémoire effectuée localement.");
            else event("assistant", answer);
            spokenText = answer;
            speakingNow = true;
            event("status", "Réponse vocale en cours. Dites « Friday stop » ou posez une autre question.");
            boolean spoken = speak(answer, turn);
            if (!live(turn)) return;
            lastReplyAt = SystemClock.elapsedRealtime();
            if (spoken) event("status", "Réponse terminée. Je reste à l'écoute pour une précision.");
        } catch (Exception ex) {
            if (live(turn)) {
                event("error", "Réponse indisponible : " + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
            }
        } finally {
            if (currentTurn == turn) {
                currentTurn = null;
                boolean hadSpoken = speakingNow;
                speakingNow = false;
                spokenText = "";
                lastReplyAt = SystemClock.elapsedRealtime();
                if (hadSpoken) lastSpeechEndedAt = lastReplyAt;
            }
        }
    }
    /** Diagnostics courants lus localement, sans appel à une API distante. */
    private String deviceAnswer(String question) {
        String q = VoiceText.normalize(question);
        if (q.contains("batterie") || q.contains("niveau de charge")) {
            Intent bat = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (bat == null) return "Le niveau de batterie n'est pas disponible.";
            int level = bat.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = bat.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            int state = bat.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            if (level < 0 || scale <= 0) return "Le niveau de batterie n'est pas disponible.";
            int pct = Math.round(100f * level / scale);
            return "La batterie de votre téléphone est à " + pct + " pour cent" +
                    (state == BatteryManager.BATTERY_STATUS_CHARGING || state == BatteryManager.BATTERY_STATUS_FULL
                        ? ", et le téléphone est en charge." : ".");
        }
        return null;
    }
    private List<JSONObject> historySnapshot() {
        synchronized (history) { return new ArrayList<>(history); }
    }
    private void addHistory(String role, String content) {
        try {
            synchronized (history) {
                history.addLast(new JSONObject().put("role", role).put("content", content));
                while (history.size() > 10) history.removeFirst();
            }
        } catch (Exception ignored) { }
    }
    private void initTts() {
        // TextToSpeech est asynchrone : attendre le succès réel, pas seulement l'objet Java.
        CountDownLatch ready = new CountDownLatch(1);
        AtomicInteger initCode = new AtomicInteger(TextToSpeech.ERROR);
        try {
            tts = new TextToSpeech(getApplicationContext(), code -> {
                initCode.set(code);
                ready.countDown();
            });
            if (!ready.await(15, TimeUnit.SECONDS)) {
                ttsDiagnostic = "Initialisation de la voix expirée (15 s). Vérifiez le moteur TTS Android.";
                event("error", ttsDiagnostic);
                return;
            }
            if (!running) { if (tts != null) tts.shutdown(); tts = null; return; }
            if (initCode.get() != TextToSpeech.SUCCESS) {
                ttsDiagnostic = "Moteur de synthèse vocale Android non initialisé (" + initCode.get() + ").";
                event("error", ttsDiagnostic);
                return;
            }
            int language = tts.setLanguage(Locale.FRENCH);
            if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsDiagnostic = "Voix française manquante : installez la langue française dans le moteur TTS du téléphone.";
                event("error", ttsDiagnostic);
                return;
            }
            tts.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA).build());
            if (!running) { tts.shutdown(); tts = null; return; }
            tts.setSpeechRate(settingInt("tts_speed", 100, 70, 150) / 100f);
            tts.setPitch(1.0f);
            ttsReady = true;
            ttsDiagnostic = "Voix française prête (" + tts.getDefaultEngine() + ").";
            event("tts", ttsDiagnostic);
        } catch (Exception e) {
            ttsReady = false;
            ttsDiagnostic = "Erreur TTS : " + e.getClass().getSimpleName() + " : " + e.getMessage();
            event("error", ttsDiagnostic);
            Log.e("FRIDAY_TTS", "Initialisation TTS", e);
        }
    }

    /** Retourne vrai uniquement si toutes les parties ont été prononcées avec succès. */
    private boolean speak(String text, ResponseTurn turn) {
        if (!live(turn)) return false;
        if (!"1".equals(settings.get("voice_enabled", "1"))) {
            event("error", "Réponse reçue en texte, mais « Répondre à voix haute » est désactivé.");
            return false;
        }
        if (MistralVoice.enabled(this, settings)) {
            try {
                event("tts", "Génération de la voix clonée Mistral Voxtral…");
                MistralVoice.speak(getApplicationContext(), settings, text, () -> live(turn), turn.audio);
                return live(turn);
            } catch (Exception ex) {
                if (!live(turn)) return false; // Interrompu : ne PAS relancer une autre voix.
                event("error", "Voix clonée indisponible : " + ex.getMessage() + " · repli Android.");
            }
        }
        if (tts == null || !ttsReady) {
            event("error", "Réponse reçue, mais TTS Android indisponible : " + ttsDiagnostic);
            return false;
        }
        List<String> chunks = VoiceText.ttsChunks(text, 280);
        if (chunks.isEmpty()) {
            event("error", "Réponse vide : rien à prononcer.");
            return false;
        }
        int index = 0;
        for (String part : chunks) {
            if (!live(turn)) return false;
            CountDownLatch done = new CountDownLatch(1);
            AtomicInteger failure = new AtomicInteger(0);
            String utterance = "friday_" + System.nanoTime() + "_" + (++index);
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String id) {
                    if (utterance.equals(id)) event("tts", "Lecture de la réponse, segment " + indexSafe(id) + "…");
                }
                @Override public void onDone(String id) {
                    if (utterance.equals(id)) done.countDown();
                }
                @Override public void onError(String id) {
                    if (utterance.equals(id)) { failure.set(-1); done.countDown(); }
                }
                @Override public void onError(String id, int errorCode) {
                    if (utterance.equals(id)) { failure.set(errorCode == 0 ? -1 : errorCode); done.countDown(); }
                }
            });
            try {
                int code = tts.speak(part, TextToSpeech.QUEUE_FLUSH, new Bundle(), utterance);
                if (code != TextToSpeech.SUCCESS) {
                    event("error", "Synthèse refusée (code " + code + "). Vérifiez la sortie média et le moteur TTS.");
                    return false;
                }
                long deadline = SystemClock.elapsedRealtime() + 60000L;
                while (live(turn) && SystemClock.elapsedRealtime() < deadline &&
                       !done.await(100, TimeUnit.MILLISECONDS)) { /* arrêt vérifié toutes les 100 ms */ }
                if (!live(turn)) return false;
                if (done.getCount() > 0) {
                    event("error", "Délai de lecture TTS dépassé."); return false;
                }
                if (failure.get() != 0) {
                    event("error", "Échec moteur TTS (code " + failure.get() + ").");
                    return false;
                }
            } catch (Exception ex) {
                if (!live(turn)) return false;
                event("error", "Lecture TTS : " + ex.getMessage());
                Log.e("FRIDAY_TTS", "Lecture TTS", ex);
                return false;
            }
        }
        return true;
    }
    private static String indexSafe(String utterance) {
        int i = utterance.lastIndexOf('_');
        return i < 0 ? "?" : utterance.substring(i + 1);
    }

    private synchronized void shutdown() {
        ResponseTurn previous = currentTurn;
        currentTurn = null;
        if (previous != null) previous.cancel();
        speakingNow = false;
        spokenText = "";
        running = false;
        active = false;
        micSilenced = false;
        textQuestions.clear();
        queuedQuestions.set(0);
        if (recorder != null) {
            try { recorder.stop(); } catch (Exception ignored) {}
        }
        ttsReady = false;
        if (tts != null) { try { tts.stop(); tts.shutdown(); } catch (Exception ignored) {} tts = null; }
        if (wakeLock != null && wakeLock.isHeld()) { wakeLock.release(); }
        event("status", "Écoute native désactivée.");
    }
    @Override public void onDestroy() { shutdown(); super.onDestroy(); }
    @Override public android.os.IBinder onBind(Intent intent) { return null; }
}
