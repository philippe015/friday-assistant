import fr.friday.assistant.MemoryRules;

public final class MemoryRulesTest {
    private static void ok(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        String[] typed = MemoryRules.parseLine("[Projet] Travailler sur FRIDAY avec Mistral");
        ok("Projet".equals(typed[0]), "category");
        ok("Travailler sur FRIDAY avec Mistral".equals(typed[1]), "content");
        ok("Général".equals(MemoryRules.parseLine("Bonjour")[0]), "default category");
        ok("[Projet] Travailler sur FRIDAY avec Mistral".equals(
                MemoryRules.format(typed[0], typed[1])), "format");
        ok(MemoryRules.normalize("PRÉFÉRENTIEL").equals("preferentiel"), "accent normalization");
        ok(MemoryRules.duplicateKey("PROJET","Bonjour").equals(
                MemoryRules.duplicateKey("Projet","bonjour")), "dedup case accent");
        ok(MemoryRules.score("cherche mémoire projet Friday", "Projet", "Application Friday Mistral") > 0,
                "related topical query");
        ok(MemoryRules.score("accident chimique", "Cuisine", "Recette de gâteau") == 0,
                "unrelated query must not be shared");
        ok(MemoryRules.score("Paris", "voyages", "Mon trajet pour Paris") > 0,
                "keyword search");
        ok(MemoryRules.score("bonjour", "Général", "Notre monde") == 0,
                "nonmatching word");
        try {
            MemoryRules.parseLine("[Santé] " + "a".repeat(601));
            throw new AssertionError("long note must fail");
        } catch (IllegalArgumentException expected) {}
        try {
            MemoryRules.parseLine("[Groupe] ");
            throw new AssertionError("empty note must fail");
        } catch (IllegalArgumentException expected) {}
        System.out.println("MemoryRulesTest OK (12 tests)");
    }
}
