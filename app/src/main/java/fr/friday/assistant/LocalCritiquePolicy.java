package fr.friday.assistant;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Independent, offline guard for self-critique. A language-model opinion is
 * NOT an external fact check. Arithmetic is verified by the deterministic
 * local parser before any LLM review; uncertain facts remain unverified.
 *
 * Pure Java (no Android runtime) for regression testing with javac.
 */
public final class LocalCritiquePolicy {
    private LocalCritiquePolicy() { }

    public static final class Assessment {
        public final String verdict;
        public final String reason;
        public final String correction;
        public final boolean deterministic;

        Assessment(String verdict, String reason, String correction, boolean deterministic) {
            this.verdict = verdict;
            this.reason = reason;
            this.correction = correction;
            this.deterministic = deterministic;
        }
    }

    private static final Pattern NUMERIC = Pattern.compile(
            "-?\\d[\\d\\s\\u00a0\\u202f.,]*");
    private static final Pattern VERDICT = Pattern.compile(
            "(?im)^\\s*(?:[-*]\\s*)?VERDICT\\s*:\\s*(.*?)\\s*$");
    private static final Pattern REASON = Pattern.compile(
            "(?im)^\\s*(?:[-*]\\s*)?MOTIF\\s*:\\s*(.*?)\\s*$");
    private static final Pattern FIX = Pattern.compile(
            "(?im)^\\s*(?:[-*]\\s*)?CORRECTION\\s*:\\s*(.*?)\\s*$");

    private static String field(String raw, Pattern pattern, int max) {
        Matcher found = pattern.matcher(raw == null ? "" : raw);
        if (!found.find()) return "";
        String text = found.group(1).replaceAll("[\\p{Cntrl}&&[^\\t]]", " ").trim();
        return text.length() > max ? text.substring(0, max).trim() : text;
    }

    /**
     * Exactly a numeric expression: use the same deterministic calculator as
     * normal chat, never infer correctness by asking the same LLM twice.
     * Non-numeric or unparseable expressions are left to a cautious review.
     */
    public static Assessment verifyArithmetic(String question, String answer) {
        if (question == null || answer == null) return null;
        String exact = LocalJarvisArithmetic.tryAnswer(question);
        if (exact == null || !exact.startsWith("Calcul effectué localement sur le téléphone : "))
            return null;
        int colon = exact.indexOf(':');
        if (colon < 0) return null;
        String expected = exact.substring(colon + 1).trim().replaceFirst("\\.$", "");
        // Only integer arithmetic: decimals require a separate exact decimal
        // verification policy rather than approximate floating-point matching.
        if (!expected.matches("-?\\d{1,15}")) return null;

        Matcher values = NUMERIC.matcher(answer);
        String last = "";
        while (values.find()) {
            String token = values.group().trim().replaceAll("[\\s\\u00a0\\u202f.,]", "");
            if (token.matches("-?\\d+")) last = token;
        }
        if (last.isEmpty()) {
            return new Assessment("CALCUL_INCOMPLET",
                    "La réponse ne contient pas de résultat numérique comparable.",
                    "Le résultat exact du calcul local est " + expected + ".",
                    true);
        }
        if (last.equals(expected)) {
            return new Assessment("CALCUL_VERIFIE",
                    "Le dernier résultat numérique correspond au calculateur hors ligne.",
                    "", true);
        }
        return new Assessment("ERREUR_CALCUL",
                "Résultat différent du calculateur local : " + last
                + " au lieu de " + expected + ".",
                "Le résultat exact est " + expected + ".",
                true);
    }

    /**
     * Enforce a conservative vocabulary: a model's 'CORRECT' means only
     * 'no obvious error noticed', NEVER 'externally verified'.
     */
    public static Assessment parseModel(String raw, boolean currentFacts) {
        if (raw == null || raw.trim().isEmpty()) return new Assessment("A_VERIFIER",
                "Le modèle local n'a produit aucune analyse exploitable.", "", false);
        if (raw.length() > 3000) return new Assessment("A_VERIFIER",
                "L'analyse dépasse les limites locales.", "", false);
        String verdict = field(raw, VERDICT, 60).toUpperCase(Locale.ROOT);
        String reason = field(raw, REASON, 220);
        String suggested = field(raw, FIX, 350);
        if (verdict.isEmpty() || reason.isEmpty()) {
            return new Assessment("A_VERIFIER",
                    "L'IA locale n'a pas fourni un diagnostic structuré vérifiable.",
                    "", false);
        }
        if (suggested.equalsIgnoreCase("aucune") ||
                suggested.equalsIgnoreCase("aucune correction") ||
                suggested.toLowerCase(Locale.FRENCH).startsWith("aucune correction fiable")) {
            suggested = "";
        }
        if (!suggested.isEmpty() && LocalLearningPolicy.validCorrection(suggested) == null)
            suggested = "";
        if (currentFacts) {
            // In no case can another local generation prove today's news,
            // prices, weather, current laws or fresh facts.
            return new Assessment("SOURCES_A_VERIFIER",
                    "Information évolutive : vérification indépendante nécessaire. "
                    + reason, "", false);
        }
        if (verdict.contains("ERREUR") || verdict.contains("ANOMALIE")) {
            return new Assessment("ANOMALIE_POSSIBLE",
                    "Signalement du modèle local, non confirmé : " + reason,
                    suggested, false);
        }
        if (verdict.contains("INCERTAIN") || verdict.contains("VERIFIER") ||
                verdict.contains("DOUTE")) {
            return new Assessment("A_VERIFIER",
                    "L'IA locale ne peut pas confirmer : " + reason,
                    "", false);
        }
        if (verdict.contains("CORRECT") || verdict.contains("OK")) {
            return new Assessment("AUCUNE_ANOMALIE_IDENTIFIEE",
                    "Aucune anomalie évidente signalée (pas une preuve d'exactitude). "
                    + reason, "", false);
        }
        return new Assessment("A_VERIFIER",
                "Verdict IA non reconnu : contrôle indépendant nécessaire.",
                "", false);
    }
}
