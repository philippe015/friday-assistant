import fr.friday.assistant.VoiceQuestionPolicy;

public final class VoiceQuestionPolicyTest {
    private static void check(boolean b, String label) {
        if (!b) throw new AssertionError(label);
    }
    public static void main(String[] args) {
        check(VoiceQuestionPolicy.plausibleDirectQuestion("Quelle heure est-il ?", false),
                "question directe après bouton écoute");
        check(VoiceQuestionPolicy.plausibleDirectQuestion(
                "Cherche les actualités scientifiques publiées aujourd'hui", false),
                "demande d'actualités directe");
        check(VoiceQuestionPolicy.plausibleDirectQuestion("Et demain ?", true),
                "relance contextuelle autorisée");
        check(!VoiceQuestionPolicy.plausibleDirectQuestion("Et demain ?", false),
                "pas de relance sans contexte");
        check(!VoiceQuestionPolicy.plausibleDirectQuestion("d'épices actuelle", true),
                "fragment Vosk à rejeter");
        check(!VoiceQuestionPolicy.plausibleDirectQuestion("les épices", true),
                "fragment sans verbe de demande à rejeter");
        check(!VoiceQuestionPolicy.plausibleDirectQuestion("bruit", false),
                "un seul mot ne déclenche jamais de requête");
        check(VoiceQuestionPolicy.plausibleDirectQuestion("Explique moi les étoiles", false),
                "requête ordinaire sans mot-clé");
        System.out.println("VoiceQuestionPolicyTest OK");
    }
}
