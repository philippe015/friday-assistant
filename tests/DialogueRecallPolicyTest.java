import fr.friday.assistant.DialogueRecallPolicy;
import java.util.ArrayList;
import java.util.List;

public final class DialogueRecallPolicyTest {
    private static void ok(boolean result, String name) {
        if (!result) throw new AssertionError(name);
    }

    public static void main(String[] args) {
        List<DialogueRecallPolicy.Event> history = new ArrayList<>();
        history.add(new DialogueRecallPolicy.Event("user",
                "Nous avions parlé du chauffage à granulés de la maison."));
        history.add(new DialogueRecallPolicy.Event("assistant",
                "Nous avions comparé des poêles à granulés."));
        history.add(new DialogueRecallPolicy.Event("user",
                "Pour mon Android je préfère une écoute en français."));
        history.add(new DialogueRecallPolicy.Event("assistant",
                "J'ai décrit un assistant qui répond en français."));
        for (int i = 0; i < 12; i++) {
            history.add(new DialogueRecallPolicy.Event("user",
                    "Quelle histoire lisons-nous ?"));
            history.add(new DialogueRecallPolicy.Event("assistant",
                    "Parlons de romans et de poésie."));
        }

        List<DialogueRecallPolicy.Turn> result = DialogueRecallPolicy.relevant(
                "Jarvis, tu te souviens de notre conversation sur le chauffage ?", history, 10, 2);
        ok(result.size() == 1, "rappel ciblé d'un ancien sujet");
        ok(result.get(0).question.contains("chauffage"), "historique non inventé");
        ok(result.get(0).answer.contains("poêles"), "question et réponse cohérentes");

        ok(DialogueRecallPolicy.relevant(
                "Fais une recherche Web sur le chauffage", history, 10, 2).isEmpty(),
                "aucune mémoire privée dans une recherche générale");
        ok(DialogueRecallPolicy.relevant(
                "Tu te souviens de notre conversation ?", history, 10, 2).isEmpty(),
                "sans sujet explicite, ne pas prétendre retrouver un ancien échange");
        ok(DialogueRecallPolicy.relevant(
                "Tu te souviens de notre conversation sur les satellites ?", history, 10, 2).isEmpty(),
                "aucun souvenir inventé");
        ok(DialogueRecallPolicy.relevant(
                "Tu te souviens de notre conversation sur le chauffage ?", history, 80, 2).isEmpty(),
                "exclusion de tous les anciens événements");
        ok(DialogueRecallPolicy.relevant(
                "Tu te souviens de notre conversation sur le chauffage ?", history, 10, 0).isEmpty(),
                "limite de résultats");
        System.out.println("DialogueRecallPolicyTest OK");
    }
}
