import fr.friday.assistant.VoiceVocabulary;
import java.util.List;

public final class VoiceVocabularyTest {
    private static void check(boolean condition) {
        if (!condition) throw new AssertionError("Vocabulaire Vosk invalide");
    }
    public static void main(String[] args) {
        check(VoiceVocabulary.grammar("") == null);
        check(VoiceVocabulary.grammar("\n - \n") == null);
        List<String> names = VoiceVocabulary.phrases("Elite Dangerous\nVendredi\nelite dangerous");
        check(names.size() == 2);
        check(names.contains("elite dangerous"));
        String grammar = VoiceVocabulary.grammar("elite dangerous\nSaint-Maur-des-Fossés");
        check(grammar.contains("\"[unk]\""));
        check(grammar.contains("\"friday\""));
        check(grammar.contains("saint-maur-des-fossés"));
        check(VoiceVocabulary.phrases("x\nnom").size() == 1);
        StringBuilder many = new StringBuilder();
        for (int i = 0; i < 60; i++) many.append("lieu ").append(i).append('\n');
        check(VoiceVocabulary.phrases(many.toString()).size() == VoiceVocabulary.MAX_LINES);
        check(!VoiceVocabulary.grammar("bonjour\nx\"], \"injected").contains("\"injected\""));
        System.out.println("VoiceVocabularyTest OK");
    }
}
