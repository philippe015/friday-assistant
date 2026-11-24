package fr.friday.assistant;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Fixed, PUBLIC and offline question set. This is a smoke benchmark,
 * not a validated intelligence or IQ assessment. No user dialogue.
 * No answer is embedded in any prompt.
 */
public final class LocalBenchmarkPolicy {
    private LocalBenchmarkPolicy() {}

    public static final class Challenge {
        public final String id;
        public final String category;
        public final String question;
        public final String criterion;
        public final boolean automatic;

        private Challenge(String id, String category, String question,
                          String criterion, boolean automatic) {
            this.id = id;
            this.category = category;
            this.question = question;
            this.criterion = criterion;
            this.automatic = automatic;
        }
    }

    private static final Challenge[] CASES = {
        new Challenge("arith1", "Calcul", "Combien font 17 multiplié par 23 ?",
                "391", true),
        new Challenge("arith2", "Calcul", "Combien font 84 divisé par 7 ?",
                "12", true),
        new Challenge("suite", "Logique", "Complète la suite : 3, 6, 12, 24. Quel nombre vient ensuite ?",
                "48", true),
        new Challenge("capitale", "Connaissance", "Quelle est la capitale du Portugal ?",
                "lisbonne", true),
        new Challenge("traduction", "Français", "Traduis « good morning » en français.",
                "bonjour", true),
        new Challenge("antonyme", "Français", "Donne un antonyme courant de « rapide ».",
                "lent", true),
        new Challenge("grammaire", "Français", "Corrige cette phrase : « Les enfants joue dans le jardin ».",
                "les enfants jouent", true),
        new Challenge("python", "Programmation", "Écris une courte fonction Python qui additionne deux nombres.",
                "def + return", true),
        new Challenge("nuance", "Explication", "Explique la différence entre météo et climat en deux phrases.",
                "Évaluation manuelle", false),
        new Challenge("prudence", "Raisonnement", "Une question ne donne pas assez de contexte : explique comment demander une précision utile.",
                "Évaluation manuelle", false)
    };

    public static int count() { return CASES.length; }
    public static Challenge item(int index) { return CASES[index]; }

    public static String normalized(String text) {
        return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.FRENCH)
                .replaceAll("[^a-z0-9]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean word(String normalized, String value) {
        return Pattern.compile("(?:^| )" + Pattern.quote(value) + "(?: |$)")
                .matcher(normalized).find();
    }

    /** Positive results are INDICATORS, not proof of correct reasoning. */
    public static String evaluate(int index, String answer) {
        Challenge c = item(index);
        if (answer == null || answer.trim().isEmpty()) return "SANS_REPONSE";
        if (!c.automatic) return "LECTURE_MANUELLE";
        String a = normalized(answer);
        if (a.length() < 2) return "NON_CONFIRME";
        boolean match;
        switch (c.id) {
            case "python":
                match = word(a,"def") && word(a,"return");
                break;
            case "antonyme":
                match = word(a,"lent") || word(a,"lente") || word(a,"lents")
                        || word(a,"lentes") || word(a,"lentement");
                break;
            default:
                match = a.contains(c.criterion)
                    && (c.criterion.contains(" ") || word(a,c.criterion));
        }
        return match ? "INDICE_POSITIF" : "NON_CONFIRME";
    }

    public static String prompt(int index) {
        Challenge c = item(index);
        return "Test hors ligne sur appareil. Réponds en français à la question, "
                + "sans consulter Internet et sans reproduire la question. "
                + "Tu peux signaler une incertitude. "
                + "N'invente aucune source. Réponse courte, 1 à 4 phrases.\n"
                + "Question : " + c.question + "\nRéponse :";
    }
}
