package fr.friday.assistant;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.util.Base64;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/** TTS avec clonage zero-shot. Seule API appelée : https://api.mistral.ai. */
public final class MistralVoice {
    private MistralVoice() {}
    /** Annulation depuis le thread d'écoute, sans perturber un aperçu vocal distinct. */
    public static final class Playback {
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private volatile HttpURLConnection connection;
        private volatile MediaPlayer player;
        public void cancel() {
            cancelled.set(true);
            HttpURLConnection c = connection;
            if (c != null) c.disconnect();
            MediaPlayer p = player;
            if (p != null) { try { p.stop(); } catch (Exception ignored) { } }
        }
        boolean live(BooleanSupplier predicate) { return !cancelled.get() && predicate.getAsBoolean(); }
    }
    public static boolean enabled(Context context, SecurePrefs settings) {
        return "1".equals(settings.get("clone_consent", "0")) &&
            "1".equals(settings.get("clone_enabled", "0")) &&
            "1".equals(settings.get("voice_enabled", "1")) &&
            VoiceSampleRecorder.exists(context);
    }
    public static void speak(Context context, SecurePrefs prefs, String text, BooleanSupplier keepPlaying) throws Exception {
        speak(context, prefs, text, keepPlaying, new Playback());
    }
    public static void speak(Context context, SecurePrefs prefs, String text, BooleanSupplier keepPlaying, Playback playback) throws Exception {
        if (!enabled(context, prefs)) throw new IOException("Voix clonée non activée ou échantillon absent.");
        String apiKey = prefs.secret("mistral");
        if (apiKey.isEmpty()) throw new IOException("Clé Mistral absente dans les réglages natifs.");
        List<String> chunks = VoiceClonePolicy.chunks(text);
        if (chunks.isEmpty()) throw new IOException("Aucun texte à lire.");
        for (String part : chunks) {
            if (!playback.live(keepPlaying)) return;
            File audio = null;
            try {
                audio = requestAudio(context, apiKey, part, playback);
                if (!playback.live(keepPlaying)) return;
                playWav(audio, keepPlaying, playback);
            } finally {
                if (audio != null && audio.exists()) audio.delete();
            }
        }
    }
    private static File requestAudio(Context context, String key, String input, Playback playback) throws Exception {
        File reference = VoiceSampleRecorder.sample(context);
        if (!VoiceClonePolicy.isValidWavSize(reference.length())) throw new IOException("Référence vocale invalide.");
        String base64 = Base64.encodeToString(Files.readAllBytes(reference.toPath()), Base64.NO_WRAP);
        JSONObject body = new JSONObject();
        body.put("model", VoiceClonePolicy.MODEL);
        body.put("input", input);
        body.put("ref_audio", base64);
        body.put("response_format", "wav");
        if (playback.cancelled.get()) return null;
        HttpURLConnection conn = (HttpURLConnection)new URL(VoiceClonePolicy.ENDPOINT).openConnection();
        playback.connection = conn;
        if (playback.cancelled.get()) { conn.disconnect(); return null; }
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(15000); conn.setReadTimeout(90000);
        conn.setRequestProperty("Authorization", "Bearer " + key);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        try {
            byte[] request = body.toString().getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(request.length);
            try (OutputStream out = conn.getOutputStream()) { out.write(request); }
            if (playback.cancelled.get()) return null;
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                // Ne pas exposer les données d'erreur, qui peuvent inclure du contenu transmis.
                // Ne pas imprimer la clé ni le contenu du WAV / de la requête.
                if (code == 401 || code == 403) throw new IOException("Mistral TTS : clé invalide ou accès vocal refusé (HTTP " + code + ").");
                if (code == 429) throw new IOException("Mistral TTS : limite de requêtes ou crédits insuffisants (429).");
                if (code == 400 || code == 404 || code == 422)
                    throw new IOException("Mistral TTS : modèle vocal indisponible ou échantillon refusé (HTTP " + code + ").");
                throw new IOException("Mistral TTS : erreur HTTP " + code + ".");
            }
            JSONObject response;
            try (InputStream in = conn.getInputStream()) {
                response = new JSONObject(new String(readBounded(in, 30_000_000), StandardCharsets.UTF_8));
            }
            if (playback.cancelled.get()) return null;
            String encoded = response.optString("audio_data", "");
            if (encoded.isEmpty() || encoded.length() > 28_000_000) throw new IOException("Réponse audio Mistral vide ou excessive.");
            byte[] wav = Base64.decode(encoded, Base64.DEFAULT);
            if (wav.length < 44 || wav.length > 20_000_000) throw new IOException("Audio généré invalide.");
            File file = File.createTempFile("friday_tts_", ".wav", context.getCacheDir());
            try (FileOutputStream out = new FileOutputStream(file)) { out.write(wav); }
            return file;
        } finally {
            if (playback.connection == conn) playback.connection = null;
            conn.disconnect();
        }
    }
    private static byte[] readBounded(InputStream in, int max) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] part = new byte[8192]; int size;
        while ((size = in.read(part)) != -1) {
            if (buffer.size() + size > max) throw new IOException("Réponse vocale trop volumineuse.");
            buffer.write(part, 0, size);
        }
        return buffer.toByteArray();
    }
    private static void playWav(File file, BooleanSupplier keepPlaying, Playback playback) throws Exception {
        if (file == null || !playback.live(keepPlaying)) return;
        MediaPlayer player = new MediaPlayer();
        playback.player = player;
        CountDownLatch finished = new CountDownLatch(1);
        AtomicBoolean failed = new AtomicBoolean(false);
        try {
            player.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
            player.setDataSource(file.getAbsolutePath());
            player.setOnCompletionListener(p -> finished.countDown());
            player.setOnErrorListener((p,what,extra) -> { failed.set(true); finished.countDown(); return true; });
            player.prepare();
            if (!playback.live(keepPlaying)) return;
            player.start();
            long until = System.currentTimeMillis() + Math.max(15000L, player.getDuration() + 15000L);
            while (playback.live(keepPlaying) && System.currentTimeMillis() < until) {
                if (finished.await(80, TimeUnit.MILLISECONDS)) break;
            }
            if (!playback.live(keepPlaying)) return;
            if (failed.get()) throw new IOException("La lecture de la voix clonée a échoué.");
            if (keepPlaying.getAsBoolean() && finished.getCount() > 0)
                throw new IOException("Lecture de la voix clonée expirée.");
        } finally {
            if (playback.player == player) playback.player = null;
            try { if (player.isPlaying()) player.stop(); } catch (Exception ignored) {}
            player.release();
        }
    }
}
