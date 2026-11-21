package fr.friday.assistant;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Intelligence+ : retrieval lexical de sujets anciens, uniquement EN LOCAL.
 * Aucune donnée, note ou historique n'est envoyé à un moteur de recherche.
 * Cette classe ne dépend ni d'Android, ni d'Internet, ni d'un modèle d'IA.
 */
public final class ConversationRecallPolicy {
    private ConversationRecallPolicy() {}

    private static final String COMMON =
            " aussi alors avec beaucoup bonjour ca car cette cet ces comme "
          + " comment dans depuis donne donc elle elles encore entre est etais "
          + " etait faire fait faut il ils leurs leur les ma mais mes moi mon "
          + " nous nous sommes notre pas peux peut plus pour pourquoi quand "
          + " quelle quelles quels quel que qui quoi sans sont sous sur suis "
          + " ta te tes toi ton tous tout toutes tu une vous votre vos "
          + " explique parler dis moi reponse question jarvis friday "
          + " aujourd hui maintenant demain hier dernier derniere "
          + " detail details precision precisions informations "
          + " avais avons deja aussi voici voudrais veux ";

    public static String fold(String text) {
        return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.FRENCH)
                .replaceAll("[^a-z0-9]+", " ").replaceAll("\\s+", " ").trim();
    }

    private static Set<String> keywords(String text) {
        Set<String> words = new HashSet<>();
        for (String word : fold(text).split(" ")) {
            if (word.length() < 4 || COMMON.contains(" " + word + " ")) continue;
            // Légère normalisation du pluriel ; pas d'embedding ni de
            // prétention à une compréhension sémantique parfaite.
            if (word.length() > 5 && word.endsWith("s")) word = word.substring(0, word.length() - 1);
            words.add(word);
        }
        return words;
    }

    /** Un score nul interdit le rappel d'une ancienne question hors sujet. */
    public static int score(String currentQuestion, String earlierQuestion) {
        Set<String> requested = keywords(currentQuestion);
        Set<String> previous = keywords(earlierQuestion);
        if (requested.isEmpty() || previous.isEmpty()) return 0;
        int hits = 0;
        for (String word : requested) if (previous.contains(word)) hits++;
        return hits;
    }

    /** Les réponses échouées/diagnostics ne deviennent pas des souvenirs. */
    public static boolean usableAnswer(String answer) {
        if (answer == null || answer.trim().length() < 12) return false;
        String normalized = fold(answer);
        return !normalized.startsWith("reponse indisponible")
                && !normalized.startsWith("recherche internet non aboutie")
                && !normalized.startsWith("j ai recu votre question mais jarvis")
                && !normalized.startsWith("erreur");
    }
}
