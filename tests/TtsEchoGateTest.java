import fr.friday.assistant.TtsEchoGate;

public final class TtsEchoGateTest {
    private static void check(boolean ok, String why) {
        if (!ok) throw new AssertionError(why);
    }
    public static void main(String[] args) {
        long end = TtsEchoGate.afterVoice(10000L);
        // Bug historique v3.8.8 : currentTurn non nul pendant 1-2 minutes
        // de Web Mistral rendait Vosk sourd bien qu'aucune voix ne soit émise.
        check(!TtsEchoGate.suppressGeneralAsr(false, 9000L, 0),
                "Pendant une requête réseau muette, la dictée doit rester active.");
        check(TtsEchoGate.suppressGeneralAsr(true, 9000L, 0),
                "Pendant la restitution audio réelle, bloquer le Vosk général.");
        check(TtsEchoGate.suppressGeneralAsr(true, 20000L, end),
                "La restitution TTS prime sur le délai après lecture.");
        check(TtsEchoGate.useStopRecognizer(true, true),
                "Décodeur STOP disponible pendant la synthèse.");
        check(!TtsEchoGate.useStopRecognizer(false, true),
                "Pas de double Vosk pendant une attente Mistral.");
        check(!TtsEchoGate.useStopRecognizer(true, false),
                "Respecter la désactivation utilisateur du STOP.");
        check(TtsEchoGate.staleNetworkTurn(true, false, 151000L, 1000L, 150000L),
                "Annuler un modèle silencieux bloqué à l'échéance.");
        check(!TtsEchoGate.staleNetworkTurn(true, true, 180000L, 1000L, 150000L),
                "Ne pas couper une réponse déjà en cours de lecture.");
        check(!TtsEchoGate.staleNetworkTurn(false, false, 180000L, 1000L, 150000L),
                "Ne pas relancer un tour déjà terminé.");
        check(!TtsEchoGate.staleNetworkTurn(true, false, 150999L, 1000L, 150000L),
                "Ne pas expirer trop tôt.");
        check(TtsEchoGate.suppressGeneralAsr(false, 10001L, end),
                "Ignorer les premiers sons réverbérés après TTS.");
        check(TtsEchoGate.suppressGeneralAsr(false, end - 1, end),
                "Ignorer la dernière milliseconde de l'écho.");
        check(!TtsEchoGate.suppressGeneralAsr(false, end, end),
                "Réouvrir le micro de dictée à la fin du délai.");
        long stop = TtsEchoGate.afterStop(10000L);
        check(stop < end && stop > 10000L, "STOP reste plus réactif que la fin naturelle.");
        check(!TtsEchoGate.suppressGeneralAsr(false, stop, stop),
                "Les questions deviennent possibles après un STOP et le bref délai.");
        System.out.println("TtsEchoGateTest OK");
    }
}
