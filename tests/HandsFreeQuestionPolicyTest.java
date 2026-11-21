import fr.friday.assistant.HandsFreeQuestionPolicy;

public class HandsFreeQuestionPolicyTest {
    private static void accept(String text) {
        if (!HandsFreeQuestionPolicy.acceptsDirectQuestion(text, true, true))
            throw new AssertionError("Refus d'une demande: " + text);
    }
    private static void reject(String text) {
        if (HandsFreeQuestionPolicy.acceptsDirectQuestion(text, true, true))
            throw new AssertionError("Fausse question: " + text);
    }
    public static void main(String[] args) {
        accept("Cherche les actualités scientifiques publiées aujourd'hui et cite les sources");
        accept("Quelle heure est il");
        accept("Comment tu t'appelles");
        accept("Je voudrais connaître la météo de demain");
        accept("J'aimerais une explication sur la lune");
        accept("Peux tu expliquer cette réponse");
        reject("d'épices actuelle");
        reject("les dernières épices du monde");
        reject("bonjour");
        reject("vendredi matin");
        if (HandsFreeQuestionPolicy.acceptsDirectQuestion(
                "Quelle heure est il", false, true))
            throw new AssertionError("Option désactivée");
        if (HandsFreeQuestionPolicy.acceptsDirectQuestion(
                "Quelle heure est il", true, false))
            throw new AssertionError("Anti-écho");
        System.out.println("HandsFreeQuestionPolicyTest OK");
    }
}
