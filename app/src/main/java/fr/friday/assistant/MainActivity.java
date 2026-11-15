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
    private TextView openaiKeyState;
    private Spinner providerSpinner;
    private EditText openaiUrl, openaiModel, openaiKey;
    private CheckBox voiceEnabled, locationEnabled, locationShare, placeNameEnabled;
    private CheckBox memoryEnabled;
    private TextView voiceSampleStatus;
    private volatile boolean voiceRecording = false;
    private volatile boolean voicePreview = false;
    private final AtomicBoolean hudCloneSpeaking = new AtomicBoolean(false);
    private boolean recordAfterMicPermission = false;
    private EditText memoryEditor;
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
    private volatile String cachedLocation = "";
    private File captureFile;
    private TextToSpeech webVoice;
    private volatile boolean webVoiceReady = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(3, 16, 26));
        getWindow().setNavigationBarColor(Color.rgb(3, 16, 26));
        prefs = new SecurePrefs(this);
        // Migration des modèles retirés : « deepseek-chat » n'existe plus (API 2026).
        String storedModel = prefs.get("openai_model", "");
        if (storedModel.isEmpty()
                || storedModel.equals("deepseek-chat")
                || storedModel.equals("deepseek-reasoner")
                || storedModel.startsWith("deepseek-v3")) {
            prefs.put("openai_model", "deepseek-flash");
        }
        // Migration : supprimer l'ancienne clé Agnès stockée sur les installations précédentes.
        try { prefs.setSecret("agnes", ""); prefs.setSecret("mistral", ""); prefs.put("mode", "deepseek"); prefs.put("agnes_model", ""); } catch (Exception ignored) {}
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
        config.addView(label("MOTEUR IA · DEEPSEEK (unique fournisseur)"));
        openaiUrl = field("https://api.deepseek.com", false);
        openaiUrl.setText(prefs.get("openai_url", "https://api.deepseek.com"));
        openaiModel = field("deepseek-flash", false);
        openaiModel.setText(prefs.get("openai_model", "deepseek-flash"));
        config.addView(label("Modèle DeepSeek"));
        config.addView(openaiModel);
        config.addView(label("Clé API DeepSeek (laissez vide pour conserver)"));
        openaiKey = field(prefs.hasSecret("openai") ? "Clé déjà enregistrée" : "Clé manquante", true);
        config.addView(openaiKey);
        openaiKeyState = label(prefs.hasSecret("openai")
                ? "✔ Clé DeepSeek : enregistrée"
                : "✘ Clé DeepSeek : absente — collez la clé puis appuyez sur « Enregistrer la clé »");
        config.addView(openaiKeyState);
        config.addView(button("💾 Enregistrer la clé DeepSeek", this::saveKeyOnly));
        config.addView(button("🌐 Se connecter / créer ma clé sur platform.deepseek.com", this::openDeepSeekPage));
        config.addView(button("🔍 Détecter les modèles disponibles", this::detectModels));

        config.addView(label("MÉMOIRE PERSONNELLE · uniquement sur demande explicite"));
        memoryEnabled = new CheckBox(this);
        memoryEnabled.setText("Activer ma mémoire chiffrée (les notes seront transmises à DeepSeek avec les questions)");
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
        config.addView(label("LOCALISATION · permission Android facultative"));
        locationEnabled = new CheckBox(this);
        locationEnabled.setText("Activer la position uniquement sur demande vocale");
        locationEnabled.setTextColor(Color.WHITE);
        locationEnabled.setChecked("1".equals(prefs.get("location_enabled", "0")));
        config.addView(locationEnabled);
        locationShare = new CheckBox(this);
        locationShare.setText("Autoriser l'envoi ponctuel de mes coordonnées GPS à DeepSeek pour les questions locales");
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
            "La précision effectivement mesurée est affichée. Si le partage IA est autorisé, les coordonnées précises de cette question sont transmises à l’API DeepSeek."));
        config.addView(button("◎ Tester précision GPS · cible 3 m", this::showGpsAccuracy));
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
        config.addView(label("La reconnaissance vocale est locale ; seule la question transcrite est envoyée à l’API DeepSeek configurée. Les clés API natives sont chiffrées Android Keystore."));

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
            prefs.put("mode", "deepseek");
            prefs.put("provider", "openai");
            String deepseekUrl = openaiUrl.getText().toString().trim();
            prefs.put("openai_url", deepseekUrl.isEmpty() ? "https://api.deepseek.com" : deepseekUrl);
            prefs.put("openai_model", openaiModel.getText().toString().trim());
            String openaiGiven = openaiKey.getText().toString().trim();
            if (!openaiGiven.isEmpty()) prefs.setSecret("openai", openaiGiven);
            openaiKey.setText("");
            prefs.put("memory_enabled", memoryEnabled.isChecked() ? "1" : "0");
            prefs.put("silence_ms", bounded(silenceInput, 2100, 900, 6000));
            prefs.put("followup_sec", bounded(followupInput, 75, 15, 180));
            prefs.put("barge_in_enabled", bargeEnabled.isChecked() ? "1" : "0");
            prefs.put("tts_speed", bounded(ttsInput, 100, 70, 150));
            prefs.put("voice_enabled", voiceEnabled.isChecked() ? "1" : "0");
            prefs.put("location_enabled", locationEnabled.isChecked() ? "1" : "0");
            prefs.put("location_share", locationEnabled.isChecked() && locationShare.isChecked() ? "1" : "0");
            prefs.put("address_lookup", locationEnabled.isChecked() && placeNameEnabled.isChecked() ? "1" : "0");
            String keyInfo = prefs.hasSecret("openai")
                    ? (SecurePrefs.LAST_ERROR.isEmpty() ? "Clé DeepSeek enregistrée." : "Clé enregistrée. ⚠ " + SecurePrefs.LAST_ERROR)
                    : "⚠ Aucune clé DeepSeek enregistrée : les réponses seront refusées.";
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
    /** Enregistre la clé telle qu'elle est saisie, sans toucher au reste des réglages. */
    private void saveKeyOnly() {
        String typed = openaiKey == null ? "" : openaiKey.getText().toString().trim();
        if (typed.isEmpty()) {
            openaiKeyState.setText(prefs.hasSecret("openai")
                    ? "✔ Clé déjà enregistrée (champ vide : la clé existante est conservée)"
                    : "✘ Aucune clé : collez la clé DeepSeek (elle commence par « sk- »)");
            return;
        }
        prefs.setSecret("openai", typed);
        openaiKey.setText("");
        boolean ok = prefs.hasSecret("openai");
        openaiKeyState.setText(ok
                ? "✔ Clé DeepSeek enregistrée" + (SecurePrefs.LAST_ERROR.isEmpty() ? "" : " — ⚠ " + SecurePrefs.LAST_ERROR)
                : "✘ Échec de l'enregistrement de la clé");
        status.setText(ok ? "Clé DeepSeek enregistrée. Vous pouvez détecter les modèles." : "Clé non enregistrée.");
        Toast.makeText(this, ok ? "Clé DeepSeek enregistrée" : "Clé non enregistrée", Toast.LENGTH_SHORT).show();
    }

    private void detectModels() {
        // La clé saisie mais non encore enregistrée est prise en compte immédiatement.
        if (openaiKey != null) {
            String typed = openaiKey.getText().toString().trim();
            if (!typed.isEmpty()) { prefs.setSecret("openai", typed); openaiKey.setText(""); }
        }
        final String key = prefs.secret("openai");
        if (key.isEmpty()) { status.setText("Enregistrez d'abord la clé DeepSeek."); return; }
        status.setText("Interrogation de la liste des modèles…");
        final String url = prefs.get("openai_url", "https://api.deepseek.com");
        new Thread(() -> {
            try {
                java.util.List<String> models = NativeAI.listModels(url, key);
                runOnUiThread(() -> {
                    if (models.isEmpty()) {
                        status.setText("Aucun modèle renvoyé par " + url + "/models");
                        return;
                    }
                    StringBuilder sb = new StringBuilder();
                    for (String m : models) { if (sb.length() > 0) sb.append(", "); sb.append(m); }
                    prefs.put("available_models", String.join(",", models));
                    status.setText("Modèles disponibles : " + sb);
                    android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    if (cm != null) cm.setPrimaryClip(android.content.ClipData.newPlainText("models", sb.toString()));
                    Toast.makeText(this, "Liste copiée dans le presse-papiers", Toast.LENGTH_SHORT).show();
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
                "[" + when + "] " + err + "\nmodèle=" + prefs.get("openai_model", "?")
                + " url=" + prefs.get("openai_url", "?")
                + " clé=" + (prefs.hasSecret("openai") ? "présente" : "absente");
        android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (cm != null) cm.setPrimaryClip(android.content.ClipData.newPlainText("friday-error", payload));
        Toast.makeText(this, "Erreur copiée dans le presse-papiers", Toast.LENGTH_SHORT).show();
    }

    private void openDeepSeekPage() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://platform.deepseek.com/api_keys")));
            status.setText("Page DeepSeek ouverte : créez ou copiez votre clé, puis collez-la ici.");
        } catch (Exception e) {
            status.setText("Aucun navigateur disponible : ouvrez https://platform.deepseek.com/api_keys");
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
                    if (webVoice != null) { try { webVoice.stop(); } catch (Exception ignored) { } }
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
        @JavascriptInterface public boolean isMicSilenced() { return FridayService.isMicSilenced(); }
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
