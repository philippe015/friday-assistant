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
    private CheckBox webEnabled, webAlways;
    private TextView webDiagnosticLabel;
    private final AtomicBoolean webTestRunning = new AtomicBoolean(false);
    private CheckBox localJarvisEnabled, localJarvisAll;
    private CheckBox jarvisInternetEnabled, jarvisWebEveryAnswerEnabled,
            jarvisWorldRssTranslateFrEnabled;
    private TextView jarvisInternetStatus;
    private final AtomicBoolean jarvisWebTesting = new AtomicBoolean(false);
    private TextView localJarvisStatus;
    private final AtomicBoolean localJarvisImporting = new AtomicBoolean(false);
    private final AtomicBoolean localJarvisTesting = new AtomicBoolean(false);
    /** Empêche les téléchargements Vosk concurrents et reprend la demande d'écoute. */
    private final AtomicBoolean voskDownloadRunning = new AtomicBoolean(false);
    private volatile boolean startAfterVoskDownload = false;
    private TextView voiceSampleStatus;
    private volatile boolean voiceRecording = false;
    private volatile boolean voicePreview = false;
    private final AtomicBoolean hudCloneSpeaking = new AtomicBoolean(false);
    private boolean recordAfterMicPermission = false;
    private EditText memoryEditor, memorySearchInput;
    private EditText silenceInput, followupInput, ttsInput, voskHintsEditor;
    private CheckBox followupHandsfreeEnabled, autoListenOnOpen;
    /** One automatic start per Activity open; never rearm after manual Stop. */
    private boolean startupListenAttempted = false;
    private boolean manuallyStoppedListening = false;
    private boolean activityVisible = false;
    /** Le service vocal autorise les questions directes pendant que le cockpit
     * est réellement visible, même après expiration de la fenêtre de 20 s. */
    private static volatile boolean cockpitVisible = false;
    public static boolean isCockpitVisible() { return cockpitVisible; }
    private LinearLayout config;
    private ScrollView configScroll;
    private WebView hud;
    private TextToSpeech testTts;
    private EditText manualQuestion;
    private TextView manualAnswerLabel;
    private final AtomicBoolean typedQuestionRunning = new AtomicBoolean(false);
    private boolean pageReady = false;
    private boolean promptStartAfterPermission = false;
    private BroadcastReceiver events;
    private static final int REQ_MIC = 17;
    private static final int REQ_LOCATION = 19;
    private static final int REQ_PHOTO = 82;
    private static final int REQ_VOICE_SAMPLE = 83;
    private static final int REQ_LOCAL_LLM_MODEL = 84;
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
            if (previous.isEmpty() || "mistral-small-latest".equals(previous)) {
                // mistral-small-latest renvoie 429 sur certaines offres ; ministral-8b est fiable.
                prefs.put("mistral_model", "ministral-8b-latest");
            }
            prefs.put("mode", "mistral");
            prefs.put("provider", "mistral");
            prefs.put("mistral_only_v330", "1");
        }
        // Réserver FRIDAY à Jarvis Android et Mistral, y compris après mise à jour.
        prefs.put("openjarvis_enabled", "0");
        prefs.put("openjarvis_route_all", "0");
        // L'utilisateur a demandé l'accès Web systématique : migration unique
        // des réglages des anciennes versions, sans toucher à la clé Mistral.
        // Les modifications explicites faites ensuite restent respectées.
        if (!"1".equals(prefs.get("web_always_default_v386", "0"))) {
            prefs.put("web_enabled", "1");
            prefs.put("web_mode", "always");
            prefs.put("web_always_default_v386", "1");
        }
        // Edition JARVIS 100 % locale. Désactivation persistante de tout routage
        // Mistral, du partage vers le cloud et de la recherche Web par LLM.
        if (!"1".equals(prefs.get("jarvis_exclusive_v3130", "0"))) {
            prefs.setSecret("mistral", "");
            prefs.put("voice_enabled", "1");
            prefs.put("jarvis_exclusive_v3130", "1");
        }
        prefs.put("mode", "jarvis_local");
        prefs.put("provider", "jarvis_local");
        prefs.put("local_jarvis_enabled", "1");
        prefs.put("local_jarvis_all", "1");
        // L'accès Web de Jarvis est indépendant de l'ancien accès Mistral.
        // Migration initiale activée à la demande ; l'utilisateur peut le couper.
        if (!"1".equals(prefs.get("jarvis_web_migrated_v3132", "0"))) {
            prefs.put("jarvis_web_enabled", "1");
            prefs.put("jarvis_web_migrated_v3132", "1");
        }
        prefs.put("web_enabled", "0");
        prefs.put("web_mode", "off");
        prefs.put("stream_enabled", "0");
        prefs.put("memory_share_mistral", "0");
        prefs.put("location_share", "0");
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
        // Android 15+ (target 35) dessine sous les barres système : préserver
        // la zone caméra, les gestes et le clavier sans rogner les boutons.
        if (Build.VERSION.SDK_INT >= 35) {
            outer.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets safe = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
                return insets;
            });
        }
        setContentView(outer);
        if (Build.VERSION.SDK_INT >= 35) outer.requestApplyInsets();
        final boolean compactPhone = getResources().getConfiguration().screenWidthDp < 520;
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(compactPhone ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        bar.setPadding(dp(7),dp(3),dp(7),dp(3));
        bar.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = label("J.A.R.V.I.S. · " + FridayVersionGuard.thisVersion(this));
        title.setTextSize(15); title.setTextColor(Color.rgb(71,228,249));
        bar.addView(title, new LinearLayout.LayoutParams(
                compactPhone ? -1 : 0, dp(compactPhone ? 38 : 48), compactPhone ? 0 : 1));
        LinearLayout menuRow = new LinearLayout(this);
        menuRow.setOrientation(LinearLayout.HORIZONTAL);
        Button configure = button("⚙ Réglages", () -> configScroll.setVisibility(
                configScroll.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
        menuRow.addView(configure, new LinearLayout.LayoutParams(
                compactPhone ? 0 : -2, dp(48), compactPhone ? 1 : 0));
        menuRow.addView(button("◈ Versions", () -> FridayVersionGuard.showManager(this)),
                new LinearLayout.LayoutParams(compactPhone ? 0 : -2, dp(48), compactPhone ? 1 : 0));
        menuRow.addView(button("✧ Profil", () -> {
            if (hud != null && pageReady) {
                configScroll.setVisibility(View.GONE);
                hud.evaluateJavascript("window.fridayOpenPersonality&&window.fridayOpenPersonality();", null);
            } else { configScroll.setVisibility(View.VISIBLE); }
        }), new LinearLayout.LayoutParams(
                compactPhone ? 0 : -2, dp(48), compactPhone ? 1 : 0));
        bar.addView(menuRow, new LinearLayout.LayoutParams(
                compactPhone ? -1 : -2, compactPhone ? dp(48) : -2));
        outer.addView(bar);
        LinearLayout commands = new LinearLayout(this);
        commands.setPadding(dp(7),0,dp(7),0);
        commands.addView(button("◉ Écoute ON / OFF écran", this::startListening), new LinearLayout.LayoutParams(0,dp(52),2));
        commands.addView(button("■ Stop", this::stopListening), new LinearLayout.LayoutParams(0,dp(52),1));
        outer.addView(commands);
        outer.addView(button("⏸ Interrompre la réponse et écouter", this::interruptSpeaking));
        LinearLayout checks = new LinearLayout(this);
        checks.setPadding(dp(7),0,dp(7),0);
        checks.addView(button("🔊 Tester le haut-parleur", this::testVoice), new LinearLayout.LayoutParams(0,dp(48),1));
        checks.addView(button("✓ Tester IA Jarvis locale", this::testLocalAnswer), new LinearLayout.LayoutParams(0,dp(48),1));
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
        // Les réglages n'écrasent plus le cockpit au démarrage sur téléphone.
        int settingsHeight = Math.min(dp(340),
                Math.max(dp(160), getResources().getDisplayMetrics().heightPixels / 2));
        outer.addView(configScroll, new LinearLayout.LayoutParams(-1, settingsHeight));
        configScroll.setVisibility(View.GONE);
        config.addView(label("DÉMARRAGE DE JARVIS · écoute prête à l'ouverture"));
        autoListenOnOpen = new CheckBox(this);
        autoListenOnOpen.setText("Démarrer automatiquement l'écoute quand j'ouvre JARVIS");
        autoListenOnOpen.setTextColor(Color.WHITE);
        autoListenOnOpen.setChecked(AutoListenPolicy.enabled(
                prefs.get("jarvis_auto_listen_on_open", "1")));
        autoListenOnOpen.setOnCheckedChangeListener((button, checked) -> {
            // Enregistré immédiatement : aucun redémarrage et aucune permission cachée.
            prefs.put("jarvis_auto_listen_on_open", checked ? "1" : "0");
            if (status != null) status.setText(checked
                    ? "Écoute automatique activée pour la prochaine ouverture de JARVIS."
                    : "Écoute automatique désactivée. Le bouton Écoute reste disponible.");
        });
        config.addView(autoListenOnOpen);
        config.addView(label("Par défaut JARVIS démarre le micro quand l'application est visible, "
                + "avec l'autorisation Android. Stop arrête l'écoute sans redémarrage forcé. "
                + "Si Vosk manque, JARVIS demande votre accord avant téléchargement. "
                + "Le micro ne peut pas être démarré automatiquement au redémarrage du téléphone "
                + "sur Android 14 et plus."));
        config.addView(label("MOTEUR VOCAL LOCAL · français Vosk (~41 Mo à télécharger une fois)"));
        config.addView(button(ModelInstaller.installed(this) ? "✓ Modèle installé" : "↓ Télécharger le modèle français", this::installModel));
        config.addView(label("MOTEUR UNIQUE · Jarvis IA installé sur le téléphone. "
                + "Aucun fournisseur Mistral, aucune requête Web vers un LLM. "
                + "Téléchargez et importez un modèle compatible pour générer vos réponses. "
                + "Les réponses aux questions écrites sont également prononcées."));
        config.addView(label("JARVIS SUR ANDROID · moteur local + recherche Internet publique"));
        config.addView(label("Le raisonnement reste sur le téléphone (MediaPipe .task ou LiteRT-LM "
                + ".litertlm). Pour les questions récentes et les demandes de recherche, "
                + "Jarvis peut consulter des pages HTTPS publiques et citer les sources. "
                + "Aucun moteur IA externe n'est utilisé."));
        localJarvisEnabled = new CheckBox(this);
        localJarvisEnabled.setText("Activer Jarvis IA local sur ce téléphone");
        localJarvisEnabled.setTextColor(Color.WHITE);
        localJarvisEnabled.setChecked(true);
        localJarvisEnabled.setEnabled(false);
        config.addView(localJarvisEnabled);
        localJarvisAll = new CheckBox(this);
        localJarvisAll.setText("Toutes les questions sont traitées localement par Jarvis");
        localJarvisAll.setTextColor(Color.WHITE);
        localJarvisAll.setChecked(true);
        localJarvisAll.setEnabled(false);
        config.addView(localJarvisAll);
        config.addView(label("Jarvis fonctionne uniquement avec un modèle local compatible. "
                + "Les réponses sont générées hors ligne, sans Mistral. "
                + "La mémoire et les échanges restent chiffrés sur ce téléphone."));
        config.addView(button("Importer un modèle IA local (.task / .litertlm)", this::chooseLocalJarvisModel));
        config.addView(button("Gemma 3 léger · MediaPipe .task (304 Mo)",
                this::openLocalJarvisModels));
        config.addView(button("Gemma 4 E2B GPU · LiteRT-LM .litertlm (2,34 Go)",
                this::openLocalJarvisGemma4Models));
        config.addView(button("Tester Jarvis IA locale sans Internet", this::testLocalJarvis));
        config.addView(button("Enregistrer Jarvis local", this::saveLocalJarvis));
        localJarvisStatus = label(LocalJarvisEngine.status(this, prefs));
        localJarvisStatus.setTextColor(Color.rgb(255, 228, 143));
        config.addView(localJarvisStatus);
        config.addView(label("INTERNET JARVIS · recherche publique HTTPS"));
        jarvisInternetEnabled = new CheckBox(this);
        jarvisInternetEnabled.setText("Autoriser Jarvis à rechercher des informations sur Internet");
        jarvisInternetEnabled.setTextColor(Color.WHITE);
        jarvisInternetEnabled.setChecked("1".equals(prefs.get("jarvis_web_enabled", "1")));
        config.addView(jarvisInternetEnabled);
        jarvisWebEveryAnswerEnabled = new CheckBox(this);
        jarvisWebEveryAnswerEnabled.setText("Rechercher sur le Web avant toute réponse générale "
                + "(plusieurs fournisseurs, activé par défaut)");
        jarvisWebEveryAnswerEnabled.setTextColor(Color.WHITE);
        jarvisWebEveryAnswerEnabled.setChecked(
                "1".equals(prefs.get("jarvis_web_every_answer", "1")));
        config.addView(jarvisWebEveryAnswerEnabled);
        config.addView(label("ACTUALITÉS MONDIALES · flux RSS multilingues"));
        jarvisWorldRssTranslateFrEnabled = new CheckBox(this);
        jarvisWorldRssTranslateFrEnabled.setText(
                "Traduire les titres et extraits RSS internationaux en français avec Gemma local");
        jarvisWorldRssTranslateFrEnabled.setTextColor(Color.WHITE);
        jarvisWorldRssTranslateFrEnabled.setChecked(
                "1".equals(prefs.get("jarvis_world_rss_translate_fr", "1")));
        config.addView(jarvisWorldRssTranslateFrEnabled);
        config.addView(label("Exemples : « Jarvis, les actualités mondiales des "
                + "24 dernières heures » ou « Les actualités du Japon ». "
                + "Catalogue mondial de 17 flux publics : français, anglais, espagnol, "
                + "allemand, portugais, japonais, arabe, hindi et italien. "
                + "Un sous-ensemble des flux est consulté à chaque question. "
                + "Gemma traduit sur le téléphone et affiche les titres originaux, "
                + "les dates et les liens des sources. Il n'existe aucune garantie "
                + "d'accès à tous les RSS, ni de traduction parfaite. "
                + "L'historique privé n'est pas transmis aux éditeurs."));
        config.addView(label("Fournisseurs : Bing RSS, DuckDuckGo, Wikipédia, Wikidata, "
                + "Crossref (publications scientifiques) et Google Actualités. "
                + "Les questions générales interrogent plusieurs sources publiques "
                + "avant la réponse de Jarvis local. Le nom des moteurs ne garantit pas "
                + "que chacun répondra à chaque requête (HTTP 202, 403 ou 429 possibles). "
                + "Les commandes de mémoire privée, GPS et réglages restent locales ; "
                + "ni historique, ni notes, ni coordonnées ne sont envoyés. "
                + "Décochez cette case pour rétablir la recherche automatique ciblée."));
        config.addView(button("Tester l'accès Internet Jarvis", this::testJarvisInternet));
        config.addView(button("Tester les actualités des 7 dernières heures",
                this::testJarvisRecentNews));
        jarvisInternetStatus = label("Internet Jarvis : test non effectué.");
        jarvisInternetStatus.setTextIsSelectable(true);
        config.addView(jarvisInternetStatus);
        config.addView(label("Formats reconnus : .task Android MediaPipe ou .litertlm "
                + "LiteRT-LM. Les modèles GGUF et les tâches *-web.task ne sont pas "
                + "acceptés. Gemma 4 E2B GPU demande beaucoup de mémoire et "
                + "un pilote GPU/OpenCL compatible ; préférez le petit Gemma 3 .task "
                + "sur un téléphone moins puissant. L'IA locale ne connaît pas les "
                + "actualités Internet hors connexion."));
        config.addView(label("FRIDAY MEMORY+ v3.8.0 · privé, chiffré et sans synchronisation GitHub"));
        memoryEnabled = new CheckBox(this);
        memoryEnabled.setText("Activer ma mémoire locale (notes chiffrées AES-GCM via Android Keystore)");
        memoryEnabled.setTextColor(Color.WHITE);
        memoryEnabled.setChecked("1".equals(prefs.get("memory_enabled", "0")));
        config.addView(memoryEnabled);
        memoryShareMistral = new CheckBox(this);
        memoryShareMistral.setChecked(false);
        config.addView(label("Aucun transfert vers Mistral ou GitHub. "
                + "Les notes restent privées et chiffrées sur ce téléphone."));
        config.addView(label("Une note par ligne · 200 notes max · 600 caractères chacune. Option : [projet] ou [travail] au début d'une note. Ne stockez pas de mots de passe."));
        memoryEditor = new EditText(this);
        memoryEditor.setSingleLine(false);
        memoryEditor.setMinLines(4);
        memoryEditor.setMaxLines(8);
        memoryEditor.setGravity(Gravity.TOP | Gravity.START);
        memoryEditor.setTextColor(Color.WHITE);
        memoryEditor.setTextSize(14);
        memoryEditor.setText(PersonalMemory.editorText(prefs));
        memoryEditor.setHint("[projet] FRIDAY utilise Jarvis local et conserve mes notes privées");
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
        config.addView(label("DÉTECTION DES PHRASES · silence de base (900 à 6000 ms)"));
        config.addView(label("FRIDAY laisse jusqu'à une seconde supplémentaire si la phrase "
                + "se termine sur une liaison comme « et », « de » ou « pour », "
                + "afin de ne pas couper une question pendant une courte pause."));
        silenceInput = field("2100", false);
        silenceInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        silenceInput.setText(prefs.get("silence_ms", "2100")); config.addView(silenceInput);
        config.addView(label("DIALOGUE CONTINU · relance directe 20 secondes après la voix ; "
                + "fenêtre prolongée (15 à 180 secondes) si l'option ci-dessous est activée"));
        followupInput = field("75", false);
        followupInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        followupInput.setText(prefs.get("followup_sec", "75")); config.addView(followupInput);
        followupHandsfreeEnabled = new CheckBox(this);
        followupHandsfreeEnabled.setText("Prolonger les relances sans dire Friday "
                + "(75 secondes par défaut, risque de fausses questions)");
        followupHandsfreeEnabled.setTextColor(Color.WHITE);
        followupHandsfreeEnabled.setChecked("1".equals(prefs.get("followup_handsfree", "0")));
        config.addView(followupHandsfreeEnabled);
        config.addView(label("Mode continu : après chaque réponse, FRIDAY ignore 2,5 secondes "
                + "d'écho du haut-parleur puis accepte une question directe pendant "
                + "20 secondes. Passé ce délai, dites Friday ou Jarvis. "
                + "La case ci-dessus permet de prolonger cette période, avec "
                + "davantage de risque de déclenchements involontaires."));
        config.addView(label("STOP vocal retiré : pendant que Jarvis parle, il ne peut être interrompu que par le bouton « Interrompre la réponse et écouter ». Le micro reprend automatiquement après la fin réelle de la lecture et la protection anti-écho de 2,5 secondes."));
        config.addView(label("AUDIO · microphone et haut-parleur intégrés du téléphone"));
        config.addView(label("FRIDAY n'utilise plus le micro ni le mode mains libres Bluetooth. "
                + "Les réponses Jarvis sortent par le haut-parleur du téléphone. "
                + "Vérifiez le volume de communication Android."));
        config.addView(label("VITESSE VOCALE · 100 = normale (70 à 150)"));
        ttsInput = field("100", false);
        ttsInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        ttsInput.setText(prefs.get("tts_speed", "100")); config.addView(ttsInput);
        voiceEnabled = new CheckBox(this);
        voiceEnabled.setText("Répondre à voix haute"); voiceEnabled.setTextColor(Color.WHITE);
        voiceEnabled.setChecked("1".equals(prefs.get("voice_enabled", "1")));
        config.addView(voiceEnabled);
        streamingEnabled = new CheckBox(this);
        streamingEnabled.setChecked(false);
        config.addView(label("Les réponses de Jarvis local sont lues par la synthèse vocale Android. "
                + "Une question écrite reçoit aussi une réponse à voix haute."));
        config.addView(label("ÉCOUTE ET COMPRÉHENSION · Vosk hors ligne"));
        oneShotEnabled = new CheckBox(this);
        oneShotEnabled.setText("Mode une question : arrêter automatiquement le micro après la réponse");
        oneShotEnabled.setTextColor(Color.WHITE);
        oneShotEnabled.setChecked("1".equals(prefs.get("listen_once", "0")));
        config.addView(oneShotEnabled);
        config.addView(label("Mode continu (case décochée) : le microphone reste actif "
                + "après chaque réponse. La nouvelle question peut être dite directement "
                + "pendant 20 secondes après l'anti-écho, sans appuyer sur un bouton. "
                + "Ensuite, dites « Friday » ou « Jarvis » pour une autre question. "
                + "Mode une question (case cochée) : l'écoute s'arrête après la réponse. "
                + "Pendant la voix, l'anti-écho protège contre les fausses questions."));
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
        locationShare.setChecked(false);
        config.addView(label("Position GPS utilisée sur le téléphone seulement. "
                + "Aucun partage vers un fournisseur IA distant. "
                + "Le service vocal peut nécessiter un redémarrage après les permissions."));
        placeNameEnabled = new CheckBox(this);
        placeNameEnabled.setText("Identifier la commune ou la rue à la demande (géocodeur du téléphone, peut utiliser son réseau)");
        placeNameEnabled.setTextColor(Color.WHITE);
        placeNameEnabled.setChecked("1".equals(prefs.get("address_lookup", "0")));
        config.addView(placeNameEnabled);
        config.addView(label("Aucune clé Google : ce géocodeur est une fonction d'Android, dont le fournisseur peut accéder à Internet. Désactivé par défaut."));
        config.addView(label("Aucun suivi GPS permanent : mesure à la demande "
                + "(objectif 3 m non garanti). La précision effectivement obtenue est affichée."));
        config.addView(button("◎ Tester précision GPS · cible 3 m", this::showGpsAccuracy));
        config.addView(label("DIAGNOSTIC · dernière erreur du moteur IA"));
        lastErrorLabel = label(prefs.get("last_error", "").isEmpty()
                ? "Aucune erreur enregistrée."
                : "Dernière erreur (" + prefs.get("last_error_time", "?") + ") : " + prefs.get("last_error", ""));
        config.addView(lastErrorLabel);
        config.addView(button("⧉ Copier la dernière erreur", this::copyLastError));
        config.addView(button("Diagnostic complet · micro / Vosk / Jarvis / voix", this::showVoiceDiagnostics));
        config.addView(button("Enregistrer les réglages", this::save));
        config.addView(label("PERSONNALITÉ · prompt sur plusieurs lignes · profils chiffrés"));
        config.addView(button("✧ Modifier la personnalité de F.R.I.D.A.Y.", () -> {
            configScroll.setVisibility(View.GONE);
            if (pageReady) hud.evaluateJavascript("window.fridayOpenPersonality&&window.fridayOpenPersonality();", null);
        }));
        config.addView(label("DIAGNOSTIC : envoyez une question au modèle Jarvis local sans microphone. "
                + "La réponse s'affiche et sera prononcée si la voix est activée."));
        manualQuestion = field("Ex. Bonjour, peux-tu me répondre ?", false);
        config.addView(manualQuestion);
        config.addView(button("Envoyer une question sans microphone", () -> sendQuestion(manualQuestion.getText().toString())));
        manualAnswerLabel = label("Les questions écrites fonctionnent sans microphone. "
                + "Jarvis affiche et prononce sa réponse sur le haut-parleur du téléphone.");
        manualAnswerLabel.setTextIsSelectable(true);
        config.addView(manualAnswerLabel);
        config.addView(button("Effacer les échanges vocaux locaux", this::clearVoiceHistory));
        config.addView(button("Réglages batterie Android", () -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }));
        config.addView(label("L'écoute native continue avec écran allumé ou verrouillé après un démarrage volontaire depuis l'application visible. Un redémarrage du service depuis l'application est nécessaire après tout changement de permission de localisation. La notification permet d'arrêter le micro."));
        config.addView(label("Reconnaissance vocale Vosk locale, réponses Jarvis locales et voix Android. "
                + "Aucune question ni donnée personnelle envoyée à une API IA."));

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
            prefs.put("mode", "jarvis_local");
            prefs.put("provider", "jarvis_local");
            prefs.put("memory_enabled", memoryEnabled.isChecked() ? "1" : "0");
            prefs.put("memory_share_mistral", "0");
            prefs.put("web_enabled", "0");
            prefs.put("web_mode", "off");
            prefs.put("jarvis_auto_listen_on_open",
                    autoListenOnOpen.isChecked() ? "1" : "0");
            prefs.put("jarvis_web_enabled", jarvisInternetEnabled.isChecked() ? "1" : "0");
            prefs.put("jarvis_web_every_answer",
                    jarvisWebEveryAnswerEnabled.isChecked() ? "1" : "0");
            prefs.put("jarvis_world_rss_translate_fr",
                    jarvisWorldRssTranslateFrEnabled.isChecked() ? "1" : "0");
            prefs.put("stream_enabled", "0");
            prefs.put("location_share", "0");
            saveLocalJarvisPrefs();
            prefs.put("silence_ms", bounded(silenceInput, 2100, 900, 6000));
            prefs.put("followup_sec", bounded(followupInput, 75, 15, 180));
            prefs.put("followup_handsfree", followupHandsfreeEnabled.isChecked() ? "1" : "0");
            prefs.put("barge_in_enabled", "0");
            prefs.put("bluetooth_mic_enabled", "0");
            prefs.put("tts_speed", bounded(ttsInput, 100, 70, 150));
            prefs.put("voice_enabled", voiceEnabled.isChecked() ? "1" : "0");
            prefs.put("listen_once", oneShotEnabled.isChecked() ? "1" : "0");
            prefs.put("vosk_hints_enabled", voskHintsEnabled.isChecked() ? "1" : "0");
            String hints = voskHintsEditor.getText().toString();
            if (hints.length() > 2100) throw new IllegalArgumentException("Vocabulaire trop long.");
            prefs.setSecret("vosk_hints", hints);
            prefs.put("location_enabled", locationEnabled.isChecked() ? "1" : "0");
            prefs.put("address_lookup", locationEnabled.isChecked() && placeNameEnabled.isChecked() ? "1" : "0");
            status.setText("Jarvis local exclusif. Réglages enregistrés. "
                    + LocalJarvisEngine.status(this, prefs)
                    + ". Redémarrez l'écoute après modification des permissions.");
            if (locationEnabled.isChecked() && !FridayLocation.hasPermission(this)) requestLocationAccess();
            Toast.makeText(this,"Réglages Jarvis enregistrés",Toast.LENGTH_SHORT).show();
        } catch (Exception ex) { status.setText("Enregistrement impossible : " + ex.getMessage()); }
    }
    /** Permet aussi de réactiver une référence enregistrée par la v2.1.8 sans la refaire. */
                /** Affiche le statut enregistré, pas seulement l'état visuel d'une case. */
            private void saveMemory() {
        try {
            int count = PersonalMemory.replaceFromEditor(prefs, memoryEditor.getText().toString());
            status.setText("Memory+ : " + count + " notes chiffrées localement. Aucun partage réseau.");
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

    private void saveLocalJarvisPrefs() {
        // Jarvis exclusif, même avant l'importation du premier modèle.
        // Le test et la réponse indiquent précisément si le modèle manque.
        localJarvisEnabled.setChecked(true);
        localJarvisAll.setChecked(true);
        prefs.put("local_jarvis_enabled", "1");
        prefs.put("local_jarvis_all", "1");
    }

    private void saveLocalJarvis() {
        try {
            saveLocalJarvisPrefs();
            String state = LocalJarvisEngine.status(this, prefs);
            localJarvisStatus.setText(state);
            status.setText(state);
        } catch (Exception failure) {
            localJarvisStatus.setText("Configuration locale refusée : " + failure.getMessage());
        }
    }

    private void chooseLocalJarvisModel() {
        if (!localJarvisImporting.compareAndSet(false, true)) {
            Toast.makeText(this, "Importation du modèle en cours.", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            picker.addCategory(Intent.CATEGORY_OPENABLE);
            picker.setType("*/*");
            startActivityForResult(picker, REQ_LOCAL_LLM_MODEL);
        } catch (Exception failure) {
            localJarvisImporting.set(false);
            localJarvisStatus.setText("Sélecteur de fichiers Android indisponible.");
        }
    }

    private void openLocalJarvisModels() {
        // Link to ONE actual compatible file, not a repository with mixed formats.
        Intent browser = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://huggingface.co/litert-community/gemma-3-270m-it/blob/main/gemma3-270m-it-q8.task"));
        try { startActivity(browser); }
        catch (Exception e) {
            localJarvisStatus.setText("Ouvrez Hugging Face et téléchargez gemma3-270m-it-q8.task.");
        }
    }

    private void openLocalJarvisGemma4Models() {
        // Separate runtime. Never suggest that the MediaPipe API loads .litertlm.
        Intent browser = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/blob/main/gemma-4-E2B-it-gpu.litertlm"));
        try { startActivity(browser); }
        catch (Exception e) {
            localJarvisStatus.setText("Ouvrez Hugging Face et téléchargez gemma-4-E2B-it-gpu.litertlm.");
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_LOCAL_LLM_MODEL) return;
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            localJarvisImporting.set(false);
            return;
        }
        final Uri selected = data.getData();
        final String pending = "Copie du modèle IA dans le stockage privé du téléphone…";
        localJarvisStatus.setText(pending);
        status.setText(pending);
        new Thread(() -> {
            String outcome;
            boolean imported = false;
            try {
                outcome = LocalJarvisEngine.importModel(this, selected);
                imported = true;
            } catch (Exception error) {
                outcome = "Importation impossible : " + error.getMessage()
                        + ". Le modèle précédent est conservé.";
            }
            final boolean success = imported;
            final String result = outcome;
            localJarvisImporting.set(false);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (success) {
                    localJarvisEnabled.setChecked(true);
                    localJarvisAll.setChecked(true);
                    prefs.put("local_jarvis_enabled", "1");
                    prefs.put("local_jarvis_all", "1");
                }
                localJarvisStatus.setText(result);
                status.setText(result);
                new AlertDialog.Builder(this).setTitle("Importation Jarvis local")
                        .setMessage(result).setPositiveButton("Fermer", null).show();
            });
        }, "friday-local-model-import").start();
    }

    /** Diagnostic réseau sans appel Mistral, exécuté hors thread graphique. */
    private void testJarvisInternet() { runJarvisNetworkDiagnostic(false); }
    private void testJarvisRecentNews() { runJarvisNetworkDiagnostic(true); }

    private void runJarvisNetworkDiagnostic(boolean recent) {
        if (!jarvisWebTesting.compareAndSet(false, true)) {
            status.setText("Test Internet Jarvis déjà en cours.");
            return;
        }
        prefs.put("jarvis_web_enabled", jarvisInternetEnabled.isChecked() ? "1" : "0");
        if (!JarvisInternet.enabled(prefs)) {
            jarvisWebTesting.set(false);
            status.setText("Activez d'abord la case Internet Jarvis.");
            return;
        }
        jarvisInternetStatus.setText(recent
                ? "Vérification des horodatages RSS des 7 dernières heures…"
                : "Recherche publique HTTPS en cours (aucune API Mistral)…");
        new Thread(() -> {
            String result;
            boolean ok;
            try {
                result = recent ? JarvisInternet.probeRecentNews() : JarvisInternet.probe();
                ok = true;
            } catch (Exception error) {
                result = "Test Internet échoué : "
                        + (error.getMessage() == null ? error.getClass().getSimpleName()
                        : error.getMessage());
                ok = false;
            }
            final String report = result;
            final boolean success = ok;
            runOnUiThread(() -> {
                jarvisWebTesting.set(false);
                if (isFinishing() || isDestroyed()) return;
                prefs.put("jarvis_web_last_test", success ? "recherche OK" : "échec du test");
                if (!success) {
                    prefs.put("last_error", report);
                    if (lastErrorLabel != null) lastErrorLabel.setText(report);
                }
                jarvisInternetStatus.setText(report);
                status.setText(report);
                new AlertDialog.Builder(this).setTitle(success
                        ? "Internet Jarvis opérationnel" : "Internet Jarvis indisponible")
                        .setMessage(report).setPositiveButton("Fermer", null).show();
            });
        }, "friday-jarvis-internet-test").start();
    }

    private void testLocalJarvis() {
        if (!localJarvisTesting.compareAndSet(false, true)) {
            Toast.makeText(this, "Test local déjà en cours.", Toast.LENGTH_LONG).show();
            return;
        }
        if (!LocalJarvisEngine.ready(this)) {
            localJarvisTesting.set(false);
            String problem = "Test Jarvis local impossible : aucun fichier de modèle compatible "
                    + "pour le moteur sélectionné. Ouvrez Réglages → Jarvis local → "
                    + "Importer un modèle .task ou .litertlm. "
                    + LocalJarvisEngine.status(this, prefs);
            localJarvisStatus.setText(problem);
            status.setText(problem);
            new AlertDialog.Builder(this).setTitle("Modèle IA local manquant")
                    .setMessage(problem).setPositiveButton("Fermer", null).show();
            return;
        }
        try { saveLocalJarvisPrefs(); }
        catch (Exception failure) {
            localJarvisTesting.set(false);
            String problem = "Test Jarvis local impossible : " + failure.getMessage();
            localJarvisStatus.setText(problem);
            status.setText(problem);
            new AlertDialog.Builder(this).setTitle("Configuration Jarvis local")
                    .setMessage(problem).setPositiveButton("Fermer", null).show();
            return;
        }
        localJarvisStatus.setText("Jarvis local : vérification réelle du moteur et du modèle… "
                + LocalJarvisEngine.status(this, prefs));
        status.setText("Test du vrai modèle Jarvis local sans microphone et sans connexion Mistral…");
        new Thread(() -> {
            String answer;
            boolean succeeded = false;
            try {
                answer = LocalJarvisEngine.answer(getApplicationContext(), prefs,
                        "Jarvis, présente ton rôle en deux phrases en français.", null);
                if (answer == null || answer.trim().isEmpty())
                    throw new IOException("Le modèle n'a généré aucune réponse.");
                answer = "TEST IA LOCALE RÉUSSI — réponse générée sans Mistral.\n"
                        + LocalJarvisEngine.status(getApplicationContext(), prefs) + "\n\n"
                        + (answer.length() > 500 ? answer.substring(0, 500) : answer);
                succeeded = true;
            } catch (OutOfMemoryError memory) {
                answer = "TEST IA LOCALE ÉCHOUÉ — mémoire RAM insuffisante. "
                        + "Importez un modèle plus petit et fermez les autres applications.";
            } catch (Exception | LinkageError failure) {
                String reason = failure.getMessage();
                answer = "TEST IA LOCALE ÉCHOUÉ — "
                        + failure.getClass().getSimpleName() + " : "
                        + (reason == null ? "échec sans détail du moteur" : reason)
                        + "\n" + LocalJarvisEngine.status(getApplicationContext(), prefs)
                        + "\nVérifiez le format du modèle, la RAM et la compatibilité GPU/CPU.";
            }
            final String result = answer;
            final boolean ok = succeeded;
            localJarvisTesting.set(false);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                localJarvisStatus.setText(result);
                status.setText(result);
                if (!ok) {
                    prefs.put("last_error", result);
                    prefs.put("last_error_time", new java.text.SimpleDateFormat(
                            "dd/MM HH:mm:ss", java.util.Locale.FRANCE).format(new java.util.Date()));
                    if (lastErrorLabel != null) lastErrorLabel.setText("Dernière erreur : " + result);
                }
                new AlertDialog.Builder(this)
                        .setTitle(ok ? "Jarvis local : modèle confirmé" : "Jarvis local : diagnostic d'échec")
                        .setMessage(result).setPositiveButton("Fermer", null).show();
            });
        }, "friday-local-llm-test").start();
    }

    /** Test réseau réel : résultat toujours lisible dans les réglages et en fenêtre. */
    private void enableAndTestWeb() {
        if (!webTestRunning.compareAndSet(false, true)) {
            Toast.makeText(this, "Un test Web est déjà en cours.", Toast.LENGTH_LONG).show();
            return;
        }
        webEnabled.setChecked(true);
        prefs.put("web_enabled", "1");
        prefs.put("web_mode", webAlways.isChecked() ? "always" : "auto");
        prefs.put("web_verified", "0");
        String pending = "Test Web Mistral EN COURS : vérification du modèle, de la clé "
                + "et de l'outil web_search. Un appel peut durer plusieurs dizaines de secondes.";
        webDiagnosticLabel.setText(pending);
        status.setText(pending);
        prefs.put("web_last_test_status", pending);
        AlertDialog progress = new AlertDialog.Builder(this)
                .setTitle("FRIDAY · diagnostic Internet")
                .setMessage(pending)
                .setPositiveButton("Masquer", (dialog, which) -> { })
                .show();
        new Thread(() -> {
            String result;
            try {
                result = NativeAI.testWeb(prefs);
            } catch (Exception error) {
                result = NativeAI.explainWebTestFailure(error);
            }
            final String detail = result;
            final boolean verified = detail.startsWith("Recherche Web Mistral opérationnelle");
            prefs.put("web_verified", verified ? "1" : "0");
            prefs.put("web_last_test_status", detail);
            webTestRunning.set(false);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                webDiagnosticLabel.setText(detail);
                status.setText(detail);
                if (progress.isShowing()) {
                    progress.setTitle(verified ? "Recherche Internet opérationnelle" : "Diagnostic Internet FRIDAY");
                    progress.setMessage(detail);
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle(verified ? "Recherche Internet opérationnelle" : "Diagnostic Internet FRIDAY")
                            .setMessage(detail)
                            .setPositiveButton("Fermer", (dialog, which) -> { })
                            .show();
                }
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
                        prefs.get("mistral_model", "ministral-8b-latest"))), (dialog, index) -> {
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

    /**
     * Diagnostic local partageable sans historique de parole, sans coordonnées
     * et sans valeurs de clés API. L'état du microphone se lit sur le service.
     */
    private void showVoiceDiagnostics() {
        String report = "FRIDAY " + FridayVersionGuard.thisVersion(this)
                + "\nService actif : " + (FridayService.isActive() ? "oui" : "non")
                + "\nAutorisation micro : "
                + (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                   == PackageManager.PERMISSION_GRANTED ? "accordée" : "manquante")
                + "\nMicro Android rendu muet : " + (FridayService.isMicSilenced() ? "oui" : "non")
                + "\nÉtat de l'écoute : " + FridayService.voiceReadiness()
                + "\nSignal du microphone : " + FridayService.microphoneSignal()
                + "\nDernière reconnaissance Vosk : " + FridayService.recognitionDiagnostic()
                + "\nSilence de base : " + prefs.get("silence_ms", "2100")
                        + " ms (adaptatif pour les phrases incomplètes)"
                + "\nInterruption STOP : " + FridayService.interruptionDiagnostics()
                + "\nTTS Android : " + FridayService.lastTtsStatus()
                + "\nAudio : microphone intégré et haut-parleur du téléphone (Bluetooth désactivé)"
                + "\nMoteur IA : Jarvis local uniquement"
                + "\nMode vocal : " + ("1".equals(prefs.get("listen_once", "0"))
                        ? "Une question (arrêt après réponse)"
                        : "Continu (relance directe pendant 20 s après anti-écho)")
                + "\nInternet Jarvis : " + (JarvisInternet.enabled(prefs)
                        ? "activé (requêtes HTTPS publiques)" : "désactivé")
                + "\nRecherche Web pour les questions générales : "
                + ("1".equals(prefs.get("jarvis_web_every_answer", "1"))
                        ? "activée" : "désactivée (mode ciblé)")
                + "\nDernier test Internet : " + prefs.get("jarvis_web_last_test", "non effectué")
                + "\nDernière erreur à : " + prefs.get("last_error_time", "aucune")
                + "\n\nLe niveau micro est un niveau PCM et ne garantit pas qu'une parole a été comprise."
                + "\nAucune clé, aucun GPS, aucun contenu de conversation dans ce diagnostic.";
        new AlertDialog.Builder(this)
                .setTitle("FRIDAY · état des composants")
                .setMessage(report)
                .setPositiveButton("Fermer", null)
                .setNeutralButton("Copier le diagnostic", (dialog, which) -> {
                    android.content.ClipboardManager manager =
                            (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    if (manager != null) {
                        manager.setPrimaryClip(android.content.ClipData.newPlainText(
                                "friday-diagnostic-sans-secrets", report));
                        Toast.makeText(this, "Diagnostic copié sans informations personnelles",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .show();
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
        // Le bouton principal doit éprouver le VRAI modèle installé.
        // Le précédent test de l'heure pouvait répondre sans aucun LLM
        // via PythonBrain et ne validait donc pas Jarvis local.
        testLocalJarvis();
    }
    /** Questions écrites : Jarvis local exclusif, sans microphone requis. */
    private void sendQuestion(String question) {
        String cleaned = question == null ? "" : VoiceText.withoutWakeWord(question.trim());
        if (cleaned.isEmpty()) {
            status.setText("Saisissez une question pour le test."); return;
        }
        if (cleaned.length() > 5000) {
            status.setText("Question trop longue (5 000 caractères maximum)."); return;
        }
        if (!typedQuestionRunning.compareAndSet(false, true)) {
            status.setText("Une question écrite est déjà en cours. Attendez sa réponse."); return;
        }
        String engineName = "Jarvis local";
        status.setText("Question écrite envoyée à " + engineName + " (microphone non utilisé).");
        if (manualAnswerLabel != null)
            manualAnswerLabel.setText("Traitement de la question écrite par " + engineName + "…");
        final String request = cleaned;
        new Thread(() -> {
            String answer = null;
            String speechAnswer = null;
            String fault = null;
            boolean persisted = false;
            try {
                if (MemoryIntent.kind(request) != MemoryIntent.NONE)
                    answer = PersonalMemory.command(prefs, request);
                if (answer == null && DialogueMemoryPolicy.asksMemoryStatus(request))
                    answer = DialogueContext.memoryStatus(
                            getApplicationContext(), prefs, request);
                if (answer == null)
                    answer = JarvisAndroidTools.answer(
                            getApplicationContext(), prefs, request);
                // Une question « Internet fonctionne ? » exige une vraie
                // vérification réseau : aucun historique ni GPS transmis.
                if (answer == null && WebSearch.requestsLiveConnectivityCheck(request))
                    answer = JarvisInternet.liveConnectionDiagnostic(
                            prefs, new NativeAI.CancelToken());
                // Les capacités intrinsèques de Jarvis restent locales.
                if (answer == null)
                    answer = WebSearch.assistantAnswer(request, JarvisInternet.enabled(prefs));
                if (answer == null) {
                    if (JarvisInternet.shouldResearch(prefs, request)) {
                        JarvisInternet.Result researched = JarvisInternet.answer(
                                getApplicationContext(), prefs, request, new NativeAI.CancelToken());
                        answer = researched.text;
                        speechAnswer = researched.speech;
                    } else {
                        answer = LocalJarvisEngine.answer(getApplicationContext(), prefs, request,
                                new NativeAI.CancelToken());
                    }
                }
                if (answer == null || answer.trim().isEmpty())
                    throw new IOException("Le moteur sélectionné a renvoyé une réponse vide.");
                try {
                    PrivateConversationHistory.append(getApplicationContext(), prefs, "user", request);
                    PrivateConversationHistory.append(getApplicationContext(), prefs, "assistant", answer);
                    persisted = true;
                } catch (Exception privateStorage) {
                    android.util.Log.w("FRIDAY_PRIVACY",
                            "Historique chiffré indisponible pour la question écrite",
                            privateStorage);
                }
            } catch (Exception ex) {
                String reason = ex.getMessage();
                fault = "Erreur " + engineName + " : "
                        + (reason == null || reason.trim().isEmpty()
                        ? ex.getClass().getSimpleName() : reason);
            } catch (LinkageError | OutOfMemoryError fatal) {
                fault = "Erreur " + engineName + " : "
                        + fatal.getClass().getSimpleName()
                        + ". Vérifiez le modèle local et la mémoire disponible.";
            }
            final String result = answer;
            final String spokenResult = speechAnswer == null ? answer : speechAnswer;
            final String error = fault;
            final boolean historySaved = persisted;
            runOnUiThread(() -> {
                typedQuestionRunning.set(false);
                if (error != null) {
                    prefs.put("last_error", error);
                    prefs.put("last_error_time", new java.text.SimpleDateFormat(
                            "dd/MM HH:mm:ss", java.util.Locale.FRANCE).format(new java.util.Date()));
                    if (lastErrorLabel != null) lastErrorLabel.setText(error);
                    if (manualAnswerLabel != null) manualAnswerLabel.setText(error);
                    status.setText(error);
                    if (pageReady && hud != null)
                        hud.evaluateJavascript("window.fridayNativeReceive&&window.fridayNativeReceive('error',"
                                + org.json.JSONObject.quote(error) + ");", null);
                } else {
                    if (manualAnswerLabel != null) manualAnswerLabel.setText(result);
                    status.setText("Réponse Jarvis locale reçue. Lecture vocale Android…");
                    if (historySaved) replayVoiceHistory();
                    else if (pageReady && hud != null)
                        hud.evaluateJavascript("window.fridayTypedReply&&window.fridayTypedReply("
                                + org.json.JSONObject.quote(request) + ","
                                + org.json.JSONObject.quote(result) + ");", null);
                    if ("1".equals(prefs.get("voice_enabled", "1")))
                        playWebVoice(spokenResult);
                }
            });
        }, "friday-typed-request-no-mic").start();
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
            PhoneAudioRoute.usePhoneSpeaker(this);
            testTts.setAudioAttributes(PhoneAudioRoute.speechAttributes());
            final String id = "test_voix_friday";
            testTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String uid) {
                    runOnUiThread(() -> status.setText("La lecture audio a démarré. Vérifiez le volume du haut-parleur."));
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
    private void installModel() { installModel(false); }

    /** Le téléchargement réseau est déclenché seulement par une action explicite. */
    private void installModel(boolean startWhenReady) {
        if (ModelInstaller.installed(this)) {
            status.setText("Modèle français Vosk installé et vérifié.");
            if (startWhenReady) startListening();
            return;
        }
        if (startWhenReady) startAfterVoskDownload = true;
        if (!voskDownloadRunning.compareAndSet(false, true)) {
            status.setText("Téléchargement Vosk en cours. L'écoute démarrera après installation si demandé.");
            return;
        }
        status.setText("Téléchargement du modèle Vosk français (~41 Mo) depuis Alphacephei…");
        new Thread(() -> {
            String problem = null;
            try {
                ModelInstaller.download(getApplicationContext(),
                        update -> runOnUiThread(() -> status.setText(update)));
                if (!ModelInstaller.installed(getApplicationContext()))
                    throw new java.io.IOException("Le modèle Vosk reste absent après téléchargement.");
            } catch (Exception failure) {
                problem = failure.getMessage();
                if (problem == null || problem.isEmpty())
                    problem = failure.getClass().getSimpleName();
            }
            final String error = problem;
            voskDownloadRunning.set(false);
            runOnUiThread(() -> {
                boolean launch = startAfterVoskDownload;
                startAfterVoskDownload = false;
                if (error == null) {
                    status.setText("Modèle Vosk français prêt. "
                            + (launch ? "Activation du microphone…" : "Appuyez sur Écoute pour parler."));
                    if (launch) startListening();
                } else {
                    status.setText("Micro arrêté : téléchargement Vosk impossible — " + error
                            + ". Vérifiez Internet, l'espace disponible, puis réessayez.");
                    configScroll.setVisibility(View.VISIBLE);
                }
            });
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
    private void startListening() { startListening(false); }

    /** Invoked only from a visible Activity. Never at boot or in background. */
    private void startListening(boolean automatically) {
        if (!ModelInstaller.installed(this)) {
            status.setText("Micro arrêté : modèle vocal Vosk français manquant.");
            new AlertDialog.Builder(this)
                    .setTitle("Préparer l'écoute JARVIS")
                    .setMessage("Cette installation ne contient pas encore le modèle vocal français "
                            + "Vosk (~41 Mo). Il est distinct du modèle IA de Jarvis. "
                            + "Télécharger Vosk depuis Alphacephei, puis démarrer automatiquement "
                            + "l'écoute ? Le fichier reste sur le téléphone.")
                    .setPositiveButton("Télécharger et activer",
                            (dialog, which) -> installModel(true))
                    .setNeutralButton("Réglages",
                            (dialog, which) -> configScroll.setVisibility(View.VISIBLE))
                    .setNegativeButton("Annuler", null)
                    .show();
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            promptStartAfterPermission = true;
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC); return;
        }
        if (FridayService.isActive()) {
            status.setText("Le microphone natif est déjà actif, même écran verrouillé. Stoppez pour appliquer de nouveaux réglages.");
            return;
        }
        // Le GPS est facultatif. Une permission de localisation manquante
        // ne doit jamais bloquer la reconnaissance vocale ni l'IA locale.
        if ("1".equals(prefs.get("location_enabled", "0"))
                && !FridayLocation.hasPermission(this))
            status.setText("Écoute JARVIS sans GPS : localisation non autorisée.");
        // POST_NOTIFICATIONS reste facultatif pour un service microphone.
        // Ne pas afficher une deuxième boîte de dialogue en pleine activation,
        // y compris après l'autorisation RECORD_AUDIO : l'activité doit rester
        // visible au moment du démarrage du service de premier plan.
        java.util.List<FridayVersionGuard.Installed> others = FridayVersionGuard.installed(this);
        if (!others.isEmpty() && !automatically) {
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
        // Ne pas bloquer le démarrage automatique par une fenêtre modale :
        // Android n'indique pas si une autre édition utilise réellement le micro.
        if (automatically && !others.isEmpty())
            status.setText("Démarrage automatique : d'autres éditions FRIDAY sont installées. "
                    + "Si Android rend le micro muet, forcez l'arrêt des anciennes éditions.");
        startServiceUnchecked();
    }
    private void startServiceUnchecked() {
        try {
            Intent i = new Intent(this, FridayService.class).setAction(FridayService.ACTION_START);
            startForegroundService(i);
            status.setText("JARVIS : écoute en cours d'activation · "
                    + FridayVersionGuard.thisVersion(this)
                    + (LocalJarvisEngine.ready(this) ? " · IA locale installée"
                       : " · modèle IA local à importer pour les réponses"));
        } catch (Exception e) { status.setText("Démarrage refusé : " + e.getMessage()); }
    }
    private void interruptSpeaking() {
        hudCloneSpeaking.set(false);
        if (webVoice != null) { try { webVoice.stop(); } catch (Exception ignored) { } }
        if (!FridayService.isActive()) {
            status.setText("Micro arrêté : tentative d'activation de l'écoute.");
            startListening();
            return;
        }
        startService(new Intent(this, FridayService.class).setAction(FridayService.ACTION_INTERRUPT));
        status.setText("Réponse interrompue, micro toujours actif. Posez votre question.");
    }
    private void stopListening() {
        manuallyStoppedListening = true;
        startService(new Intent(this, FridayService.class).setAction(FridayService.ACTION_STOP));
        status.setText("JARVIS : écoute arrêtée à votre demande. "
                + "Elle ne redémarrera pas toute seule pendant cette ouverture.");
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
                if (kind.equals("status") || kind.equals("error") || kind.equals("tts")
                        || kind.equals("mic_conflict") || kind.equals("interrupted")
                        || kind.equals("voice_state") || kind.equals("asr_diag")) status.setText(text);
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
        activityVisible = true;
        cockpitVisible = true;
        replayVoiceHistory();
        boolean autoStartScheduled = false;
        if (!startupListenAttempted) {
            boolean shouldAutoStart = AutoListenPolicy.shouldStart(
                    prefs.get("jarvis_auto_listen_on_open", "1"),
                    startupListenAttempted, manuallyStoppedListening,
                    FridayService.isActive());
            startupListenAttempted = true;
            if (shouldAutoStart) {
                autoStartScheduled = true;
                status.setText("JARVIS se prépare : activation automatique du micro…");
                // L'activité est désormais visible. Un seul démarrage
                // et aucune relance automatique après un appui sur Stop.
                status.post(() -> {
                    if (!activityVisible) {
                        // L'utilisateur a quitté JARVIS avant l'activation.
                        // Réessayer seulement lors de la prochaine ouverture visible.
                        startupListenAttempted = false;
                        return;
                    }
                    if (!isFinishing() && !isDestroyed()
                            && !manuallyStoppedListening
                            && AutoListenPolicy.enabled(
                                    prefs.get("jarvis_auto_listen_on_open", "1"))
                            && !FridayService.isActive())
                        startListening(true);
                });
            }
        }
        // Ne pas écraser « activation automatique en cours » par
        // « Micro arrêté » avant l'exécution du startForegroundService.
        if (status != null && !autoStartScheduled) {
            if (FridayService.isMicSilenced()) status.setText("Microphone momentanément rendu muet par Android : vérifiez les autres applications.");
            else if (FridayService.isActive()) status.setText("Écoute native active · " + FridayVersionGuard.thisVersion(this));
            else if (voskDownloadRunning.get())
                status.setText("Micro arrêté : téléchargement du modèle Vosk en cours.");
            else if (!ModelInstaller.installed(this))
                status.setText("Micro arrêté : modèle Vosk français manquant. Appuyez sur Écoute pour l'installer.");
            else if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
                status.setText("Micro arrêté : autorisation Android du microphone manquante. Appuyez sur Écoute.");
            else status.setText("Micro arrêté. Appuyez sur Écoute pour démarrer le service vocal.");
        }
    }
    @Override protected void onPause() {
        activityVisible = false;
        cockpitVisible = false;
        super.onPause();
    }

    /** Photo pleine définition dans le cache privé, partagée ponctuellement avec la caméra. */
            /** Évite le chargement de plusieurs dizaines de Mo depuis le capteur photo. */
        private static String safeError(Exception ex) {
        String message = ex.getMessage();
        return message == null || message.trim().isEmpty() ? ex.getClass().getSimpleName() : message;
    }

    /** Voix Android pour toutes les réponses écrites, même sans service micro. */
    private void playWebVoice(String text) {
        if (text == null || text.trim().isEmpty()) return;
        final String spoken = text;
        playWebVoiceAndroid(spoken);
    }
    private void speakVoiceChunks(String spoken) {
        if (webVoice == null || !webVoiceReady) return;
        try {
            PhoneAudioRoute.usePhoneSpeaker(this);
            webVoice.setAudioAttributes(PhoneAudioRoute.speechAttributes());
            int rate = Integer.parseInt(prefs.get("tts_speed", "100"));
            webVoice.setSpeechRate(Math.max(0.7f, Math.min(1.5f, rate / 100f)));
            java.util.List<String> parts = VoiceText.ttsChunks(spoken, 280);
            for (int i=0; i<parts.size(); i++) {
                int result = webVoice.speak(parts.get(i),
                        i == 0 ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD,
                        new Bundle(), "friday_jarvis_ui_" + System.nanoTime() + "_" + i);
                if (result != TextToSpeech.SUCCESS) {
                    status.setText("Échec synthèse vocale Android (code " + result + "). "
                            + "Vérifiez la voix française et la sortie du casque.");
                    break;
                }
            }
        } catch (Exception ex) {
            status.setText("Voix Android indisponible : " + ex.getClass().getSimpleName());
        }
    }
    private void playWebVoiceAndroid(String spoken) {
        if (webVoice != null && webVoiceReady) {
            PhoneAudioRoute.usePhoneSpeaker(this);
            speakVoiceChunks(spoken);
            return;
        }
        if (webVoice != null) return;
        webVoice = new TextToSpeech(getApplicationContext(), result -> runOnUiThread(() -> {
            webVoiceReady = result == TextToSpeech.SUCCESS && webVoice != null &&
                webVoice.setLanguage(Locale.FRENCH) >= TextToSpeech.LANG_AVAILABLE;
            if (webVoiceReady) speakVoiceChunks(spoken);
            else status.setText("Voix française Android indisponible. "
                    + "Configurez le moteur de synthèse vocale dans Android.");
        }));
    }
    private void refreshLocation() {
        if (!"1".equals(prefs.get("location_enabled", "0"))) {
            cachedLocation = "GPS désactivé dans les réglages de JARVIS.";
            return;
        }
        if (!FridayLocation.hasPermission(this)) {
            cachedLocation = "Autorisation Android de position manquante. "
                    + "Ouvrez Tester GPS pour la demander.";
            return;
        }
        if (!FridayLocation.providerEnabled(this)) {
            cachedLocation = "Localisation Android désactivée. "
                    + "Activez Localisation dans le téléphone.";
            return;
        }
        cachedLocation = "Acquisition GPS en cours (maximum 16 s)…";
        new Thread(() -> {
            try {
                FridayLocation.Fix fix = FridayLocation.locate(getApplicationContext());
                cachedLocation = fix == null
                        ? "Aucun relevé récent reçu d'Android après 16 secondes. "
                          + "Vérifiez la localisation précise et essayez à l'extérieur."
                        : fix.forUser();
            } catch (SecurityException denied) {
                cachedLocation = "Autorisation GPS Android refusée : "
                        + "vérifiez les permissions de JARVIS.";
            } catch (Exception unavailable) {
                cachedLocation = "Acquisition GPS indisponible ("
                        + unavailable.getClass().getSimpleName()
                        + "). Réessayez avec le bouton Tester GPS.";
            }
            runOnUiThread(() -> {
                if (pageReady && hud != null)
                    hud.evaluateJavascript("window.fridayLocationSync&&window.fridayLocationSync();", null);
            });
        }, "friday-position").start();
    }
    /** Mode Jarvis Web : l'activation ne constitue pas une preuve de connexion. */
    private String webTelemetry() {
        if (!JarvisInternet.enabled(prefs)) return "Désactivée";
        String check = prefs.get("jarvis_web_last_test", "");
        return check.isEmpty() ? "Jarvis Web activé · test non effectué"
                : "Jarvis Web activé · " + check;
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
        @JavascriptInterface public String voiceReadiness() { return FridayService.voiceReadiness(); }
        @JavascriptInterface public String voiceInterruptTelemetry() {
            return "STOP vocal retiré · interruption uniquement avec le bouton";
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
        cockpitVisible = false;
        if (events != null) unregisterReceiver(events);
        if (testTts != null) { try { testTts.shutdown(); } catch (Exception ignored) {} testTts = null; }
        if (webVoice != null) { try { webVoice.shutdown(); } catch (Exception ignored) {} webVoice = null; }
        if (!FridayService.isActive()) PhoneAudioRoute.release(this);
        if (hud != null) { hud.removeJavascriptInterface("FridayAndroid"); hud.destroy(); }
        super.onDestroy();
        // Le service micro reste actif après fermeture du cockpit, jusqu'à arrêt explicite.
    }
}
