import fr.friday.assistant.VoiceTurnPolicy;

/** Une réponse lente ne doit pas empêcher une nouvelle demande vocale explicite. */
public final class VoiceTurnPolicyTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        check(VoiceTurnPolicy.canReplace(true, false, false, true),
                "Une nouvelle question Jarvis doit fonctionner pendant le calcul silencieux");
        check(VoiceTurnPolicy.canReplace(true, false, true, true),
                "Une nouvelle demande peut remplacer un calcul silencieux");
        check(VoiceTurnPolicy.canReplace(true, true, true, true),
                "Interruption explicite pendant la voix active");
        check(!VoiceTurnPolicy.canReplace(true, true, false, true),
                "La voix ne doit pas être coupée si l'interruption est désactivée");
        check(!VoiceTurnPolicy.canReplace(true, false, false, false),
                "Pas de nouvelle demande sans mot-clé confirmé");
        check(!VoiceTurnPolicy.canReplace(true, true, true, false),
                "L'écho TTS ne doit jamais être interprété comme un ordre");
        check(!VoiceTurnPolicy.canReplace(false, false, true, true),
                "Ne pas remplacer une réponse inexistante");
        System.out.println("VoiceTurnPolicyTest OK");
    }
}
