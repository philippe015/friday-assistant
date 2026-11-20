import fr.friday.assistant.DialogueMemoryPolicy;

public final class DialogueMemoryPolicyTest {
    private static void ok(boolean valid, String name) {
        if (!valid) throw new AssertionError(name);
    }

    public static void main(String[] args) {
        String example = "Jarvis as-tu de la mémoire de nos dialogues";
        ok(DialogueMemoryPolicy.isPrivateDialogue(example), "historique intime reconnu");
        ok(DialogueMemoryPolicy.asksMemoryStatus(example), "vérification mémoire locale");
        ok(DialogueMemoryPolicy.keepLocal(example), "aucune recherche Wikipédia ou Scribbr");
        ok(DialogueMemoryPolicy.isPrivateDialogue("Tu te souviens de notre conversation"),
                "références aux conversations personnelles");
        ok(DialogueMemoryPolicy.isPrivateDialogue("Résume nos derniers échanges"),
                "résumé de l'historique");
        ok(DialogueMemoryPolicy.isContextualFollowUp("Et pourquoi ?"),
                "pourquoi elliptique");
        ok(DialogueMemoryPolicy.isContextualFollowUp("Développe davantage"),
                "approfondissement");
        ok(DialogueMemoryPolicy.isContextualFollowUp("Donne-moi des exemples"),
                "exemples du sujet précédent");
        ok(DialogueMemoryPolicy.isContextualFollowUp("Et pour Lyon ?"),
                "relance référentielle");
        ok(!DialogueMemoryPolicy.isContextualFollowUp("Pourquoi les oiseaux migrent-ils ?"),
                "question factuelle indépendante");
        ok(!DialogueMemoryPolicy.isPrivateDialogue(
                "15 exemples complets de mémoires universitaires"),
                "recherche documentaire et non conversation");
        ok(!DialogueMemoryPolicy.isPrivateDialogue("Explique le dialogue socratique"),
                "philosophie, pas la mémoire de l'assistant");
        ok(!DialogueMemoryPolicy.isContextualFollowUp("Actualités de France aujourd'hui"),
                "nouvelles actuelles requièrent sources");
        ok(!DialogueMemoryPolicy.isContextualFollowUp("Cherche sur Internet maintenant"),
                "instruction Web explicite");
        System.out.println("DialogueMemoryPolicyTest OK (14 cas)");
    }
}
