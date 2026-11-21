import fr.friday.assistant.SherpaVoicePolicy;

public class SherpaVoicePolicyTest {
    static void check(boolean v, String label) {
        if (!v) throw new AssertionError(label);
    }
    public static void main(String[] args) {
        check(!SherpaVoicePolicy.useSherpa("0", true), "désactivé");
        check(!SherpaVoicePolicy.useSherpa("1", false), "modèle manquant");
        check(SherpaVoicePolicy.useSherpa("1", true), "opt-in seulement");
        check(!SherpaVoicePolicy.useSherpa("", true), "valeur absente");
        check(SherpaVoicePolicy.explain("1", false).contains("Vosk"), "secours annoncé");
        System.out.println("SherpaVoicePolicyTest OK");
    }
}
