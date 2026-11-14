package fr.friday.assistant;
public class FridayVersionPolicyTest {
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        check(FridayVersionPolicy.isOtherVersion("fr.friday.assistant.smartpreview", "fr.friday.assistant.preview"), "Ancienne version manquée");
        check(!FridayVersionPolicy.isOtherVersion("fr.friday.assistant.preview", "fr.friday.assistant.preview"), "Edition identique détectée");
        check(!FridayVersionPolicy.isOtherVersion("fr.friday.assistant.preview", "com.example.fake"), "Autre application détectée");
        check(FridayVersionPolicy.warning(0).contains("Aucune"), "Texte zéro version");
        check(FridayVersionPolicy.warning(2).contains("2"), "Texte deux versions");
        check(FridayVersionPolicy.warning(2).contains("ne permet pas"), "Limite Android omise");
        System.out.println("FridayVersionPolicyTest : OK");
    }
}
