import fr.friday.assistant.AssistantIdentity;

/** Régression : FRIDAY ne doit jamais s'identifier par le nom du fournisseur Mistral. */
public final class AssistantIdentityTest {
    private static void check(boolean yes, String why) {
        if (!yes) throw new AssertionError(why);
    }
    public static void main(String[] args) {
        String defaultPrompt = "Tu incarnes F.R.I.D.A.Y., un assistant vocal personnel francophone.";
        String answer = AssistantIdentity.reply("Friday, qui es-tu ?", "F.R.I.D.A.Y.", defaultPrompt);
        check(answer != null && answer.contains("Je suis F.R.I.D.A.Y."), "nom du profil ignoré");
        check(!answer.startsWith("Je suis Mistral"), "l'identité ne doit pas être celle du fournisseur");
        check(AssistantIdentity.reply("Comment tu t'appelles ?", "FRIDAY", defaultPrompt)
                .contains("Je suis FRIDAY"), "question de nom");
        check(AssistantIdentity.reply("Est-ce que tu es Mistral ?", "FRIDAY", defaultPrompt)
                .contains("Je suis FRIDAY"), "identité fournisseur");
        check(AssistantIdentity.reply("Quel est ton nom ?", "NOVA", "Parle avec calme.")
                .contains("Je suis NOVA"), "nom du profil utilisateur");
        check(AssistantIdentity.reply("Qui es-tu ?", "Robot", "Tu t'appelles Jarvis.")
                .contains("Je suis Jarvis"), "nom explicitement imposé par le prompt");
        check(AssistantIdentity.reply("Quel temps fait-il à Paris ?", "NOVA", defaultPrompt)
                == null, "une question ordinaire ne doit pas être interceptée");
        check(AssistantIdentity.reply("Peux-tu vérifier ma localisation ?", "NOVA", defaultPrompt)
                == null, "une question GPS ne doit pas être interceptée");
        System.out.println("AssistantIdentityTest OK (8 tests)");
    }
}
