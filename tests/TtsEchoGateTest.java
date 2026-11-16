import fr.friday.assistant.TtsEchoGate;

public final class TtsEchoGateTest {
    private static void check(boolean ok, String why) {
        if (!ok) throw new AssertionError(why);
    }
    public static void main(String[] args) {
        long end = TtsEchoGate.afterVoice(10000L);
        check(TtsEchoGate.suppressGeneralAsr(true, 9000L, 0),
                "Ne jamais transcrire les questions pendant une réponse, même avant le TTS.");
        check(TtsEchoGate.suppressGeneralAsr(true, 20000L, end),
                "Le tour actif prime sur le délai après TTS.");
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
