package fr.friday.assistant;

import android.content.Context;
import com.k2fsa.sherpa.onnx.OnlineModelConfig;
import com.k2fsa.sherpa.onnx.OnlineRecognizer;
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig;
import com.k2fsa.sherpa.onnx.OnlineStream;
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig;
import java.io.File;
import org.json.JSONObject;

/**
 * Adaptateur sherpa-onnx v2.0.2, streaming Zipformer français.
 * Reçoit les PCM 16 kHz du MÊME AudioRecord que Vosk : jamais deux micros.
 * Les commandes STOP pendant le TTS sont toujours reconnues par le décodeur
 * Vosk existant et gardent la priorité.
 */
public final class SherpaVoiceDecoder implements JarvisVoiceDecoder {
    private static OnlineRecognizer shared;
    private static String sharedPath;
    private OnlineStream stream;
    private String finalEndpoint = "";

    public SherpaVoiceDecoder(Context ctx) {
        if (!SherpaVoiceModels.ready(ctx))
            throw new IllegalStateException("Modèle français sherpa-onnx non installé.");
        File folder = SherpaVoiceModels.directory(ctx);
        synchronized (SherpaVoiceDecoder.class) {
            if (shared == null || !folder.getAbsolutePath().equals(sharedPath)) {
                shutdown();
                // L'AAR Android 2.0.2 expose les classes à setters (API Android),
                // différente de l'API Builder JVM disponible dans le dépôt source.
                OnlineTransducerModelConfig transducer = new OnlineTransducerModelConfig();
                transducer.setEncoder(new File(folder, "encoder.onnx").getAbsolutePath());
                transducer.setDecoder(new File(folder, "decoder.onnx").getAbsolutePath());
                transducer.setJoiner(new File(folder, "joiner.onnx").getAbsolutePath());
                OnlineModelConfig model = new OnlineModelConfig();
                model.setTransducer(transducer);
                model.setTokens(new File(folder, "tokens.txt").getAbsolutePath());
                model.setNumThreads(2);
                model.setDebug(false);
                model.setModelType("zipformer");
                OnlineRecognizerConfig config = new OnlineRecognizerConfig();
                config.setModelConfig(model);
                shared = new OnlineRecognizer(ctx.getAssets(), config);
                sharedPath = folder.getAbsolutePath();
            }
            stream = shared.createStream("");
        }
    }

    private static String json(String key, String value) {
        try { return new JSONObject().put(key, value == null ? "" : value).toString(); }
        catch (org.json.JSONException ignored) { return "{}"; }
    }

    private void decodeReady() {
        int tries = 0;
        while (shared.isReady(stream)) {
            if (++tries > 500)
                throw new IllegalStateException("Décodeur sherpa-onnx bloqué : Vosk sera réactivé.");
            shared.decode(stream);
        }
    }

    @Override public boolean acceptWaveForm(byte[] data, int size) {
        if (stream == null || data == null || size < 0 || size > data.length)
            throw new IllegalStateException("Flux sherpa-onnx indisponible.");
        int samples = size / 2;
        if (samples == 0) return false;
        float[] pcm = new float[samples];
        for (int i = 0; i < samples; i++) {
            int b0 = data[2 * i] & 0xff;
            int b1 = data[2 * i + 1];
            short value = (short)((b1 << 8) | b0);
            pcm[i] = value / 32768.0f;
        }
        stream.acceptWaveform(pcm, 16000);
        decodeReady();
        if (shared.isEndpoint(stream)) {
            finalEndpoint = shared.getResult(stream).getText();
            shared.reset(stream);
            return true;
        }
        return false;
    }

    @Override public String getResult() {
        String value = finalEndpoint;
        finalEndpoint = "";
        return json("text", value);
    }

    @Override public String getPartialResult() {
        return json("partial", shared.getResult(stream).getText());
    }

    @Override public String getFinalResult() {
        float[] silence = new float[12800];
        stream.acceptWaveform(silence, 16000);
        decodeReady();
        String text = shared.getResult(stream).getText();
        shared.reset(stream);
        finalEndpoint = "";
        return json("text", text);
    }

    @Override public String name() { return "sherpa-onnx FR"; }

    @Override public void close() {
        if (stream != null) {
            stream.release();
            stream = null;
        }
    }

    public static synchronized void shutdown() {
        if (shared != null) {
            shared.release();
            shared = null;
            sharedPath = null;
        }
    }
}
