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
    private TextView lastErrorLabel;
    private TextView mistralKeyState;
    private EditText mistralModel, mistralKey, webPageUrl;
    private CheckBox voiceEnabled, streamingEnabled, oneShotEnabled, voskHintsEnabled,
            locationEnabled, locationShare, placeNameEnabled;
    private CheckBox memoryEnabled, memoryShareMistral;
    private CheckBox webEnabled;
    private TextView voiceSampleStatus;
    private volatile boolean voiceRecording = false;
    private volatile boolean voicePreview = false;
    private final AtomicBoolean hudCloneSpeaking = new AtomicBoolean(false);
    private boolean recordAfterMicPermission = false;
    private EditText memoryEditor, memorySearchInput;
    private EditText silenceInput, followupInput, ttsInput, voskHintsEditor;
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
    private volatile String cachedLocation = "";
    private File captureFile;
    private TextToSpeech webVoice;
    private volatile boolean webVoiceReady = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(3, 16, 26));
        getWindow().setNavigationBarColor(Color.rgb(3, 16, 26));
        prefs = new SecurePrefs(this);
        // Migration unique : supprimer l'ancienne clé API de l'autre fournisseur, sans effacer
        // une éventuelle clé Mistral déjà présente dans les anciens profils.
        if (!"1".equals(prefs.get("mistral_only_v330", "0"))) {
            prefs.setSecret("openai", "");
            prefs.setSecret("agnes", "");
            prefs.put("openai_model", "");
            prefs.put("openai_url", "");
            prefs.put("available_models", "");
            prefs.put("available_models_time", "0");
            String previous = prefs.get("mistral_model", "").trim();
            if (previous.isEmpty()) prefs.put("mistral_model", "mistral-small-latest");
            prefs.put("mode", "mistral");
            prefs.put("provider", "mistral");
            prefs.put("mistral_only_v330", "1");
        }
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
        TextView title = label("F.R.I.D.A.Y. · " + FridayVersionGuard.thisVersion(this));
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
        config.addView(label("MOTEUR IA · MISTRAL (fournisseur unique)"));
        mistralModel = field("mistral-small-latest", false);
        mistralModel.setText(prefs.get("mistral_model", "mistral-small-latest"));
        config.addView(label("Modèle Mistral"));
        config.addView(mistralModel);
        config.addView(label("Clé API Mistral (laissez vide pour conserver)"));
        mistralKey = field(prefs.hasSecret("mistral") ? "Clé déjà enregistrée" : "Clé manquante", true);
        config.addView(mistralKey);
        mistralKeyState = label(prefs.hasSecret("mistral")
                ? "✔ Clé Mistral : enregistrée"
                : "✘ Clé Mistral : absente — collez-la puis appuyez sur « Enregistrer la clé »");
        config.addView(mistralKeyState);
        config.addView(button("💾 Enregistrer la clé Mistral", this::saveKeyOnly));
        config.addView(button("🌐 Ouvrir Mistral Studio · clés API", this::openMistralPage));
        config.addView(button("🔍 Détecter les modèles Mistral", this::detectModels));
        webEnabled = new CheckBox(this);
        webEnabled.setText("Recherche Web (Mistral Conversations) : réponses sourcées, API facturable");
        webEnabled.setTextColor(Color.WHITE);
        webEnabled.setChecked("1".equals(prefs.get("web_enabled", "1")));
        config.addView(webEnabled);
        config.addView(button("🌐 Activer et tester la recherche Web Mistral", this::enableAndTestWeb));
        config.addView(label("Le bouton vérifie un appel réel à web_search et les URL sources. Si le modèle choisi échoue, un essai avec mistral-small-latest est effectué sans modifier votre modèle de dialogue. Une erreur de clé, de droits ou de quota reste visible. Les recherches peuvent être facturées."));
        config.addView(button("Tester la lecture d'une page HTTPS publique", this::testHttpsPage));
        webPageUrl = field("https://www.example.com/", false);
        config.addView(label("Lire une URL HTTPS publique, même microphone arrêté. FRIDAY corrige « https// » en « https:// ». Certains sites interdisent la lecture automatique (403) : leur contenu ne sera pas contourné ni inventé. L'extrait disponible est transmis à Mistral uniquement après votre demande."));
        config.addView(webPageUrl);
        config.addView(button("Analyser cette page Web avec Mistral", this::analyzeWebPage));

        config.addView(label("FRIDAY MEMORY+ v3.8.0 · privé, chiffré et sans synchronisation GitHub"));
        memoryEnabled = new CheckBox(this);
        memoryEnabled.setText("Activer ma mémoire locale (notes chiffrées AES-GCM via Android Keystore)");
        memoryEnabled.setTextColor(Color.WHITE);
        memoryEnabled.setChecked("1".equals(prefs.get("memory_enabled", "0")));
        config.addView(memoryEnabled);
        memoryShareMistral = new CheckBox(this);
        memoryShareMistral.setText("J'autorise Mistral à recevoir jusqu'à 5 notes pertinentes pour répondre à mes questions");
        memoryShareMistral.setTextColor(Color.WHITE);
        memoryShareMistral.setChecked("1".equals(prefs.get("memory_share_mistral", "0")));
        config.addView(memoryShareMistral);
        config.addView(label("Ce partage est désactivé par défaut. Les notes restent sur le téléphone sauf consentement explicite. Le texte d'une question envoyée à Mistral quitte normalement le téléphone. Aucun transfert vers GitHub."));
        config.addView(label("Une note par ligne · 200 notes max · 600 caractères chacune. Option : [projet] ou [travail] au début d'une note. Ne stockez pas de mots de passe."));
        memoryEditor = new EditText(this);
        memoryEditor.setSingleLine(false);
        memoryEditor.setMinLines(4);
        memoryEditor.setMaxLines(8);
        memoryEditor.setGravity(Gravity.TOP | Gravity.START);
        memoryEditor.setTextColor(Color.WHITE);
        memoryEditor.setTextSize(14);
        memoryEditor.setText(PersonalMemory.editorText(prefs));
        memoryEditor.setHint("[projet] Mon projet F.R.I.D.A.Y. n'utilise que Mistral");
        config.addView(memoryEditor);
        config.addView(button("Enregistrer les notes chiffrées", this::saveMemory));
        memorySearchInput = field("Rechercher localement (mot, projet ou thème)", false);
        config.addView(memorySearchInput);
        config.addView(button("Rechercher / modifier / supprimer une note", this::showMemorySearch));
        config.addView(label("Les dates de révision sont saisies manuellement : elles ne certifient pas l'exactitude d'une information."));
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
        config.addView(label("Commandes vocales locales : « Friday, retiens que... », « Friday, cherche dans ma mémoire projet », « Friday, qu'as-tu mémorisé ? ». Effacement total uniquement après confirmation dans les réglages."));
        config.addView(label("DÉTECTION DES PHRASES · délai de silence (millisecondes, 900 à 6000)"));
        silenceInput = field("2100", false);
        silenceInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        silenceInput.setText(prefs.get("silence_ms", "2100")); config.addView(silenceInput);
        config.addView(label("CONVERSATION SANS RÉPÉTER FRIDAY · fenêtre (secondes, 15 à 180)"));
        followupInput = field("75", false);
        followupInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        followupInput.setText(prefs.get("followup_sec", "75")); config.addView(followupInput);
        bargeEnabled = new CheckBox(this);
        bargeEnabled.setText("Activer STOP vocal pendant la réponse (« stop », « Friday stop », « j’ai une précision »)");
        bargeEnabled.setTextColor(Color.WHITE);
        bargeEnabled.setChecked("1".equals(prefs.get("barge_in_enabled", "1")));
        config.addView(bargeEnabled);
        config.addView(label("Anti-écho v3.8.4 : la dictée générale Vosk est suspendue pendant les réponses et 2,5 secondes après. Le micro reste disponible pour les commandes STOP et précision grâce au décodeur séparé. Posez une nouvelle question après cette courte pause. Si Android bloque le micro, utilisez le bouton Interrompre."));
        config.addView(label("VITESSE VOCALE · 100 = normale (70 à 150)"));
        ttsInput = field("100", false);
        ttsInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        ttsInput.setText(prefs.get("tts_speed", "100")); config.addView(ttsInput);
        voiceEnabled = new CheckBox(this);
        voiceEnabled.setText("Répondre à voix haute"); voiceEnabled.setTextColor(Color.WHITE);
        voiceEnabled.setChecked("1".equals(prefs.get("voice_enabled", "1")));
        config.addView(voiceEnabled);
        streamingEnabled = new CheckBox(this);
        streamingEnabled.setText("Réponse Mistral progressive (streaming + synthèse vocale au fil des phrases)");
        streamingEnabled.setTextColor(Color.WHITE);
        streamingEnabled.setChecked("1".equals(prefs.get("stream_enabled", "1")));
        config.addView(streamingEnabled);
        config.addView(label("La recherche Web de Mistral peut encore attendre la réponse complète. "
                + "Désactivez cette option si le réseau coupe les flux SSE."));
        config.addView(label("ÉCOUTE ET COMPRÉHENSION · Vosk hors ligne"));
        oneShotEnabled = new CheckBox(this);
        oneShotEnabled.setText("Mode une question : arrêter automatiquement le micro après la réponse");
        oneShotEnabled.setTextColor(Color.WHITE);
        oneShotEnabled.setChecked("1".equals(prefs.get("listen_once", "0")));
        config.addView(oneShotEnabled);
        config.addView(label("Après la réponse, le microphone s'arrête automatiquement. "
                + "Pendant que FRIDAY parle, le micro reste actif pour « stop » "
                + "et « j'ai une précision ». Un casque améliore la reconnaissance "
                + "des commandes et limite l'écho du haut-parleur."));
        voskHintsEnabled = new CheckBox(this);
        voskHintsEnabled.setText("Vocabulaire Vosk spécialisé (expérimental, sur demande)");
        voskHintsEnabled.setTextColor(Color.WHITE);
        voskHintsEnabled.setChecked("1".equals(prefs.get("vosk_hints_enabled", "0")));
        config.addView(voskHintsEnabled);
        config.addView(label("Une courte expression par ligne, maximum 24. "
                + "Une grammaire peut améliorer certaines commandes mais détériorer "
                + "la dictée libre. [unk] est conservé en secours."));
        voskHintsEditor = field("Ex. elite dangerous", false);
        voskHintsEditor.setSingleLine(false);
        voskHintsEditor.setMinLines(3);
        voskHintsEditor.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        voskHintsEditor.setFilters(new android.text.InputFilter[]{
                new android.text.InputFilter.LengthFilter(2100)});
        voskHintsEditor.setText(prefs.secret("vosk_hints"));
        config.addView(voskHintsEditor);
                config.addView(label("LOCALISATION · permission Android facultative"));
        locationEnabled = new CheckBox(this);
        locationEnabled.setText("Activer la position uniquement sur demande vocale");
        locationEnabled.setTextColor(Color.WHITE);
        locationEnabled.setChecked("1".equals(prefs.get("location_enabled", "0")));
        config.addView(locationEnabled);
        locationShare = new CheckBox(this);
        locationShare.setText("Autoriser l'envoi ponctuel de mes coordonnées GPS à Mistral pour les questions locales");
        locationShare.setTextColor(Color.WHITE);
        locationShare.setChecked("1".equals(prefs.get("location_share", "0")));
        config.addView(locationShare);
        config.addView(label("Important : autoriser le GPS Android n'envoie PAS la position à Mistral. "
                + "Cochez aussi le partage ponctuel, ou utilisez « Configurer le GPS pour les réponses locales ». "
                + "Le service vocal peut nécessiter un redémarrage après modification des permissions."));
        placeNameEnabled = new CheckBox(this);
        placeNameEnabled.setText("Identifier la commune ou la rue à la demande (géocodeur du téléphone, peut utiliser son réseau)");
        placeNameEnabled.setTextColor(Color.WHITE);
        placeNameEnabled.setChecked("1".equals(prefs.get("address_lookup", "0")));
        config.addView(placeNameEnabled);
        config.addView(label("Aucune clé Google : ce géocodeur est une fonction d'Android, dont le fournisseur peut accéder à Internet. Désactivé par défaut."));
        config.addView(label("Aucun suivi GPS permanent : mesure à la demande (jusqu'à 16 s) et objectif 3 m non garanti. " +
            "La précision effectivement mesurée est affichée. Si le partage IA est autorisé, les coordonnées précises de cette question sont transmises à l’API Mistral."));
        config.addView(button("◎ Tester précision GPS · cible 3 m", this::showGpsAccuracy));
        config.addView(button("⌖ Autoriser la position GPS à Mistral (ponctuellement)", this::configureGpsForAssistant));
        config.addView(label("DIAGNOSTIC · dernière erreur du moteur IA"));
        lastErrorLabel = label(prefs.get("last_error", "").isEmpty()
                ? "Aucune erreur enregistrée."
                : "Dernière erreur (" + prefs.get("last_error_time", "?") + ") : " + prefs.get("last_error", ""));
        config.addView(lastErrorLabel);
        config.addView(button("⧉ Copier la dernière erreur", this::copyLastError));
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
        config.addView(label("La reconnaissance vocale est locale ; seule la question transcrite est envoyée à l’API Mistral. Les clés API natives sont chiffrées Android Keystore."));

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
            prefs.put("mode", "mistral");
            prefs.put("provider", "mistral");
            prefs.put("mistral_model", mistralModel.getText().toString().trim());
            String mistralGiven = mistralKey.getText().toString().trim();
            if (!mistralGiven.isEmpty()) { prefs.setSecret("mistral", mistralGiven); prefs.put("web_verified", "0"); }
            mistralKey.setText("");
            prefs.put("memory_enabled", memoryEnabled.isChecked() ? "1" : "0");
            prefs.put("memory_share_mistral", memoryEnabled.isChecked() && memoryShareMistral.isChecked() ? "1" : "0");
            prefs.put("web_enabled", webEnabled.isChecked() ? "1" : "0");
            if (!webEnabled.isChecked()) prefs.put("web_verified", "0");
            prefs.put("silence_ms", bounded(silenceInput, 2100, 900, 6000));
            prefs.put("followup_sec", bounded(followupInput, 75, 15, 180));
            prefs.put("barge_in_enabled", bargeEnabled.isChecked() ? "1" : "0");
            prefs.put("tts_speed", bounded(ttsInput, 100, 70, 150));
            prefs.put("voice_enabled", voiceEnabled.isChecked() ? "1" : "0");
            prefs.put("stream_enabled", streamingEnabled.isChecked() ? "1" : "0");
            prefs.put("listen_once", oneShotEnabled.isChecked() ? "1" : "0");
            prefs.put("vosk_hints_enabled", voskHintsEnabled.isChecked() ? "1" : "0");
            String hints = voskHintsEditor.getText().toString();
            if (hints.length() > 2100) throw new IllegalArgumentException("Vocabulaire trop long.");
            prefs.setSecret("vosk_hints", hints);
            prefs.put("location_enabled", locationEnabled.isChecked() ? "1" : "0");
            prefs.put("location_share", locationEnabled.isChecked() && locationShare.isChecked() ? "1" : "0");
            prefs.put("address_lookup", locationEnabled.isChecked() && placeNameEnabled.isChecked() ? "1" : "0");
            String keyInfo = prefs.hasSecret("mistral")
                    ? (SecurePrefs.LAST_ERROR.isEmpty() ? "Clé Mistral enregistrée." : "Clé enregistrée. ⚠ " + SecurePrefs.LAST_ERROR)
                    : "⚠ Aucune clé Mistral enregistrée : les réponses seront refusées.";
            status.setText(keyInfo + " Réglages enregistrés. Arrêtez puis redémarrez le service pour appliquer les permissions.");
            if (locationEnabled.isChecked() && !FridayLocation.hasPermission(this)) requestLocationAccess();
            Toast.makeText(this,"Réglages natifs enregistrés",Toast.LENGTH_SHORT).show();
        } catch (Exception ex) { status.setText("Enregistrement impossible : " + ex.getMessage()); }
    }
        /** Permet aussi de réactiver une référence enregistrée par la v2.1.8 sans la refaire. */
                /** Affiche le statut enregistré, pas seulement l'état visuel d'une case. */
            private void saveMemory() {
        try {
            int count = PersonalMemory.replaceFromEditor(prefs, memoryEditor.getText().toString());
            status.setText("Memory+ : " + count + " notes chiffrées localement. Partage Mistral désactivé par défaut.");
            Toast.makeText(this, "Notes enregistrées : " + count, Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
            status.setText("Échec de la mémoire : " + ex.getMessage());
        }
    }

    private void showMemorySearch() {
        try {
            java.util.List<PersonalMemory.Entry> notes = PersonalMemory.search(
                    prefs, memorySearchInput.getText().toString(), 40);
            if (notes.isEmpty()) {
                status.setText("Aucune note locale correspondant à la recherche.");
                return;
            }
            String[] labels = new String[notes.size()];
            for (int i = 0; i < notes.size(); i++) {
                PersonalMemory.Entry e = notes.get(i);
                String preview = e.editorLine();
                labels[i] = (e.lastReviewedAt > 0 ? "✓ " : "• ") +
                        (preview.length() <= 100 ? preview : preview.substring(0, 100) + "…");
            }
            new AlertDialog.Builder(this).setTitle("Memory+ · notes locales chiffrées")
                    .setItems(labels, (d, which) -> editMemoryEntry(notes.get(which)))
                    .setNegativeButton("Fermer", null).show();
        } catch (Exception ex) {
            status.setText("Recherche mémoire impossible (chiffrement indisponible).");
        }
    }

    private void editMemoryEntry(PersonalMemory.Entry selected) {
        EditText edit = field("Note ou [catégorie] note", false);
        edit.setSingleLine(false);
        edit.setMinLines(3);
        edit.setText(selected.editorLine());
        new AlertDialog.Builder(this)
                .setTitle("Memory+ · " + selected.category +
                        (selected.lastReviewedAt > 0 ? " · révisée manuellement" : " · non révisée"))
                .setView(edit)
                .setPositiveButton("Modifier", (d, which) -> {
                    try {
                        PersonalMemory.update(prefs, selected.id, edit.getText().toString());
                        reloadMemoryEditor();
                        status.setText("Note mise à jour localement et chiffrée.");
                    } catch (Exception ex) { status.setText("Modification impossible : " + ex.getMessage()); }
                })
                .setNeutralButton("Marquer révisée", (d, which) -> {
                    try {
                        PersonalMemory.markReviewed(prefs, selected.id);
                        status.setText("Note marquée comme relue par vous. Véracité non certifiée.");
                    } catch (Exception ex) { status.setText("Révision impossible."); }
                })
                .setNegativeButton("Supprimer", (d, which) -> {
                    new AlertDialog.Builder(this)
                            .setTitle("Supprimer cette note ?")
                            .setMessage("Cette action supprime définitivement la note chiffrée sélectionnée.")
                            .setNegativeButton("Annuler", null)
                            .setPositiveButton("Supprimer", (dialog, button) -> {
                                try {
                                    PersonalMemory.delete(prefs, selected.id);
                                    reloadMemoryEditor();
                                    status.setText("Note supprimée localement.");
                                } catch (Exception ex) { status.setText("Suppression impossible."); }
                            }).show();
                }).show();
    }

    private void reloadMemoryEditor() {
        memoryEditor.setText(PersonalMemory.editorText(prefs));
    }

    private String bounded(EditText input, int fallback, int low, int high) {
        int value;
        try { value = Integer.parseInt(input.getText().toString().trim()); }
        catch (Exception ignored) { value = fallback; }
        value = Math.max(low, Math.min(high, value));
        input.setText(String.valueOf(value));
        return String.valueOf(value);
    }
    /** Enregistre la clé telle qu'elle est saisie, sans toucher au reste des réglages. */
    private void saveKeyOnly() {
        String typed = mistralKey == null ? "" : mistralKey.getText().toString().trim();
        if (typed.isEmpty()) {
            mistralKeyState.setText(prefs.hasSecret("mistral")
                    ? "✔ Clé déjà enregistrée (champ vide : la clé existante est conservée)"
                    : "✘ Aucune clé : collez la clé Mistral (elle commence par « sk- »)");
            return;
        }
        prefs.setSecret("mistral", typed);
        prefs.put("web_verified", "0");
        mistralKey.setText("");
        boolean ok = prefs.hasSecret("mistral");
        mistralKeyState.setText(ok
                ? "✔ Clé Mistral enregistrée" + (SecurePrefs.LAST_ERROR.isEmpty() ? "" : " — ⚠ " + SecurePrefs.LAST_ERROR)
                : "✘ Échec de l'enregistrement de la clé");
        status.setText(ok ? "Clé Mistral enregistrée. Vous pouvez détecter les modèles." : "Clé non enregistrée.");
        Toast.makeText(this, ok ? "Clé Mistral enregistrée" : "Clé non enregistrée", Toast.LENGTH_SHORT).show();
    }

    /** Activation immédiate, puis test réseau réellement exécuté hors du thread UI. */
    private void enableAndTestWeb() {
        webEnabled.setChecked(true);
        prefs.put("web_enabled", "1");
        prefs.put("web_verified", "0");
        status.setText("Recherche Web activée. Vérification de l'API Mistral…");
        new Thread(() -> {
            String result;
            try { result = NativeAI.testWeb(prefs); }
            catch (Exception error) {
                result = "Recherche Web activée, mais test impossible : "
                        + (error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage());
            }
            final String detail = result;
            prefs.put("web_verified", detail.startsWith("Recherche Web Mistral opérationnelle") ? "1" : "0");
            runOnUiThread(() -> {
                status.setText(detail);
                if (pageReady && hud != null)
                    hud.evaluateJavascript("window.fridayRefreshNative&&window.fridayRefreshNative();", null);
            });
        }, "friday-test-web").start();
    }

    /** Test explicite : lecture HTTP de page publique sans transmettre le contenu à Mistral. */
    private void testHttpsPage() {
        status.setText("Test HTTPS en cours sur www.example.com…");
        new Thread(() -> {
            String result;
            try {
                WebPageReader.Page page = WebPageReader.read("https://www.example.com/");
                result = "Lecture HTTPS opérationnelle (" + page.text.length()
                        + " caractères). Cela ne prouve pas l'accès aux outils Web Mistral.";
            } catch (Exception ex) {
                result = "Lecture HTTPS indisponible : " + ex.getMessage();
            }
            final String text = result;
            runOnUiThread(() -> status.setText(text));
        }, "friday-test-page-http").start();
    }

    /** Analyse écrite autonome : aucun microphone ni service vocal nécessaires. */
    private void analyzeWebPage() {
        if (!webEnabled.isChecked()) {
            status.setText("Activez la recherche Web dans Réglages.");
            return;
        }
        final String url;
        try {
            url = WebPageReader.checkedUri(webPageUrl.getText().toString()).toString();
            webPageUrl.setText(url);  // Affiche le préfixe HTTPS réparé à l'utilisateur.
        } catch (Exception ex) {
            status.setText("Adresse Web invalide : " + ex.getMessage()
                    + " Exemple : https://www.example.com/");
            return;
        }
        prefs.put("web_enabled", "1");
        status.setText("Lecture de la page HTTPS en cours, sans microphone…");
        new Thread(() -> {
            final String answer;
            try {
                answer = NativeAI.answer(prefs,
                        "Analyse le contenu réellement accessible de cette page Web, "
                        + "résume ses informations et cite uniquement cette source : " + url,
                        java.util.Collections.emptyList(), null);
            } catch (Exception failure) {
                String message = failure.getMessage() == null
                        ? failure.getClass().getSimpleName() : failure.getMessage();
                runOnUiThread(() -> status.setText(
                        "Analyse Web impossible : " + message));
                return;
            }
            runOnUiThread(() -> {
                status.setText(answer.startsWith("Je ne peux pas lire")
                        ? "Accès à cette page refusé ou indisponible : voir le diagnostic."
                        : "Analyse de page terminée · consultez la réponse.");
                new AlertDialog.Builder(this)
                        .setTitle("FRIDAY · Page Web")
                        .setMessage(answer)
                        .setPositiveButton("Fermer", null)
                        .show();
                if (pageReady && hud != null)
                    hud.evaluateJavascript("window.fridayRefreshNative&&window.fridayRefreshNative();", null);
            });
        }, "friday-webpage-no-microphone").start();
    }

    /** Une liste de modèles ne prouve pas la compatibilité avec web_search ou vision. */
    private void showModelChoices(java.util.List<String> models, boolean cached) {
        if (models.isEmpty()) {
            status.setText("Aucun modèle Mistral accessible à cette clé.");
            return;
        }
        String[] names = models.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("Modèles Mistral " + (cached ? "· cache 1 h" : "· API"))
                .setMessage("Le modèle sélectionné est utilisé pour le chat. "
                        + "L'outil Web nécessite une vérification séparée avec "
                        + "« Activer et tester la recherche Web ».")
                .setSingleChoiceItems(names, Math.max(0, models.indexOf(
                        prefs.get("mistral_model", "mistral-small-latest"))), (dialog, index) -> {
                    String name = names[index];
                    prefs.put("mistral_model", name);
                    mistralModel.setText(name);
                    prefs.put("web_verified", "0");
                    status.setText("Modèle Mistral choisi : " + name + ". Testez le Web séparément.");
                    dialog.dismiss();
                })
                .setNeutralButton("Actualiser", (dialog, which) -> {
                    prefs.put("available_models_time", "0");
                    detectModels();
                })
                .setNegativeButton("Annuler", null).show();
    }

    private void detectModels() {
        // La clé saisie mais non encore enregistrée est prise en compte immédiatement.
        if (mistralKey != null) {
            String typed = mistralKey.getText().toString().trim();
            if (!typed.isEmpty()) { prefs.setSecret("mistral", typed); mistralKey.setText(""); }
        }
        final String key = prefs.secret("mistral");
        if (key.isEmpty()) { status.setText("Enregistrez d'abord la clé Mistral."); return; }
        final String url = "https://api.mistral.ai/v1";
        String cached = prefs.get("available_models", "");
        long savedAt = 0L;
        try { savedAt = Long.parseLong(prefs.get("available_models_time", "0")); }
        catch (NumberFormatException ignored) {}
        if (!cached.isEmpty() && System.currentTimeMillis() - savedAt < 3600000L
                && savedAt <= System.currentTimeMillis()) {
            showModelChoices(java.util.Arrays.asList(cached.split(",")), true);
            return;
        }
        status.setText("Interrogation de la liste des modèles…");
        new Thread(() -> {
            try {
                java.util.List<String> models = NativeAI.listModels(url, key);
                runOnUiThread(() -> {
                    if (models.isEmpty()) {
                        status.setText("Aucun modèle renvoyé par " + url + "/models");
                        return;
                    }
                    prefs.put("available_models", String.join(",", models));
                    prefs.put("available_models_time", String.valueOf(System.currentTimeMillis()));
                    showModelChoices(models, false);
                });
            } catch (final Exception e) {
                runOnUiThread(() -> status.setText("Détection impossible : " + e.getMessage()));
            }
        }, "friday-models").start();
    }

    private void copyLastError() {
        String err = prefs.get("last_error", "");
        String when = prefs.get("last_error_time", "");
        String payload = err.isEmpty() ? "Aucune erreur enregistrée." :
                "[" + when + "] " + err + "\nmodèle=" + prefs.get("mistral_model", "?")
                + " url=" + "https://api.mistral.ai/v1"
                + " clé=" + (prefs.hasSecret("mistral") ? "présente" : "absente");
        android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (cm != null) cm.setPrimaryClip(android.content.ClipData.newPlainText("friday-error", payload));
        Toast.makeText(this, "Erreur copiée dans le presse-papiers", Toast.LENGTH_SHORT).show();
    }

    private void openMistralPage() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://console.mistral.ai/api-keys/")));
            status.setText("Page Mistral ouverte : créez ou copiez votre clé, puis collez-la ici.");
        } catch (Exception e) {
            status.setText("Aucun navigateur disponible : ouvrez https://console.mistral.ai/api-keys/");
        }
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
        try {
            PrivateConversationHistory.clear(this, prefs);
            if (pageReady) hud.evaluateJavascript("window.fridayNativeReset&&window.fridayNativeReset();", null);
            status.setText("Historique vocal chiffré effacé sur cet appareil.");
        } catch (Exception ex) {
            status.setText("Suppression de l'historique impossible : stockage sécurisé indisponible.");
        }
    }
    private void installModel() {
        if (ModelInstaller.installed(this)) { status.setText("Modèle français déjà installé."); return; }
        status.setText("Téléchargement du modèle français lancé…");
        new Thread(() -> {
            try { ModelInstaller.download(this, s -> runOnUiThread(() -> status.setText(s))); }
            catch (Exception e) { runOnUiThread(() -> status.setText("Erreur téléchargement : " + e.getMessage())); }
        }, "friday-model-download").start();
    }
    /** Le partage de coordonnées avec Mistral requiert un consentement distinct. */
    private void configureGpsForAssistant() {
        new AlertDialog.Builder(this)
                .setTitle("Localisation de F.R.I.D.A.Y.")
                .setMessage("Le GPS peut répondre localement à « Où suis-je ? ». "
                        + "Voulez-vous aussi transmettre ponctuellement les coordonnées à Mistral "
                        + "uniquement lorsque vous posez une question sur les alentours ?")
                .setPositiveButton("Autoriser le partage ponctuel", (dialog, which) -> {
                    prefs.put("location_enabled", "1");
                    prefs.put("location_share", "1");
                    locationEnabled.setChecked(true);
                    locationShare.setChecked(true);
                    status.setText("GPS et partage ponctuel avec Mistral autorisés. "
                            + (FridayService.isActive() ? "Arrêtez puis redémarrez l'écoute pour appliquer les permissions GPS." : "Prêt pour une question sur votre position."));
                    showGpsAccuracy();
                })
                .setNeutralButton("GPS local uniquement", (dialog, which) -> {
                    prefs.put("location_enabled", "1");
                    prefs.put("location_share", "0");
                    locationEnabled.setChecked(true);
                    locationShare.setChecked(false);
                    status.setText("GPS local autorisé, aucune coordonnée envoyée à Mistral.");
                    showGpsAccuracy();
                })
                .setNegativeButton("Annuler", (dialog, which) -> {})
                .show();
    }

    /** Acquisition réelle avec états distincts (permission, GPS, position). */
    private void showGpsAccuracy() {
        if (!"1".equals(prefs.get("location_enabled", "0"))) {
            prefs.put("location_enabled", "1");
            locationEnabled.setChecked(true);
            // Ne jamais effacer un consentement de partage déjà donné simplement
            // parce que l'utilisateur vérifie sa position sur l'écran GPS.
        }
        if (!FridayLocation.hasPermission(this)) {
            cachedLocation = "Autorisation Android de localisation manquante.";
            status.setText(cachedLocation);
            requestLocationAccess();
            return;
        }
        if (!FridayLocation.providerEnabled(this)) {
            cachedLocation = "Localisation Android désactivée : activez le GPS dans le téléphone.";
            status.setText(cachedLocation);
            if (pageReady && hud != null)
                hud.evaluateJavascript("window.fridayLocationSync&&window.fridayLocationSync();", null);
            return;
        }
        cachedLocation = "Acquisition GPS en cours (objectif 3 m non garanti)…";
        status.setText(cachedLocation);
        if (pageReady && hud != null)
            hud.evaluateJavascript("window.fridayLocationSync&&window.fridayLocationSync();", null);
        new Thread(() -> {
            String result;
            try {
                FridayLocation.Fix fix = FridayLocation.locate(getApplicationContext());
                String place = fix != null && "1".equals(prefs.get("address_lookup", "0"))
                        ? AndroidPlaceResolver.resolve(getApplicationContext(), fix) : "";
                result = fix == null
                        ? "Android n'a pas fourni de position récente. Réessayez dehors."
                        : AndroidPlaceResolver.userText(fix, place);
            } catch (Exception error) {
                result = "Acquisition GPS impossible : " + error.getClass().getSimpleName();
            }
            cachedLocation = result;
            runOnUiThread(() -> {
                status.setText(cachedLocation);
                if (pageReady && hud != null)
                    hud.evaluateJavascript("window.fridayLocationSync&&window.fridayLocationSync();"
                            + "window.fridayRefreshNative&&window.fridayRefreshNative();", null);
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
        } else if (requestCode == REQ_LOCATION) {
            if (FridayLocation.hasPermission(this)) {
                locationEnabled.setChecked(true);
                prefs.put("location_enabled", "1");
                status.setText("Position autorisée. Mesure GPS en cours ; redémarrez l'écoute pour l'utiliser écran verrouillé.");
                showGpsAccuracy();
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
                    if (webVoice != null) { try { webVoice.stop(); } catch (Exception ignored) { } }
                }
                if (kind.equals("location")) {
                    cachedLocation = text;
                }
                if (kind.equals("gps_api")) {
                    status.setText(text);
                }
                if (kind.equals("error")) {
                    prefs.put("last_error", text);
                    prefs.put("last_error_time", new java.text.SimpleDateFormat("dd/MM HH:mm:ss", java.util.Locale.FRANCE).format(new java.util.Date()));
                    if (lastErrorLabel != null) lastErrorLabel.setText("Dernière erreur : " + text);
                }
                if (kind.equals("status") || kind.equals("error") || kind.equals("tts") || kind.equals("mic_conflict") || kind.equals("interrupted")) status.setText(text);
                if (pageReady) {
                    if (kind.equals("user") || kind.equals("assistant")) replayVoiceHistory();
                    else {
                        String js = "window.fridayNativeReceive&&window.fridayNativeReceive(" + JSONObjectQuote(kind) + "," + JSONObjectQuote(text) + ");";
                        hud.evaluateJavascript(js, null);
                        if (kind.equals("gps_api"))
                            hud.evaluateJavascript("window.fridayRefreshNative&&window.fridayRefreshNative();", null);
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
        try {
            String eventsJson = PrivateConversationHistory.read(this, prefs);
            new org.json.JSONArray(eventsJson);
            hud.evaluateJavascript("window.fridayNativeInbox&&window.fridayNativeInbox(" + eventsJson + ");", null);
        } catch (Exception encryptedFailure) {
            status.setText("Historique privé illisible : Keystore Android indisponible. Aucun accès en clair.");
        }
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
            /** Évite le chargement de plusieurs dizaines de Mo depuis le capteur photo. */
        private static String safeError(Exception ex) {
        String message = ex.getMessage();
        return message == null || message.trim().isEmpty() ? ex.getClass().getSimpleName() : message;
    }

                private void playWebVoice(String text) {
        if (text == null || text.trim().isEmpty()) return;
        final String spoken = text.length() > 5000 ? text.substring(0, 5000) : text;
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
    /** Configuration vérifiée, sans assimiler le réglage activé à un accès Web validé. */
    private String webTelemetry() {
        if (!"1".equals(prefs.get("web_enabled", "1"))) return "Désactivée";
        if (!prefs.hasSecret("mistral")) return "Clé Mistral absente";
        String state = "1".equals(prefs.get("web_verified", "0"))
                ? "Activée · testée" : "Activée · non testée";
        String recent = prefs.get("web_last_status", "");
        return recent.isEmpty() ? state : state + " · " + recent;
    }
    private String gpsTelemetry() {
        if (!"1".equals(prefs.get("location_enabled", "0"))) return "Désactivé";
        if (!FridayLocation.hasPermission(this)) return "Autorisation manquante";
        if (!FridayLocation.providerEnabled(this)) return "GPS Android arrêté";
        return FridayLocation.isFine(this) ? "Autorisé · précis" : "Autorisé · approximatif";
    }
    private String gpsShareTelemetry() {
        if (!"1".equals(prefs.get("location_enabled", "0"))) return "GPS désactivé";
        if (!"1".equals(prefs.get("location_share", "0"))) return "Non autorisé";
        if (!FridayLocation.hasPermission(this)) return "Permission Android manquante";
        if (!FridayLocation.providerEnabled(this)) return "Localisation Android arrêtée";
        if (FridayService.isActive() && !FridayService.locationForegroundEnabled())
            return "Redémarrer l'écoute";
        String timestamp = prefs.get("gps_last_shared", "");
        try {
            long time = Long.parseLong(timestamp);
            if (time > 0)
                return "Autorisé · dernier envoi "
                        + new java.text.SimpleDateFormat("HH:mm", java.util.Locale.FRANCE)
                        .format(new java.util.Date(time));
        } catch (NumberFormatException ignored) {}
        return "Autorisé · en attente d'une question locale";
    }
    private final class NativeBridge {
        @JavascriptInterface public String activeProfileName() { return PersonalityProfiles.activeName(prefs); }
        @JavascriptInterface public String listeningMode() {
            return "1".equals(prefs.get("listen_once", "0"))
                    ? "Une question" : "Continu";
        }
        @JavascriptInterface public String vocabularyMode() {
            if (!"1".equals(prefs.get("vosk_hints_enabled", "0"))) return "Vosk libre";
            int lines = VoiceVocabulary.phrases(prefs.secret("vosk_hints")).size();
            return lines == 0 ? "Vosk libre" : "Vosk · " + lines + " expressions";
        }
        @JavascriptInterface public String webTelemetry() { return MainActivity.this.webTelemetry(); }
        @JavascriptInterface public String gpsTelemetry() { return MainActivity.this.gpsTelemetry(); }
        @JavascriptInterface public String gpsShareTelemetry() { return MainActivity.this.gpsShareTelemetry(); }
        @JavascriptInterface public void gpsConsent() { runOnUiThread(MainActivity.this::configureGpsForAssistant); }
        @JavascriptInterface public String getPersonalityProfiles() {
            return PersonalityProfiles.read(prefs);
        }
        @JavascriptInterface public String savePersonalityProfiles(String json) {
            try {
                PersonalityProfiles.save(prefs, json);
                runOnUiThread(() -> {
                    if (pageReady && hud != null)
                        hud.evaluateJavascript("window.fridayRefreshNative&&window.fridayRefreshNative();", null);
                });
                return "OK";
            }
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
        @JavascriptInterface public boolean isMicSilenced() { return FridayService.isMicSilenced(); }
        @JavascriptInterface public String voiceInterruptTelemetry() {
            if (!"1".equals(prefs.get("barge_in_enabled", "1"))) return "Désactivé dans Réglages";
            return FridayService.interruptionDiagnostics();
        }
        @JavascriptInterface public String currentVersion() { return FridayVersionGuard.thisVersion(MainActivity.this); }
        @JavascriptInterface public int otherVersionsCount() { return FridayVersionGuard.installed(MainActivity.this).size(); }
        @JavascriptInterface public void manageVersions() { runOnUiThread(() -> FridayVersionGuard.showManager(MainActivity.this)); }
        @JavascriptInterface public void gpsCheck() { runOnUiThread(MainActivity.this::showGpsAccuracy); }
        @JavascriptInterface public String locationText() { return cachedLocation; }
        @JavascriptInterface public void locationRefresh() { runOnUiThread(MainActivity.this::refreshLocation); }
        @JavascriptInterface public void speakNative(String text) { runOnUiThread(() -> playWebVoice(text)); }
        @JavascriptInterface public void stopSpeakingNative() {
            runOnUiThread(() -> { hudCloneSpeaking.set(false);
                if (webVoice != null) webVoice.stop(); });
        }
        @JavascriptInterface public boolean locationEnabled() {
            return "1".equals(prefs.get("location_enabled", "0")) && FridayLocation.hasPermission(MainActivity.this);
        }
    }
    @Override public void onDestroy() {
        hudCloneSpeaking.set(false);
        if (events != null) unregisterReceiver(events);
        if (testTts != null) { try { testTts.shutdown(); } catch (Exception ignored) {} testTts = null; }
        if (webVoice != null) { try { webVoice.shutdown(); } catch (Exception ignored) {} webVoice = null; }
        if (hud != null) { hud.removeJavascriptInterface("FridayAndroid"); hud.destroy(); }
        super.onDestroy();
        // Le service micro reste actif après fermeture du cockpit, jusqu'à arrêt explicite.
    }
}
