package fr.friday.assistant;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Dernière défense contre la boucle TTS -> micro -> Vosk -> nouvelle question.
 *
 * Comparaison locale des mots (sans son, sans réseau, sans stockage permanent).
 * Elle ne concerne que les transcriptions MICROPHONE, jamais les questions
 * tapées par l'utilisateur. Le texte de référence est une voix émise récemment
 * par JARVIS, et non l'historique privé.
 */
public final class VoiceSelfEchoPolicy {
    private VoiceSelfEchoPolicy() { }

    public static final long MEMORY_MS = 45_000L;

    private static List<String> words(String input) {
        String normalized = Normalizer.normalize(input == null ? "" : input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.FRENCH)
                .replaceAll("[^a-z0-9]+", " ").trim();
        ArrayList<String> tokens = new ArrayList<>();
        for (String word : normalized.split("\\s+")) {
            if (!word.isEmpty()) tokens.add(word);
            if (tokens.size() >= 900) break;
        }
        return tokens;
    }

    /**
     * Une réponse longue peut être entendue partiellement par le micro :
     * recherche d'un sous-passage partagé, même quand Vosk déforme
     * « horodatage RSS » en « rodage reiser ».
     * À l'inverse « et demain ? » n'est jamais écarté comme écho.
     */
    public static boolean resemblesRecentSpeech(String microphone, String spoken,
                                                 long nowMs, long lastSpokenAtMs) {
        if (microphone == null || spoken == null || lastSpokenAtMs <= 0L
                || nowMs < lastSpokenAtMs || nowMs - lastSpokenAtMs > MEMORY_MS)
            return false;
        List<String> heard = words(microphone);
        List<String> said = words(spoken);
        // Plusieurs mots sont nécessaires pour éviter de bloquer une
        // question réelle très courte (« et demain ? »).
        if (heard.size() < 8 || said.size() < 8) return false;

        // Un bloc de 4 mots consécutifs reste discriminant pour des
        // formulations courantes, mais seul ne suffit pas à rejeter.
        Set<String> joined = new HashSet<>();
        for (int i = 0; i + 3 < said.size(); i++)
            joined.add(gram(said, i));
        int exact4 = 0;
        for (int i = 0; i + 3 < heard.size(); i++)
            if (joined.contains(gram(heard, i))) exact4++;

        // Le micro peut capter seulement une partie d'un long discours.
        // On mesure donc la couverture du candidat, pas celle de la voix.
        int overlap = 0;
        Set<String> saidWords = new HashSet<>(said);
        for (String w : heard) if (saidWords.contains(w)) overlap++;

        double ratio = overlap / (double) heard.size();
        return (exact4 >= 2 && ratio >= 0.62)
                || (exact4 >= 1 && heard.size() >= 16 && ratio >= 0.77);
    }

    private static String gram(List<String> input, int i) {
        return input.get(i) + " " + input.get(i + 1) + " "
                + input.get(i + 2) + " " + input.get(i + 3);
    }
}
