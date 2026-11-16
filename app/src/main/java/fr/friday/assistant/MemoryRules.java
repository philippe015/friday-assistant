package fr.friday.assistant;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Recherche locale sans réseau, sans moteur de langage et sans journalisation. */
public final class MemoryRules {
    private MemoryRules() {}

    private static final Set<String> IGNORED = new HashSet<>(Arrays.asList(
            "comment", "quelles", "quelle", "quels", "quel", "quoi", "que", "qui",
            "pourquoi", "dans", "avec", "pour", "plus", "moins", "nous", "vous",
            "notre", "votre", "mes", "mon", "ma", "moi", "mes", "sur", "une", "des",
            "les", "aux", "est", "sont", "etre", "avoir", "fait", "faire", "sais",
            "savoir", "peux", "peut", "veux", "cherche", "recherche", "montre",
            "memoire", "memorise", "friday", "assistant", "souvenir", "notes",
            "information", "informations", "parle", "concernant", "propos"));

    public static String normalize(String value) {
        String text = Normalizer.normalize(value == null ? "" : value,
                Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.FRENCH).replaceAll("[^a-z0-9]+", " ")
                .replaceAll("\\s+", " ").trim();
        return text;
    }

    /** Prefixe facultatif : [projet] tâche à retenir. */
    public static String[] parseLine(String line) {
        String input = line == null ? "" : line.trim().replaceAll("\\s+", " ");
        String category = "Général";
        if (input.startsWith("[")) {
            int close = input.indexOf(']');
            if (close >= 2 && close <= 32) {
                category = input.substring(1, close).trim();
                input = input.substring(close + 1).trim();
            }
        }
        if (category.length() > 30 || !category.matches("[\\p{L}0-9 _'-]{1,30}"))
            throw new IllegalArgumentException("Catégorie invalide");
        if (input.isEmpty() || input.length() > 600)
            throw new IllegalArgumentException("Note vide ou supérieure à 600 caractères");
        return new String[]{category, input};
    }

    public static String format(String category, String content) {
        return "Général".equals(category) ? content : "[" + category + "] " + content;
    }

    public static String duplicateKey(String category, String content) {
        return normalize(category) + "|" + normalize(content);
    }

    /** 0 signifie aucun mot significatif présent dans la note. */
    public static int score(String query, String category, String note) {
        String q = normalize(query);
        String text = " " + normalize(category + " " + note) + " ";
        if (q.isEmpty()) return 0;
        int points = 0;
        for (String word : q.split(" ")) {
            if (word.length() < 3 || IGNORED.contains(word)) continue;
            if (text.contains(" " + word + " ")) points += 4;
            else if (word.length() >= 5 && text.matches("(?s).*\\b"
                    + java.util.regex.Pattern.quote(word) + "[a-z0-9]*\\b.*")) points += 2;
        }
        return points;
    }
}
