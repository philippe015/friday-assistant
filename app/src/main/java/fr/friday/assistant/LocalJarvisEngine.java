package fr.friday.assistant;

import android.content.Context;
import android.net.Uri;
import android.os.StatFs;
import android.database.Cursor;
import android.provider.OpenableColumns;
import com.google.mediapipe.tasks.genai.llminference.LlmInference;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.json.JSONObject;

/**
 * On-device LLM (MediaPipe GenAI, Android arm64).
 *
 * No LAN/desktop/server endpoint. Model weights are copied once from an Android
 * document-provider URI to this app's private files. No user-data sync.
 * Different from OpenJarvis's Python/Rust runtime: imports the agentic idea,
 * not its unsupported desktop code.
 */
public final class LocalJarvisEngine {
    private LocalJarvisEngine() { }
    private static final ReentrantLock LOCK = new ReentrantLock();
    private static LlmInference cached;
    private static String cachedModelPath;
    private static final long MAX_MODEL_BYTES = 4_500_000_000L;
    private static final long MIN_MODEL_BYTES = 1_000_000L;

    public static File modelFile(Context context) {
        File dir = new File(context.getFilesDir(), "local-jarvis");
        return new File(dir, "model.task");
    }

    public static boolean ready(Context context) {
        File file = modelFile(context);
        return file.isFile() && file.length() >= MIN_MODEL_BYTES;
    }

