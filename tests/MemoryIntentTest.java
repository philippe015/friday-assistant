import fr.friday.assistant.MemoryIntent;
public class MemoryIntentTest {
    private static void eq(Object actual, Object expected) {
        if (!expected.equals(actual)) throw new AssertionError("Attendu " + expected + " obtenu " + actual);
    }
    public static void main(String[] args) {
        eq(MemoryIntent.kind("retiens que je préfère le français"), MemoryIntent.SAVE);
        eq(MemoryIntent.kind("mémorise que j'aime la science"), MemoryIntent.SAVE);
        eq(MemoryIntent.fact("retiens que je préfère le français"), "je préfère le français");
        eq(MemoryIntent.kind("Qu'as-tu mémorisé ?"), MemoryIntent.READ);
        eq(MemoryIntent.kind("Efface ma mémoire"), MemoryIntent.CLEAR);
        eq(MemoryIntent.kind("Cherche dans ma mémoire le projet FRIDAY"), MemoryIntent.SEARCH);
        eq(MemoryIntent.searchQuery("Cherche dans ma mémoire le projet FRIDAY"), "le projet friday");
        eq(MemoryIntent.kind("Quel est le temps aujourd'hui ?"), MemoryIntent.NONE);
        System.out.println("MemoryIntentTest OK");
    }
}
