package fr.friday.assistant;

import org.vosk.Model;
import org.vosk.Recognizer;

/** Contrat commun : un seul AudioRecord, un seul moteur de dictée générale. */
public interface JarvisVoiceDecoder extends AutoCloseable {
    boolean acceptWaveForm(byte[] pcm, int size);
    String getResult();
    String getPartialResult();
    String getFinalResult();
    String name();
    void close();

    /** Mode historique, toujours disponible sans sherpa ni modèle ONNX. */
    final class Vosk implements JarvisVoiceDecoder {
        private final Recognizer recognizer;
        public Vosk(Model model, String grammar) throws Exception {
            recognizer = new Recognizer(model, 16000.0f);
            recognizer.setWords(true);
            if (grammar != null) {
                try { recognizer.setGrammar(grammar); }
                catch (RuntimeException | UnsatisfiedLinkError invalid) {
                    try { recognizer.setGrammar("[]"); }
                    catch (Exception ignored) { }
                }
            }
        }
        public boolean acceptWaveForm(byte[] pcm, int size) {
            return recognizer.acceptWaveForm(pcm, size);
        }
        public String getResult() { return recognizer.getResult(); }
        public String getPartialResult() { return recognizer.getPartialResult(); }
        public String getFinalResult() { return recognizer.getFinalResult(); }
        public String name() { return "Vosk"; }
        public void close() { recognizer.close(); }
    }
}