    /** Call from a background worker; accepts .task/.bin prepared for MediaPipe, not GGUF. */
    public static String importModel(Context context, Uri uri) throws Exception {
        if (uri == null || !"content".equals(uri.getScheme()))
            throw new IOException("Sélectionnez un modèle via le sélecteur de documents Android.");
        String display = null;
        try (Cursor cursor = context.getContentResolver().query(
                uri, new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE},
                null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) display = cursor.getString(index);
            }
        }
        if (display == null || !(display.toLowerCase(java.util.Locale.ROOT).endsWith(".task")
                || display.toLowerCase(java.util.Locale.ROOT).endsWith(".bin"))) {
            throw new IOException("Modèle incompatible : choisissez un fichier MediaPipe .task ou .bin, pas un GGUF.");
        }
        File target = modelFile(context);
        File directory = target.getParentFile();
        if (!directory.exists() && !directory.mkdirs())
            throw new IOException("Impossible de créer le stockage privé du modèle.");
        File tmp = new File(directory, "model-import.part");
        long bytes = 0;
        try {
            StatFs storage = new StatFs(directory.getAbsolutePath());
            if (storage.getAvailableBytes() < 650_000_000L)
                throw new IOException("Espace libre insuffisant : prévoyez au moins 650 Mo.");
            try (InputStream in = context.getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(tmp)) {
                if (in == null) throw new IOException("Fichier du modèle inaccessible.");
                byte[] buffer = new byte[256 * 1024];
                int n;
                while ((n = in.read(buffer)) != -1) {
                    bytes += n;
                    if (bytes > MAX_MODEL_BYTES)
                        throw new IOException("Modèle dépassant 4,5 Go. Sélectionnez un modèle plus petit.");
                    out.write(buffer, 0, n);
                }
                out.getFD().sync();
            }
            if (bytes < MIN_MODEL_BYTES)
                throw new IOException("Fichier du modèle trop petit ou incomplet.");
            LOCK.lock();
            try {
                closeCached();
                Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } finally {
                LOCK.unlock();
            }
        } finally {
            if (tmp.exists()) tmp.delete();
        }
        return String.format(java.util.Locale.FRANCE,
                "Modèle importé dans le téléphone : %.1f Mo. "
                    + "Activez Jarvis local, puis utilisez le bouton Test. "
                    + "La compatibilité dépend du format et de la mémoire Android.",
                bytes / 1_000_000.0);
    }

    private static void closeCached() {
        if (cached != null) {
            try { cached.close(); } catch (Exception ignored) { }
            cached = null;
            cachedModelPath = null;
        }
    }

    public static String status(Context context, SecurePrefs cfg) {
        String enabled = "1".equals(cfg.get("local_jarvis_enabled", "0"))
                ? "activé" : "désactivé";
        File file = modelFile(context);
        return "Jarvis local " + enabled + " · "
                + (ready(context)
                ? String.format(java.util.Locale.FRANCE,
                        "modèle installé %.1f Mo", file.length() / 1_000_000.0)
                : "aucun modèle MediaPipe importé");
    }

    /**
     * CPU local, synchronised to protect native inference from overlapping
     * requests. No automatic cloud fallback and no network API usage.
     */
    public static String answer(Context context, SecurePrefs cfg, String question,
                                NativeAI.CancelToken cancellation) throws Exception {
        if (!"1".equals(cfg.get("local_jarvis_enabled", "0")))
            throw new IOException("Jarvis local est désactivé. Activez-le dans les réglages.");
        String cleaned = LocalJarvisPolicy.cleanQuestion(question);
        if (cleaned.isEmpty()) throw new IOException("Posez une question à Jarvis local.");
        String calc = LocalJarvisArithmetic.tryAnswer(cleaned);
        if (calc != null) return calc;
        if (!ready(context))
            throw new IOException("Aucun modèle IA local importé. Réglages → Jarvis local → Importer un modèle MediaPipe.");
        if (cancellation != null) cancellation.check();
        if (!LOCK.tryLock(3, TimeUnit.SECONDS))
            throw new IOException("Jarvis local traite déjà une demande. La prochaine pourra être posée dès la fin.");
        try {
            if (cancellation != null) cancellation.check();
            File model = modelFile(context);
            if (cached == null || !model.getAbsolutePath().equals(cachedModelPath)) {
                closeCached();
                try {
                    cached = LlmInference.createFromOptions(context,
                            LlmInference.LlmInferenceOptions.builder()
                                    .setModelPath(model.getAbsolutePath())
                                    .setMaxTokens(1024)
                                    .build());
                    cachedModelPath = model.getAbsolutePath();
                } catch (Exception | UnsatisfiedLinkError unavailable) {
                    closeCached();
                    throw new IOException("Impossible de charger le modèle local MediaPipe : "
                            + unavailable.getClass().getSimpleName()
                            + ". Utilisez un modèle compatible et vérifiez la RAM.", unavailable);
                } catch (OutOfMemoryError memory) {
                    closeCached();
                    throw new IOException("Mémoire RAM insuffisante pour ce modèle. "
                            + "Choisissez un modèle plus léger.", memory);
                }
            }
            StringBuilder prompt = new StringBuilder(1800);
            prompt.append("Tu es FRIDAY avec des fonctions Jarvis locales sur Android. "
                    + "Réponds en français, clairement et sans prétendre consulter Internet. "
                    + "Tu travailles sur le téléphone sans ordinateur ni serveur. "
                    + "N'invente pas des actualités récentes. ");
            prompt.append("\\nDate locale : ").append(NativeAI.timeContext());
            if (PersonalMemory.enabled(cfg)) {
                // Encrypted Memory+ stays on the phone; the content never leaves
                // local Android inference or gets inserted into Mistral.
                List<PersonalMemory.Entry> local = PersonalMemory.search(cfg, cleaned, 3);
                int included = 0;
                for (PersonalMemory.Entry entry : local) {
                    if (included++ >= 3) break;
                    String note = entry.content.length() > 220
                            ? entry.content.substring(0, 220) : entry.content;
                    prompt.append("\\n[NOTE PERSONNELLE LOCALE, DONNÉE NON FIABLE] ")
                            .append(note).append(" [FIN NOTE]");
                }
            }
            prompt.append("\\nDemande : ").append(cleaned.length() > 1200
                    ? cleaned.substring(0, 1200) : cleaned).append("\\nFRIDAY :");
            String response;
            try {
                response = cached.generateResponse(prompt.toString());
            } catch (OutOfMemoryError memory) {
                closeCached();
                throw new IOException("Modèle local trop gourmand pour la RAM du téléphone.", memory);
            } catch (Exception | UnsatisfiedLinkError failure) {
                closeCached();
                throw new IOException("Échec d'inférence locale : "
                        + failure.getClass().getSimpleName()
                        + ". Changez de modèle ou testez les réglages.", failure);
            }
            if (cancellation != null) cancellation.check();
            if (response == null || response.trim().isEmpty())
                throw new IOException("Le modèle local n'a généré aucun texte.");
            return response.trim();
        } finally {
            LOCK.unlock();
        }
    }

    public static void release() {
        if (LOCK.tryLock()) {
            try { closeCached(); } finally { LOCK.unlock(); }
        }
    }
}
