package fr.friday.assistant;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;
import java.io.File;
import java.io.IOException;
import android.provider.MediaStore;
import java.io.ByteArrayOutputStream;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import android.media.AudioAttributes;
import android.speech.tts.TextToSpeech;
import java.util.concurrent.atomic.AtomicBoolean;
import android.speech.tts.UtteranceProgressListener;
import java.util.Locale;

/** Cockpit holographique avec service vocal natif v2.0, sans serveur Termux. */
public class MainActivity extends Activity {
    private SecurePrefs prefs;
    private TextView status;
    private EditText mistralKey, mistralModel, visionModel;
    private CheckBox voiceEnabled, locationEnabled, locationShare, placeNameEnabled;
    private CheckBox memoryEnabled, deepReasoning, clonedVoiceEnabled, voiceConsent;
    private TextView voiceSampleStatus;
    private volatile boolean voiceRecording = false;
    private volatile boolean voicePreview = false;
    private final AtomicBoolean hudCloneSpeaking = new AtomicBoolean(false);
    private volatile MistralVoice.Playback hudPlayback;
    private boolean recordAfterMicPermission = false;
    private EditText memoryEditor;
    private Spinner webMode;
    private EditText silenceInput, followupInput, ttsInput;
    private CheckBox bargeEnabled;
    private LinearLayout config;
    private ScrollView configScroll;
    private WebView hud;
    private TextToSpeech testTts;
    private EditText manualQuestion;
    private boolean pageReady = false;
    private boolean promptStartAfterPermission = false;
    private BroadcastReceiver events;
    private static final int REQ_MIC = 17;
    private static final int REQ_LOCATION = 19;
    private static final int REQ_PHOTO = 82;
    private static final int REQ_VOICE_SAMPLE = 83;
    private volatile String visionState = "";
    private volatile String cachedLocation = "";
    private volatile byte[] pendingJpeg;
    private File captureFile;
    private volatile boolean visionRequestInFlight = false;
    private volatile String pendingVisionQuestion = "";
    private TextToSpeech webVoice;
    private volatile boolean webVoiceReady = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(3, 16, 26));
        getWindow().setNavigationBarColor(Color.rgb(3, 16, 26));
        prefs = new SecurePrefs(this);
        // Migration : supprimer l'ancienne clé Agnès stockée sur les installations précédentes.
        try { prefs.setSecret("agnes", ""); prefs.put("mode", "mistral"); prefs.put("agnes_model", ""); } catch (Exception ignored) {}
        // Supprime une éventuelle ancienne clé Google Places, désormais inutilisée.
        try { prefs.setSecret("places", ""); } catch (Exception ignored) {}
        buildUi();
        listenEvents();
    }
    private int dp(float x) { return (int) (x * getResources().getDisplayMetrics().density + .5f); }
    private TextView label(String s) {
        TextView t = new TextView(this); t.setText(s); t.setTextColor(Color.rgb(184,225,240));
        t.setTextSize(13); t.setPadding(dp(6),dp(7),dp(6),dp(3)); return t;
    }
    private EditText field(String text, boolean secret) {
        EditText e = new EditText(this);
        e.setTextSize(14); e.setTextColor(Color.WHITE); e.setHintTextColor(Color.LTGRAY);
        e.setSingleLine(true); e.setHint(text);
        if (secret) e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(47,190,223)));
        e.setPadding(dp(6),dp(6),dp(6),dp(9)); return e;
    }
    private Button button(String text, Runnable action) {
        Button b = new Button(this); b.setAllCaps(false); b.setText(text);
        b.setTextColor(Color.WHITE); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(6,110,148)));
        b.setOnClickListener(v -> action.run()); return b;
    }
    private void buildUi() {
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setBackgroundColor(Color.rgb(4,15,27));
        setContentView(outer);
        LinearLayout bar = new LinearLayout(this);
        bar.setPadding(dp(7),dp(3),dp(7),dp(3));
        bar.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = label("F.R.I.D.A.Y. · 2.2.0");
        title.setTextSize(15); title.setTextColor(Color.rgb(71,228,249));
        bar.addView(title, new LinearLayout.LayoutParams(0,dp(48),1));
        Button configure = button("⚙ Réglages", () -> configScroll.setVisibility(configScroll.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
        bar.addView(configure);
        bar.addView(button("◈ Versions", () -> FridayVersionGuard.showManager(this)));
        bar.addView(button("✧ Profil", () -> {
            if (hud != null && pageReady) {
                configScroll.setVisibility(View.GONE);
                hud.evaluateJavascript("window.fridayOpenPersonality&&window.fridayOpenPersonality();", null);
            } else { configScroll.setVisibility(View.VISIBLE); }
        }));
        outer.addView(bar);
        LinearLayout commands = new LinearLayout(this);
        commands.setPadding(dp(7),0,dp(7),0);
        commands.addView(button("◉ Écoute ON / OFF écran", this::startListening), new LinearLayout.LayoutParams(0,dp(52),2));
        commands.addView(button("■ Stop", this::stopListening), new LinearLayout.LayoutParams(0,dp(52),1));
        outer.addView(commands);
        outer.addView(button("⏸ Interrompre la réponse et écouter", this::interruptSpeaking));
        LinearLayout checks = new LinearLayout(this);
        checks.setPadding(dp(7),0,dp(7),0);
        checks.addView(button("🔊 Tester la voix", this::testVoice), new LinearLayout.LayoutParams(0,dp(48),1));
        checks.addView(button("✓ Tester réponse locale", this::testLocalAnswer), new LinearLayout.LayoutParams(0,dp(48),1));
        outer.addView(checks);
        status = label(FridayService.isActive() ?
            "Écoute native active · écran allumé ou verrouillé." :
            "Écoute arrêtée. Téléchargez le modèle français et autorisez le micro.");
        status.setTextSize(12); status.setPadding(dp(12),dp(5),dp(12),dp(10));
        outer.addView(status);

        configScroll = new ScrollView(this);
        config = new LinearLayout(this);
        config.setPadding(dp(12),dp(6),dp(12),dp(12));
        config.setOrientation(LinearLayout.VERTICAL);
        config.setBackgroundColor(Color.rgb(8,31,47));
        configScroll.addView(config);
        outer.addView(configScroll, new LinearLayout.LayoutParams(-1, dp(340)));
        config.addView(label("MOTEUR VOCAL LOCAL · français Vosk (~41 Mo à télécharger une fois)"));
        config.addView(button(ModelInstaller.installed(this) ? "✓ Modèle installé" : "↓ Télécharger le modèle français", this::installModel));
        config.addView(label("INTELLIGENCE ARTIFICIELLE UNIQUE · MISTRAL"));
        config.addView(label("Clé Mistral (laissez vide pour conserver l'ancienne)"));
        mistralKey = field(prefs.secret("mistral").isEmpty() ? "Clé Mistral manquante" : "Clé Mistral déjà enregistrée", true);
        config.addView(mistralKey);
        config.addView(label("Modèle Mistral"));
        mistralModel = field("ministral-8b-latest", false);
        mistralModel.setText(prefs.get("mistral_model", "ministral-8b-latest")); config.addView(mistralModel);
        config.addView(label("MODÈLE D'ANALYSE VISUELLE MISTRAL · nécessite la capacité vision"));
        visionModel = field("ministral-14b-2512", false);
        visionModel.setText(prefs.get("vision_model", "ministral-14b-2512"));
        config.addView(visionModel);
        config.addView(label("INTERNET · recherche Web officielle de Mistral (selon forfait, quotas et modèle)"));
        webMode = new Spinner(this);
        String[] webChoices = {"Web étendu · accessible à chaque question", "Web automatique · si utile", "Sans recherche Web"};
        ArrayAdapter<String> webAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, webChoices);
        webAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        webMode.setAdapter(webAdapter);
        String webCurrent = prefs.get("web_mode", "1".equals(prefs.get("web", "1")) ? "always" : "off");
        webMode.setSelection("off".equals(webCurrent) ? 2 : "auto".equals(webCurrent) ? 1 : 0);
        config.addView(webMode);
        config.addView(label("La recherche Web Mistral consulte des sources publiques ; elle n’accède pas aux sites privés sans autorisation."));
        config.addView(label("INTELLIGENCE · analyse approfondie"));
        deepReasoning = new CheckBox(this);
        deepReasoning.setText("Raisonnement approfondi (réponses plus détaillées ; potentiellement plus lentes et coûteuses)");
        deepReasoning.setTextColor(Color.WHITE);
        deepReasoning.setChecked("1".equals(prefs.get("deep_reasoning", "0")));
        config.addView(deepReasoning);
        config.addView(button("Choisir Mistral Medium (modèle plus puissant)", () -> {
            mistralModel.setText("mistral-medium-latest");
            Toast.makeText(this, "Enregistrez les réglages pour appliquer le modèle", Toast.LENGTH_LONG).show();
        }));
        config.addView(label("MÉMOIRE PERSONNELLE · uniquement sur demande explicite"));
        memoryEnabled = new CheckBox(this);
        memoryEnabled.setText("Activer ma mémoire chiffrée (les notes seront transmises à Mistral avec les questions)");
        memoryEnabled.setTextColor(Color.WHITE);
        memoryEnabled.setChecked("1".equals(prefs.get("memory_enabled", "0")));
        config.addView(memoryEnabled);
        config.addView(label("Une note par ligne · 12 notes max · 300 caractères chacune. Ne stockez pas de mots de passe ou de secrets."));
        memoryEditor = new EditText(this);
        memoryEditor.setSingleLine(false);
        memoryEditor.setMinLines(4);
        memoryEditor.setMaxLines(8);
        memoryEditor.setGravity(Gravity.TOP | Gravity.START);
        memoryEditor.setTextColor(Color.WHITE);
        memoryEditor.setTextSize(14);
        memoryEditor.setText(PersonalMemory.editorText(prefs));
        memoryEditor.setHint("Exemple : Préfère des réponses techniques en français");
        config.addView(memoryEditor);
        config.addView(button("Enregistrer les notes de mémoire", this::saveMemory));
        config.addView(button("Effacer toutes les notes mémorisées", () -> {
            new AlertDialog.Builder(this).setTitle("Effacer la mémoire personnelle ?")
                .setMessage("Cela supprime définitivement les notes personnelles enregistrées localement.")
                .setNegativeButton("Annuler", (d, w) -> {})
                .setPositiveButton("Effacer", (d, w) -> {
                    try { PersonalMemory.replaceFromEditor(prefs, ""); memoryEditor.setText("");
                        Toast.makeText(this, "Mémoire effacée", Toast.LENGTH_SHORT).show(); }
                    catch (Exception e) { status.setText("Erreur mémoire : " + e.getMessage()); }
                }).show();
        }));
        config.addView(label("Commandes vocales : « Friday, retiens que... », « Friday, qu'as-tu mémorisé ? »."));
        config.addView(label("DÉTECTION DES PHRASES · délai de silence (millisecondes, 900 à 6000)"));
        silenceInput = field("2100", false);
        silenceInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        silenceInput.setText(prefs.get("silence_ms", "2100")); config.addView(silenceInput);
        config.addView(label("CONVERSATION SANS RÉPÉTER FRIDAY · fenêtre (secondes, 15 à 180)"));
        followupInput = field("75", false);
        followupInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        followupInput.setText(prefs.get("followup_sec", "75")); config.addView(followupInput);
        bargeEnabled = new CheckBox(this);
        bargeEnabled.setText("Permettre d'interrompre la voix en parlant (« Friday stop »), micro actif pendant la réponse");
        bargeEnabled.setTextColor(Color.WHITE);
        bargeEnabled.setChecked("1".equals(prefs.get("barge_in_enabled", "1")));
        config.addView(bargeEnabled);
        config.addView(label("Réduction de l'écho Android si compatible. Au haut-parleur, un casque est préférable si Friday entend sa propre voix."));
        config.addView(label("VITESSE VOCALE · 100 = normale (70 à 150)"));
        ttsInput = field("100", false);
        ttsInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        ttsInput.setText(prefs.get("tts_speed", "100")); config.addView(ttsInput);
        voiceEnabled = new CheckBox(this);
        voiceEnabled.setText("Répondre à voix haute"); voiceEnabled.setTextColor(Color.WHITE);
        voiceEnabled.setChecked("1".equals(prefs.get("voice_enabled", "1")));
        config.addView(voiceEnabled);
        config.addView(label("VOIX CLONÉE · API Mistral Voxtral TTS (facultatif, peut être facturé)"));
        config.addView(label("Enregistrez votre propre voix pendant 12 secondes dans une pièce calme. " +
            "Le fichier reste dans l'espace privé du téléphone ; il sera envoyé à Mistral lors de chaque synthèse activée. " +
            "Aucun clonage sans autorisation de la personne enregistrée."));
        voiceConsent = new CheckBox(this);
        voiceConsent.setText("Je confirme que cette voix est la mienne ou que j'ai l'accord explicite de la personne.");
        voiceConsent.setTextColor(Color.WHITE);
        voiceConsent.setChecked("1".equals(prefs.get("clone_consent", "0")));
        config.addView(voiceConsent);
        clonedVoiceEnabled = new CheckBox(this);
        clonedVoiceEnabled.setText("Utiliser la voix clonée pour les réponses (Internet et clé Mistral requis)");
        clonedVoiceEnabled.setTextColor(Color.WHITE);
        clonedVoiceEnabled.setChecked("1".equals(prefs.get("clone_enabled", "0")) && VoiceSampleRecorder.exists(this));
        config.addView(clonedVoiceEnabled);
        voiceSampleStatus = label("");
        config.addView(voiceSampleStatus);
        refreshVoiceSampleStatus();
        config.addView(button("● Enregistrer mon échantillon vocal (12 s)", this::recordMyVoice));
        config.addView(button("✓ Activer mon échantillon déjà enregistré", this::activateRecordedVoice));
        config.addView(button("▶ Tester ma voix clonée", this::previewClonedVoice));
        config.addView(button("Supprimer définitivement mon échantillon", this::deleteVoiceSample));
        config.addView(label("La lecture Android standard sert de repli si Mistral TTS est indisponible. " +
            "L'échantillon n'est pas enregistré comme voix permanente dans l'espace Mistral. " +
            "La synthèse personnalisée peut ajouter un délai et un coût par réponse."));
        config.addView(label("LOCALISATION · permission Android facultative"));
        locationEnabled = new CheckBox(this);
        locationEnabled.setText("Activer la position uniquement sur demande vocale");
        locationEnabled.setTextColor(Color.WHITE);
        locationEnabled.setChecked("1".equals(prefs.get("location_enabled", "0")));
        config.addView(locationEnabled);
        locationShare = new CheckBox(this);
        locationShare.setText("Autoriser l'envoi ponctuel de mes coordonnées GPS à Mistral pour les questions locales (précision élevée)");
        locationShare.setTextColor(Color.WHITE);
        locationShare.setChecked("1".equals(prefs.get("location_share", "0")));
        config.addView(locationShare);
        placeNameEnabled = new CheckBox(this);
        placeNameEnabled.setText("Identifier la commune ou la rue à la demande (géocodeur du téléphone, peut utiliser son réseau)");
        placeNameEnabled.setTextColor(Color.WHITE);
        placeNameEnabled.setChecked("1".equals(prefs.get("address_lookup", "0")));
        config.addView(placeNameEnabled);
        config.addView(label("Aucune clé Google : ce géocodeur est une fonction d'Android, dont le fournisseur peut accéder à Internet. Désactivé par défaut."));
        config.addView(label("Aucun suivi GPS permanent : mesure à la demande (jusqu'à 16 s) et objectif 3 m non garanti. " +
            "La précision effectivement mesurée est affichée. Si le partage IA est autorisé, les coordonnées précises de cette question sont transmises à l’API Mistral."));
        config.addView(button("◎ Tester précision GPS · cible 3 m", this::showGpsAccuracy));
        config.addView(label("ANALYSE DES ALENTOURS · sans API Google : recherche Web de Mistral uniquement. " +
            "Ne garantit ni liste exhaustive ni distances exactes ; exigez des sources pour les lieux cités."));
        config.addView(button("◎ Où suis-je : ville et rue ?", () -> sendQuestion("Friday, où suis-je exactement ?")));
        config.addView(button("◈ Analyser les lieux autour de moi", () -> sendQuestion("Quels lieux et services se trouvent autour de moi ?")));
        config.addView(button("◈ Pharmacies proches", () -> sendQuestion("Cherche les pharmacies autour de moi")));
        config.addView(button("◈ Restaurants proches", () -> sendQuestion("Cherche les restaurants autour de moi")));
        config.addView(button("Enregistrer les réglages", this::save));
        config.addView(label("PERSONNALITÉ · prompt sur plusieurs lignes · profils chiffrés"));
        config.addView(button("✧ Modifier la personnalité de F.R.I.D.A.Y.", () -> {
            configScroll.setVisibility(View.GONE);
            if (pageReady) hud.evaluateJavascript("window.fridayOpenPersonality&&window.fridayOpenPersonality();", null);
        }));
        config.addView(label("DIAGNOSTIC : écrire une question, puis l'envoyer au moteur natif. Cela vérifie les API sans microphone."));
        manualQuestion = field("Ex. Bonjour, peux-tu me répondre ?", false);
        config.addView(manualQuestion);
        config.addView(button("Envoyer une question sans microphone", () -> sendQuestion(manualQuestion.getText().toString())));
        config.addView(button("Effacer les échanges vocaux locaux", this::clearVoiceHistory));
        config.addView(button("Réglages batterie Android", () -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }));
        config.addView(label("L'écoute native continue avec écran allumé ou verrouillé après un démarrage volontaire depuis l'application visible. Un redémarrage du service depuis l'application est nécessaire après tout changement de permission de localisation. La notification permet d'arrêter le micro."));
        config.addView(label("La reconnaissance vocale est locale ; seule la question transcrite est envoyée à l’API Mistral configurée. Les clés API natives sont chiffrées Android Keystore."));

        hud = new WebView(this);
        hud.setBackgroundColor(Color.rgb(5,17,28));
        WebSettings settings = hud.getSettings();
        settings.setJavaScriptEnabled(true); settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        hud.addJavascriptInterface(new NativeBridge(), "FridayAndroid");
        hud.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                if ("file".equals(u.getScheme()) && u.getPath() != null && u.getPath().startsWith("/android_asset/")) return false;
                if ("https".equals(u.getScheme()) || "http".equals(u.getScheme())) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) { }
                }
                return true;
            }
            @Override public void onPageFinished(WebView view, String url) {
                pageReady = true;
                view.evaluateJavascript("window.fridayNativeReady&&window.fridayNativeReady();", null);
                replayVoiceHistory();
            }
        });
        outer.addView(hud, new LinearLayout.LayoutParams(-1,0,1));
        hud.loadUrl("file:///android_asset/index.html");
        configScroll.setVisibility(View.GONE);
    }
    private void save() {
        try {
            String a = mistralKey.getText().toString().trim();
            if (!a.isEmpty()) prefs.setSecret("mistral", a);
            prefs.put("mode", "mistral");
            prefs.put("mistral_model", mistralModel.getText().toString().trim());
            String selectedVisionModel = visionModel.getText().toString().trim();
            prefs.put("vision_model", selectedVisionModel.isEmpty() ? "ministral-14b-2512" : selectedVisionModel);
            String selectedWebMode = webMode.getSelectedItemPosition() == 2 ? "off" :
                webMode.getSelectedItemPosition() == 1 ? "auto" : "always";
            prefs.put("web_mode", selectedWebMode);
            prefs.put("deep_reasoning", deepReasoning.isChecked() ? "1" : "0");
            prefs.put("memory_enabled", memoryEnabled.isChecked() ? "1" : "0");
            prefs.put("web", WebPolicy.enabled(selectedWebMode) ? "1" : "0");
            prefs.put("silence_ms", bounded(silenceInput, 2100, 900, 6000));
            prefs.put("followup_sec", bounded(followupInput, 75, 15, 180));
            prefs.put("barge_in_enabled", bargeEnabled.isChecked() ? "1" : "0");
            prefs.put("tts_speed", bounded(ttsInput, 100, 70, 150));
            if (clonedVoiceEnabled.isChecked() && !VoiceClonePolicy.hasConsent(voiceConsent.isChecked(), VoiceSampleRecorder.exists(this))) {
                clonedVoiceEnabled.setChecked(false);
                prefs.put("clone_enabled", "0");
                status.setText("Voix clonée non activée : cochez le consentement et enregistrez votre voix.");
                return;
            }
            if (clonedVoiceEnabled.isChecked()) {
                // Une voix clonée ne sert à rien si les réponses vocales générales sont coupées.
                voiceEnabled.setChecked(true);
                VoiceCloneActivation.enable(prefs::put, voiceConsent.isChecked(), VoiceSampleRecorder.exists(this));
            } else {
                prefs.put("clone_consent", voiceConsent.isChecked() ? "1" : "0");
                prefs.put("clone_enabled", "0");
                prefs.put("voice_enabled", voiceEnabled.isChecked() ? "1" : "0");
            }
            refreshVoiceSampleStatus();
            refreshVoiceModeOnHud();
            prefs.put("location_enabled", locationEnabled.isChecked() ? "1" : "0");
            prefs.put("location_share", locationEnabled.isChecked() && locationShare.isChecked() ? "1" : "0");
            prefs.put("address_lookup", locationEnabled.isChecked() && placeNameEnabled.isChecked() ? "1" : "0");
            mistralKey.setText("");
            status.setText(clonedVoiceEnabled.isChecked() ?
                "Réglages enregistrés : VOIX CLONÉE ACTIVE. Testez-la puis redémarrez l'écoute." :
                "Réglages enregistrés. Arrêtez puis redémarrez le service pour appliquer les permissions.");
            if (locationEnabled.isChecked() && !FridayLocation.hasPermission(this)) requestLocationAccess();
            Toast.makeText(this,"Réglages natifs enregistrés",Toast.LENGTH_SHORT).show();
        } catch (Exception ex) { status.setText("Enregistrement impossible : " + ex.getMessage()); }
    }
    private void recordMyVoice() {
        if (voiceRecording) { status.setText("Enregistrement déjà en cours."); return; }
        if (!voiceConsent.isChecked()) {
            new AlertDialog.Builder(this).setTitle("Autorisation nécessaire")
                .setMessage("Enregistrez uniquement votre voix ou une voix avec l'accord explicite de la personne.")
                .setPositiveButton("Compris", null).show();
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            recordAfterMicPermission = true;
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_VOICE_SAMPLE);
            return;
        }
        // Ne jamais enregistrer la référence avec Vosk actif dans la même application.
        if (FridayService.isActive()) stopListening();
        voiceRecording = true;
        status.setText("Enregistrement vocal : 12 secondes. Parlez naturellement, sans musique.");
        new Thread(() -> {
            try {
                Thread.sleep(850);
                if (FridayService.isActive()) throw new IOException("Arrêtez d'abord l'écoute F.R.I.D.A.Y.");
                VoiceSampleRecorder.capture(getApplicationContext(), VoiceClonePolicy.SECONDS);
                // L'activation devient effective dès la fin de la capture, sans bouton Enregistrer.
                runOnUiThread(this::activateRecordedVoice);
            } catch (Exception e) {
                runOnUiThread(() -> status.setText("Échantillon vocal non enregistré : " + e.getMessage()));
            } finally { voiceRecording = false; }
        }, "friday-voice-capture").start();
    }
    /** Permet aussi de réactiver une référence enregistrée par la v2.1.8 sans la refaire. */
    private void activateRecordedVoice() {
        try {
            VoiceCloneActivation.enable(prefs::put,
                voiceConsent.isChecked(), VoiceSampleRecorder.exists(this));
            clonedVoiceEnabled.setChecked(true);
            voiceEnabled.setChecked(true);
            refreshVoiceSampleStatus();
            refreshVoiceModeOnHud();
            status.setText("Voix clonée ACTIVÉE et enregistrée. " +
                "Appuyez sur « Tester ma voix clonée » avec l'écoute arrêtée.");
        } catch (Exception ex) {
            refreshVoiceSampleStatus();
            status.setText("Activation impossible : " + ex.getMessage());
        }
    }
    private void deleteVoiceSample() {
        new AlertDialog.Builder(this).setTitle("Supprimer ma référence vocale ?")
            .setMessage("L'échantillon enregistré dans le téléphone sera supprimé. Aucune voix n'a été créée de manière permanente dans Mistral.")
            .setNegativeButton("Annuler", null)
            .setPositiveButton("Supprimer", (dialog, which) -> {
                if (voiceRecording || voicePreview) {
                    status.setText("Attendez la fin de l'enregistrement ou de la lecture avant de supprimer."); return;
                }
                prefs.put("clone_enabled", "0");
                prefs.put("clone_consent", "0");
                clonedVoiceEnabled.setChecked(false);
                voiceConsent.setChecked(false);
                boolean erased = VoiceSampleRecorder.erase(this);
                refreshVoiceSampleStatus();
                refreshVoiceModeOnHud();
                status.setText(erased ? "Référence supprimée. Voix Android rétablie." : "Impossible de supprimer la référence.");
            }).show();
    }
    private void previewClonedVoice() {
        if (voicePreview || voiceRecording) return;
        if (!voiceConsent.isChecked() || !VoiceSampleRecorder.exists(this)) {
            status.setText("Enregistrez votre voix et cochez la confirmation d'autorisation."); return;
        }
        if (!MistralVoice.enabled(this, prefs)) {
            status.setText("La voix n'est pas activée. Enregistrez un échantillon ou activez la case dans les réglages."); return;
        }
        if (FridayService.isActive()) {
            status.setText("Arrêtez l'écoute F.R.I.D.A.Y. avant de tester la voix."); return;
        }
        voicePreview = true;
        status.setText("Test Mistral Voxtral : génération de la voix clonée en cours…");
        new Thread(() -> {
            try {
                MistralVoice.speak(getApplicationContext(), prefs,
                    "Bonjour, je suis Friday. Cette réponse utilise votre voix personnalisée.", () -> true);
                runOnUiThread(() -> status.setText("Test de voix clonée terminé."));
            } catch (Exception ex) {
                runOnUiThread(() -> status.setText("Test voix clonée impossible : " + ex.getMessage()));
            } finally { voicePreview = false; }
        }, "friday-voice-preview").start();
    }

    /** Affiche le statut enregistré, pas seulement l'état visuel d'une case. */
    private void refreshVoiceSampleStatus() {
        if (voiceSampleStatus == null) return;
        boolean present = VoiceSampleRecorder.exists(this);
        boolean active = MistralVoice.enabled(this, prefs);
        voiceSampleStatus.setText("Échantillon : " + (present ? "ENREGISTRÉ" : "ABSENT") +
            "  |  Voix clonée : " + (active ? "ACTIVÉE" : "DÉSACTIVÉE") +
            ". Arrêtez l'écoute des autres éditions avant une nouvelle capture.");
    }
    private void refreshVoiceModeOnHud() {
        if (pageReady && hud != null) {
            hud.evaluateJavascript("window.fridayRefreshNative&&window.fridayRefreshNative();", null);
        }
    }

    private void saveMemory() {
        try {
            int count = PersonalMemory.replaceFromEditor(prefs, memoryEditor.getText().toString());
            status.setText("Mémoire chiffrée : " + count + " notes enregistrées. Activez la case mémoire pour les utiliser.");
            Toast.makeText(this, "Notes enregistrées : " + count, Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
            status.setText("Échec de la mémoire : " + ex.getMessage());
        }
    }

    private String bounded(EditText input, int fallback, int low, int high) {
        int value;
        try { value = Integer.parseInt(input.getText().toString().trim()); }
        catch (Exception ignored) { value = fallback; }
        value = Math.max(low, Math.min(high, value));
        input.setText(String.valueOf(value));
        return String.valueOf(value);
    }
    private void testLocalAnswer() {
        sendQuestion("Quelle heure est-il ?");
    }
    private void sendQuestion(String question) {
        if (question == null || question.trim().isEmpty()) {
            status.setText("Saisissez une question pour le test."); return;
        }
        if (!FridayService.isActive()) {
            status.setText("Démarrez d'abord l'écoute native, puis relancez le test."); return;
        }
        Intent cmd = new Intent(this, FridayService.class).setAction(FridayService.ACTION_ASK_TEXT);
        cmd.putExtra("question", question.trim());
        startService(cmd);
        status.setText("Question envoyée au service natif (sans micro). Consultez la réponse et le diagnostic TTS.");
    }
    private void testVoice() {
        if (testTts != null) { testTts.shutdown(); testTts = null; }
        status.setText("Test de la voix : initialisation du moteur TTS Android…");
        testTts = new TextToSpeech(getApplicationContext(), code -> runOnUiThread(() -> {
            if (code != TextToSpeech.SUCCESS || testTts == null) {
                status.setText("Test vocal impossible : moteur TTS absent ou initialisation échouée (" + code + ").");
                return;
            }
            int lang = testTts.setLanguage(Locale.FRENCH);
            if (lang == TextToSpeech.LANG_MISSING_DATA || lang == TextToSpeech.LANG_NOT_SUPPORTED) {
                status.setText("Voix française absente : configurez Synthèse vocale dans les paramètres Android.");
                return;
            }
            testTts.setSpeechRate(1.0f);
            testTts.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
            final String id = "test_voix_friday";
            testTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String uid) {
                    runOnUiThread(() -> status.setText("La lecture audio a démarré. Vérifiez le volume média."));
                }
                @Override public void onDone(String uid) {
                    runOnUiThread(() -> status.setText("Test de synthèse terminé. Avez-vous entendu : Bonjour, je suis Friday ?"));
                }
                @Override public void onError(String uid) {
                    runOnUiThread(() -> status.setText("Échec du moteur TTS Android pendant le test."));
                }
                @Override public void onError(String uid, int e) {
                    runOnUiThread(() -> status.setText("Erreur du moteur TTS Android : " + e));
                }
            });
            int result = testTts.speak("Bonjour, je suis Friday. Le test vocal est opérationnel.",
                    TextToSpeech.QUEUE_FLUSH, new Bundle(), id);
            if (result != TextToSpeech.SUCCESS) status.setText("Erreur de démarrage du test TTS : " + result);
        }));
    }
    private void clearVoiceHistory() {
        getSharedPreferences("friday_voice_history", MODE_PRIVATE).edit().clear().apply();
        if (pageReady) hud.evaluateJavascript("window.fridayNativeReset&&window.fridayNativeReset();", null);
        status.setText("Historique vocal local effacé ; l'historique Web éventuel reste séparé.");
    }
    private void installModel() {
        if (ModelInstaller.installed(this)) { status.setText("Modèle français déjà installé."); return; }
        status.setText("Téléchargement du modèle français lancé…");
        new Thread(() -> {
            try { ModelInstaller.download(this, s -> runOnUiThread(() -> status.setText(s))); }
            catch (Exception e) { runOnUiThread(() -> status.setText("Erreur téléchargement : " + e.getMessage())); }
        }, "friday-model-download").start();
    }
    /** Vérification ponctuelle de la précision réelle du GPS, sans partage réseau. */
    private void showGpsAccuracy() {
        if (!FridayLocation.hasPermission(this)) {
            status.setText("Autorisez la position précise Android puis relancez le test GPS.");
            requestLocationAccess(); return;
        }
        status.setText("Acquisition GPS en cours (objectif 3 m, non garanti)…");
        new Thread(() -> {
            FridayLocation.Fix fix = FridayLocation.locate(getApplicationContext());
            String place = fix != null && "1".equals(prefs.get("address_lookup", "0")) ?
                AndroidPlaceResolver.resolve(getApplicationContext(), fix) : "";
            cachedLocation = fix == null ? "Position GPS indisponible" : AndroidPlaceResolver.userText(fix, place);
            runOnUiThread(() -> {
                status.setText(cachedLocation);
                if (pageReady && hud != null) hud.evaluateJavascript("window.fridayLocationSync&&window.fridayLocationSync();", null);
            });
        }, "friday-gps-test").start();
    }
    private void requestLocationAccess() {
        if (FridayLocation.hasPermission(this)) {
            status.setText("Localisation Android autorisée. Enregistrez les réglages puis redémarrez l'écoute depuis l'application.");
            return;
        }
        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOCATION);
    }
    private void startListening() {
        if (!ModelInstaller.installed(this)) {
            status.setText("Téléchargez d'abord le modèle vocal français dans Réglages.");
            configScroll.setVisibility(View.VISIBLE); return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            promptStartAfterPermission = true;
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC); return;
        }
        if (FridayService.isActive()) {
            status.setText("Le microphone natif est déjà actif, même écran verrouillé. Stoppez pour appliquer de nouveaux réglages.");
            return;
        }
        if ("1".equals(prefs.get("location_enabled", "0")) && !FridayLocation.hasPermission(this)) {
            status.setText("Localisation demandée : autorisez-la, puis redémarrez l'écoute.");
            requestLocationAccess();
            return;
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 18);
        }
        java.util.List<FridayVersionGuard.Installed> others = FridayVersionGuard.installed(this);
        if (!others.isEmpty()) {
            new AlertDialog.Builder(this)
                .setTitle("Autre édition F.R.I.D.A.Y. installée")
                .setMessage(FridayVersionPolicy.warning(others.size()) +
                    "\n\nL'activité réelle de l'autre édition ne peut pas être déterminée ici. " +
                    "Pour éviter deux écoutes simultanées, arrêtez-la avant de continuer.")
                .setPositiveButton("Démarrer quand même", (dialog, which) -> startServiceUnchecked())
                .setNeutralButton("Gérer les versions", (dialog, which) -> FridayVersionGuard.showManager(this))
                .setNegativeButton("Annuler", null)
                .show();
            return;
        }
        startServiceUnchecked();
    }
    private void startServiceUnchecked() {
        try {
            Intent i = new Intent(this, FridayService.class).setAction(FridayService.ACTION_START);
            startForegroundService(i);
            status.setText("Démarrage écoute native · " + FridayVersionGuard.thisVersion(this));
        } catch (Exception e) { status.setText("Démarrage refusé : " + e.getMessage()); }
    }
    private void interruptSpeaking() {
        hudCloneSpeaking.set(false);
        MistralVoice.Playback playback = hudPlayback;
        if (playback != null) playback.cancel();
        if (webVoice != null) { try { webVoice.stop(); } catch (Exception ignored) { } }
        startService(new Intent(this, FridayService.class).setAction(FridayService.ACTION_INTERRUPT));
        status.setText("Réponse interrompue, micro toujours actif. Posez votre question.");
    }
    private void stopListening() {
        startService(new Intent(this, FridayService.class).setAction(FridayService.ACTION_STOP));
        status.setText("Arrêt de l'écoute demandé.");
    }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(requestCode, permissions, grants);
        if (requestCode == REQ_MIC) {
            if (grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED && promptStartAfterPermission) {
                promptStartAfterPermission = false; startListening();
            } else { promptStartAfterPermission = false; status.setText("Microphone refusé dans les autorisations Android."); }
        } else if (requestCode == REQ_VOICE_SAMPLE) {
            if (grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED && recordAfterMicPermission) {
                recordAfterMicPermission = false; recordMyVoice();
            } else { recordAfterMicPermission = false; status.setText("Microphone refusé pour la référence vocale."); }
        } else if (requestCode == REQ_LOCATION) {
            if (FridayLocation.hasPermission(this)) {
                locationEnabled.setChecked(true);
                prefs.put("location_enabled", "1");
                status.setText("Position autorisée. Redémarrez l'écoute depuis l'application pour l'utiliser écran verrouillé.");
            } else {
                prefs.put("location_enabled", "0");
                locationEnabled.setChecked(false);
                status.setText("Position refusée : l'écoute vocale continue sans localisation.");
            }
        }
    }
    private void listenEvents() {
        events = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                String kind = intent.getStringExtra("kind");
                String text = intent.getStringExtra("text");
                if (kind == null || text == null) return;
                if (kind.equals("interrupted")) {
                    hudCloneSpeaking.set(false);
                    MistralVoice.Playback playback = hudPlayback;
                    if (playback != null) playback.cancel();
                    if (webVoice != null) { try { webVoice.stop(); } catch (Exception ignored) { } }
                }
                if (kind.equals("status") || kind.equals("error") || kind.equals("tts") || kind.equals("mic_conflict") || kind.equals("interrupted")) status.setText(text);
                if (pageReady) {
                    if (kind.equals("user") || kind.equals("assistant")) replayVoiceHistory();
                    else {
                        String js = "window.fridayNativeReceive&&window.fridayNativeReceive(" + JSONObjectQuote(kind) + "," + JSONObjectQuote(text) + ");";
                        hud.evaluateJavascript(js, null);
                    }
                }
            }
        };
        android.content.IntentFilter filter = new android.content.IntentFilter(FridayService.ACTION_EVENT);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(events, filter, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(events, filter);
    }
    private String JSONObjectQuote(String value) { return org.json.JSONObject.quote(value); }
    private void replayVoiceHistory() {
        if (hud == null || !pageReady) return;
        String eventsJson = getSharedPreferences("friday_voice_history", MODE_PRIVATE).getString("events", "[]");
        if (eventsJson == null) eventsJson = "[]";
        try {
            new org.json.JSONArray(eventsJson);
            hud.evaluateJavascript("window.fridayNativeInbox&&window.fridayNativeInbox(" + eventsJson + ");", null);
        } catch (Exception ignored) { }
    }
    @Override protected void onResume() {
        super.onResume();
        replayVoiceHistory();
        if (status != null) {
            if (FridayService.isMicSilenced()) status.setText("Microphone momentanément rendu muet par Android : vérifiez les autres applications.");
            else if (FridayService.isActive()) status.setText("Écoute native active · " + FridayVersionGuard.thisVersion(this));
            else status.setText(FridayVersionPolicy.warning(FridayVersionGuard.installed(this).size()));
        }
    }
    /** Photo pleine définition dans le cache privé, partagée ponctuellement avec la caméra. */
    private void capturePhoto(String question) {
        if (visionRequestInFlight || "1".equals(visionState)) {
            visionState = "4|Une capture ou analyse est déjà en cours.";
            return;
        }
        pendingVisionQuestion = question == null ? "" : question.trim();
        pendingJpeg = null;
        cleanupPhoto();
        try {
            File dir = new File(getCacheDir(), "vision");
            if (!dir.exists() && !dir.mkdirs()) throw new IOException("Impossible de créer le dossier photo temporaire.");
            captureFile = File.createTempFile("friday_capture_", ".jpg", dir);
            Uri photoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", captureFile);
            Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            camera.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            camera.setClipData(ClipData.newRawUri("photo", photoUri));
            camera.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            visionState = "1";
            startActivityForResult(camera, REQ_PHOTO);
        } catch (Exception ex) {
            cleanupPhoto();
            visionState = "4|Appareil photo indisponible : " + safeError(ex);
        }
    }

    private void cleanupPhoto() {
        File f = captureFile;
        captureFile = null;
        if (f != null && f.exists()) f.delete();
    }

    /** Évite le chargement de plusieurs dizaines de Mo depuis le capteur photo. */
    private static byte[] normalizeJpeg(File file, Bitmap fallback) throws Exception {
        Bitmap bitmap = null;
        if (file != null && file.exists() && file.length() > 0) {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new IOException("Photo illisible (JPEG absent ou corrompu).");
            int sample = 1;
            while (Math.max(bounds.outWidth / sample, bounds.outHeight / sample) > 2000) sample *= 2;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sample;
            bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            if (bitmap == null) throw new IOException("Décodage de la photo impossible.");
            int orientation = new ExifInterface(file.getAbsolutePath()).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int angle = orientation == ExifInterface.ORIENTATION_ROTATE_90 ? 90 :
                        orientation == ExifInterface.ORIENTATION_ROTATE_180 ? 180 :
                        orientation == ExifInterface.ORIENTATION_ROTATE_270 ? 270 : 0;
            if (angle != 0) {
                Matrix matrix = new Matrix(); matrix.postRotate(angle);
                Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                if (rotated != bitmap) { bitmap.recycle(); bitmap = rotated; }
            }
        } else if (fallback != null) bitmap = fallback;
        if (bitmap == null) throw new IOException("La caméra n'a retourné aucune photo exploitable.");
        try {
            // Rééchantillonnage final : pas plus de 1800 px côté long.
            int longest = Math.max(bitmap.getWidth(), bitmap.getHeight());
            if (longest > 1800) {
                float ratio = 1800f / longest;
                Bitmap scaled = Bitmap.createScaledBitmap(bitmap,
                        Math.max(1, Math.round(bitmap.getWidth() * ratio)),
                        Math.max(1, Math.round(bitmap.getHeight() * ratio)), true);
                if (scaled != bitmap) { if (bitmap != fallback) bitmap.recycle(); bitmap = scaled; }
            }
            for (int quality : new int[]{85, 75, 60, 45}) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out))
                    throw new IOException("Compression JPEG impossible.");
                byte[] bytes = out.toByteArray();
                if (bytes.length > 0 && bytes.length <= 1_800_000) return bytes;
            }
            throw new IOException("Photo encore trop volumineuse. Rapprochez-vous et réessayez.");
        } finally {
            if (bitmap != fallback && !bitmap.isRecycled()) bitmap.recycle();
        }
    }

    private static String safeError(Exception ex) {
        String message = ex.getMessage();
        return message == null || message.trim().isEmpty() ? ex.getClass().getSimpleName() : message;
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PHOTO) return;
        File file = captureFile;
        captureFile = null;
        if (resultCode != RESULT_OK) {
            if (file != null) file.delete();
            visionState = "4|Capture annulée.";
            return;
        }
        visionState = "2";
        new Thread(() -> {
            try {
                Bitmap thumbnail = null;
                if (data != null && data.getExtras() != null) {
                    Object extra = data.getExtras().get("data");
                    if (extra instanceof Bitmap) thumbnail = (Bitmap) extra;
                }
                pendingJpeg = normalizeJpeg(file, thumbnail);
                visionState = "5";
                if (!pendingVisionQuestion.isEmpty()) analyzePhoto(pendingVisionQuestion);
            } catch (Exception ex) {
                visionState = "4|Lecture de la photo : " + safeError(ex);
            } finally {
                if (file != null && file.exists()) file.delete();
            }
        }, "friday-photo-decode").start();
    }

    private void analyzePhoto(String question) {
        if (visionRequestInFlight) return;
        byte[] image = pendingJpeg;
        if (image == null || image.length == 0) {
            visionState = "4|Photo indisponible. Prenez une nouvelle photo.";
            return;
        }
        visionRequestInFlight = true;
        pendingJpeg = null;
        visionState = "2";
        new Thread(() -> {
            try { visionState = "3|" + NativeAI.visionAnswer(prefs, image, question); }
            catch (Exception ex) { visionState = "4|" + safeError(ex); }
            finally { visionRequestInFlight = false; }
        }, "friday-vision").start();
    }
    private void cancelVision() {
        // N'arrête pas une requête HTTP déjà transmise ; efface la photo en attente.
        if (!visionRequestInFlight) {
            pendingJpeg = null;
            pendingVisionQuestion = "";
            cleanupPhoto();
            visionState = "";
        }
    }
    private void playWebVoice(String text) {
        if (text == null || text.trim().isEmpty()) return;
        final String spoken = text.length()>5000 ? text.substring(0,5000) : text;
        if (MistralVoice.enabled(this, prefs)) {
            if (!hudCloneSpeaking.compareAndSet(false, true)) return; // une seule lecture à la fois
            new Thread(() -> {
                MistralVoice.Playback currentPlayback = new MistralVoice.Playback();
                hudPlayback = currentPlayback;
                try { MistralVoice.speak(getApplicationContext(), prefs, spoken, hudCloneSpeaking::get, currentPlayback); }
                catch (Exception e) { if (hudCloneSpeaking.get()) runOnUiThread(() -> {
                    status.setText("Voix clonée indisponible : " + e.getMessage() + " · voix Android utilisée.");
                    playWebVoiceAndroid(spoken);
                }); }
                finally {
                    if (hudPlayback == currentPlayback) hudPlayback = null;
                    hudCloneSpeaking.set(false);
                }
            }, "friday-hud-cloned-voice").start();
            return;
        }
        playWebVoiceAndroid(spoken);
    }
    private void playWebVoiceAndroid(String spoken) {
        if (webVoice != null && webVoiceReady) {
            webVoice.speak(spoken, TextToSpeech.QUEUE_FLUSH, new Bundle(), "friday_web_" + System.nanoTime());
            return;
        }
        if (webVoice != null) return;
        webVoice = new TextToSpeech(getApplicationContext(), result -> runOnUiThread(() -> {
            webVoiceReady = result == TextToSpeech.SUCCESS && webVoice != null &&
                webVoice.setLanguage(Locale.FRENCH) >= TextToSpeech.LANG_AVAILABLE;
            if (webVoiceReady) webVoice.speak(spoken, TextToSpeech.QUEUE_FLUSH, new Bundle(), "friday_web_" + System.nanoTime());
        }));
    }
    private void refreshLocation() {
        if (!"1".equals(prefs.get("location_enabled", "0")) || !FridayLocation.hasPermission(this)) {
            cachedLocation = "Localisation désactivée"; return;
        }
        new Thread(() -> {
            try {
                FridayLocation.Fix fix = FridayLocation.locate(getApplicationContext());
                cachedLocation = fix == null ? "Position indisponible" : fix.forUser();
            } catch (Exception e) { cachedLocation = "Position indisponible"; }
            runOnUiThread(() -> {
                if (pageReady && hud != null)
                    hud.evaluateJavascript("window.fridayLocationSync&&window.fridayLocationSync();", null);
            });
        }, "friday-position").start();
    }
    private final class NativeBridge {
        @JavascriptInterface public String getPersonalityProfiles() {
            return PersonalityProfiles.read(prefs);
        }
        @JavascriptInterface public String savePersonalityProfiles(String json) {
            try { PersonalityProfiles.save(prefs, json); return "OK"; }
            catch (Exception e) { return "Erreur : " + e.getMessage(); }
        }
        @JavascriptInterface public void ask(String question) {
            if (question == null || question.length() > 15000) return;
            runOnUiThread(() -> sendQuestion(question));
        }
        @JavascriptInterface public void sharePersonalityProfiles(String json) {
            try {
                String safe = PersonalityProfiles.validated(json).toString(2);
                runOnUiThread(() -> {
                    Intent send = new Intent(Intent.ACTION_SEND);
                    send.setType("application/json");
                    send.putExtra(Intent.EXTRA_TEXT, safe);
                    startActivity(Intent.createChooser(send, "Exporter les profils F.R.I.D.A.Y."));
                });
            } catch (Exception ignored) { }
        }
        @JavascriptInterface public void start() { runOnUiThread(MainActivity.this::startListening); }
        @JavascriptInterface public void stop() { runOnUiThread(MainActivity.this::stopListening); }
        @JavascriptInterface public void interrupt() { runOnUiThread(MainActivity.this::interruptSpeaking); }
        @JavascriptInterface public void configure() { runOnUiThread(() -> configScroll.setVisibility(View.VISIBLE)); }
        @JavascriptInterface public boolean isListening() { return FridayService.isActive(); }
        @JavascriptInterface public boolean voiceCloneActive() { return MistralVoice.enabled(MainActivity.this, prefs); }
        @JavascriptInterface public boolean isMicSilenced() { return FridayService.isMicSilenced(); }
        @JavascriptInterface public String currentVersion() { return FridayVersionGuard.thisVersion(MainActivity.this); }
        @JavascriptInterface public int otherVersionsCount() { return FridayVersionGuard.installed(MainActivity.this).size(); }
        @JavascriptInterface public void manageVersions() { runOnUiThread(() -> FridayVersionGuard.showManager(MainActivity.this)); }
        @JavascriptInterface public boolean visionAvailable() { return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY); }
        @JavascriptInterface public void vision(String question) { runOnUiThread(() -> capturePhoto(question)); }
        @JavascriptInterface public void visionAsk(String question) { runOnUiThread(() -> analyzePhoto(question)); }
        @JavascriptInterface public synchronized String visionPoll() {
            String current = visionState;
            if (current.startsWith("3|") || current.startsWith("4|")) visionState = "";
            return current;
        }
        @JavascriptInterface public void visionCancel() { runOnUiThread(MainActivity.this::cancelVision); }
        @JavascriptInterface public void gpsCheck() { runOnUiThread(MainActivity.this::showGpsAccuracy); }
        @JavascriptInterface public void nearbyAnalyze() {
            runOnUiThread(() -> sendQuestion("Quels lieux et services se trouvent autour de moi ?"));
        }
        @JavascriptInterface public String webPolicy() {
            return prefs.get("web_mode", "1".equals(prefs.get("web", "1")) ? "always" : "off");
        }
        @JavascriptInterface public String locationText() { return cachedLocation; }
        @JavascriptInterface public void locationRefresh() { runOnUiThread(MainActivity.this::refreshLocation); }
        @JavascriptInterface public void speakNative(String text) { runOnUiThread(() -> playWebVoice(text)); }
        @JavascriptInterface public void stopSpeakingNative() {
            runOnUiThread(() -> { hudCloneSpeaking.set(false);
                MistralVoice.Playback playback = hudPlayback;
                if (playback != null) playback.cancel();
                if (webVoice != null) webVoice.stop(); });
        }
        @JavascriptInterface public boolean locationEnabled() {
            return "1".equals(prefs.get("location_enabled", "0")) && FridayLocation.hasPermission(MainActivity.this);
        }
    }
    @Override public void onDestroy() {
        hudCloneSpeaking.set(false);
        if (hudPlayback != null) hudPlayback.cancel();
        cleanupPhoto();
        if (events != null) unregisterReceiver(events);
        if (testTts != null) { try { testTts.shutdown(); } catch (Exception ignored) {} testTts = null; }
        if (webVoice != null) { try { webVoice.shutdown(); } catch (Exception ignored) {} webVoice = null; }
        if (hud != null) { hud.removeJavascriptInterface("FridayAndroid"); hud.destroy(); }
        super.onDestroy();
        // Le service micro reste actif après fermeture du cockpit, jusqu'à arrêt explicite.
    }
}
