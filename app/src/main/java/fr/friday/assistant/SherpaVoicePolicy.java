package fr.friday.assistant;

/**
 * Choix prudent du moteur de dictée.
 * Vosk reste le défaut : sherpa-onnx n'est activé qu'après import vérifié.
 * La détection STOP continue avec Vosk, indépendamment de ce choix.
 */
public final class SherpaVoicePolicy {
    private SherpaVoicePolicy() { }

    public static boolean useSherpa(String enabled, boolean modelReady) {
        return "1".equals(enabled) && modelReady;
    }

    public static String explain(String enabled, boolean modelReady) {
        if (!"1".equals(enabled)) return "Reconnaissance Vosk active (sherpa-onnx désactivé)";
        return modelReady
                ? "sherpa-onnx français demandé : tentative à la prochaine ouverture du micro"
                : "Modèle sherpa-onnx français incomplet : Vosk conservé";
    }
}
