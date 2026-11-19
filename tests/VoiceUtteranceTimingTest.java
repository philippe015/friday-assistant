import fr.friday.assistant.VoiceUtteranceTiming;

public final class VoiceUtteranceTimingTest {
    private static void eq(int expected, int actual, String name) {
        if (expected != actual)
            throw new AssertionError(name + ": attendu " + expected + ", obtenu " + actual);
    }
    public static void main(String[] args) {
        eq(2100, VoiceUtteranceTiming.timeoutMs("", "bonjour", 2100, false),
                "courte phrase");
        eq(3100, VoiceUtteranceTiming.timeoutMs("friday cherche", "les actualités pour",
                2100, false), "phrase incomplète");
        eq(2550, VoiceUtteranceTiming.timeoutMs(
                "friday recherche dans tous les journaux de france ces informations",
                "", 2100, false), "question longue");
        eq(1300, VoiceUtteranceTiming.timeoutMs("stop", "", 2100, true),
                "commande stop rapide");
        eq(6000, VoiceUtteranceTiming.timeoutMs("friday recherche pour", "", 6000, false),
                "limite haute");
        eq(900, VoiceUtteranceTiming.timeoutMs("friday", "", 300, false),
                "limite basse");
        System.out.println("VoiceUtteranceTimingTest OK");
    }
}
