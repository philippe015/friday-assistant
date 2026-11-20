package fr.friday.assistant;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Local, deterministic retrieval of older dialogue turns by topical overlap.
 * No cloud, model call, embeddings download, external index or persistence.
 * The encrypted conversation history remains the only source of truth.
 */
public final class DialogueRecallPolicy {
    private DialogueRecallPolicy() { }

    private static final Set<String> STOP = new HashSet<>(Arrays.asList(
            "jarvis", "bonjour", "merci", "avec", "dans", "pour", "depuis", "entre",
            "notre", "votre", "leurs", "nous", "vous", "moi", "elle", "elles",
            "leur", "mes", "mon", "une", "des", "est", "sont", "etre", "avais",
            "etait", "fait", "faire", "avez", "souviens", "souvenir", "rappelle",
            "rappelles", "discussion", "conversations", "conversation", "dialogue",
            "dialogues", "echanges", "echange", "historique", "memoire",
            "parlions", "parlait", "parle", "parler", "question", "questions",
            "precedent", "precedente", "precedents", "derniere", "dernier",
            "derniers", "resume", "resumez", "explique", "detail", "details",
            "encore", "avant", "comment", "pourquoi", "quelle", "quelles",
            "quels", "quel", "quoi", "quand", "demande", "demandes",
            "donne", "redonne", "peux", "peut", "veux", "reviens",
            "sujet", "sujets", "concernant", "propos", "cette"
    ));

    public static final class Event {
        public final String kind;
        public final String text;

        public Event(String kind, String text) {
            this.kind = kind == null ? "" : kind;
            this.text = text == null ? "" : text;
        }
    }

    public static final class Turn {
        public final int eventIndex;
        public final String question;
        public final String answer;
        private final int score;

        private Turn(int eventIndex, String question, String answer, int score) {
            this.eventIndex = eventIndex;
            this.question = question;
            this.answer = answer;
            this.score = score;
        }
    }

    private static Set<String> tokens(String raw) {
        String s = Normalizer.normalize(raw == null ? "" : raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.FRENCH)
                .replaceAll("[^a-z0-9]+", " ");
        Set<String> result = new LinkedHashSet<>();
        for (String word : s.split("\\s+")) {
            if (word.length() >= 4 && !STOP.contains(word) && !word.matches("\\d+"))
                result.add(word);
        }
        return result;
    }

    /**
     * Only explicit private-conversation questions trigger older-topic recall.
     * trailingExcluded is the number of recent events already in the normal context.
     * Returns chronological turns, not free-form claims about the user's history.
     */
    public static List<Turn> relevant(String question, List<Event> events,
                                      int trailingExcluded, int maxTurns) {
        if (events == null || maxTurns <= 0 || !DialogueMemoryPolicy.isPrivateDialogue(question))
            return Collections.emptyList();
        Set<String> topic = tokens(question);
        if (topic.isEmpty()) return Collections.emptyList();
        int cutoff = Math.max(0, events.size() - Math.max(0, trailingExcluded));
        ArrayList<Turn> candidates = new ArrayList<>();
        for (int i = 0; i < cutoff; i++) {
            Event e = events.get(i);
            if (!"user".equals(e.kind)) continue;
            String answer = "";
            int turnIndex = i;
            if (i + 1 < cutoff && "assistant".equals(events.get(i + 1).kind))
                answer = events.get(++i).text;
            Set<String> questionWords = tokens(e.text);
            Set<String> answerWords = tokens(answer);
            int score = 0;
            for (String term : topic) {
                if (questionWords.contains(term)) score += 3;
                else if (answerWords.contains(term)) score += 1;
            }
            if (score > 0) candidates.add(new Turn(turnIndex, e.text, answer, score));
        }
        candidates.sort(Comparator.<Turn>comparingInt(t -> t.score).reversed()
                .thenComparing(Comparator.comparingInt((Turn t) -> t.eventIndex).reversed()));
        ArrayList<Turn> selected = new ArrayList<>();
        for (Turn turn : candidates) {
            if (selected.size() >= maxTurns) break;
            selected.add(turn);
        }
        selected.sort(Comparator.comparingInt(t -> t.eventIndex));
        return selected;
    }
}
