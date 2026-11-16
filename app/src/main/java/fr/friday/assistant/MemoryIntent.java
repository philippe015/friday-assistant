package fr.friday.assistant;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Intentions explicites de mémoire. Testable sans Android. */
public final class MemoryIntent {
    public static final int NONE = 0, SAVE = 1, READ = 2, CLEAR = 3, SEARCH = 4;
    private static final Pattern SAVE_PATTERN = Pattern.compile(
        "^(?:retiens|memorise|souviens.toi|garde en memoire|note pour plus tard)\\s+(?:que\\s+)?(.+)$",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private MemoryIntent() {}
    private static String normalize(String s) {
        return Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "").toLowerCase(Locale.FRENCH).trim();
    }
    public static int kind(String input) {
        String s = normalize(input).replaceAll("[?!.,;:]+$", "").trim();
        if (s.matches(".*(?:efface (?:toute )?ma memoire|oublie toutes mes notes|oublie tout ce que tu as (?:retenu|memorise)).*")) return CLEAR;
        if (s.matches("^(?:cherche|recherche|retrouve) (?:dans )?(?:ma |mes )?(?:memoire|notes) (?:sur |concernant |a propos de )?.+")) return SEARCH;
        if (s.matches(".*(?:qu.as.tu (?:memorise|retenu)|que (?:sais.tu|te rappelles.tu) de moi|liste (?:mes )?(?:souvenirs|notes)|montre (?:ma )?memoire).*")) return READ;
        return SAVE_PATTERN.matcher(s).matches() ? SAVE : NONE;
    }
    public static String searchQuery(String input) {
        String s = normalize(input).replaceAll("[?!.,;:]+$", "").trim();
        return s.replaceFirst("^(?:cherche|recherche|retrouve) (?:dans )?(?:ma |mes )?(?:memoire|notes) (?:sur |concernant |a propos de )?", "").trim();
    }
    public static String fact(String input) {
        if (input == null) return "";
        String original = input.trim();
        String norm = normalize(original);
        Matcher match = SAVE_PATTERN.matcher(norm);
        if (!match.matches()) return "";
        // Les accents changent la longueur ; on prélève après le préfixe depuis l'original.
        String removed = original.replaceFirst("(?iu)^(?:retiens|m[ée]morise|souviens[- ]toi|garde en m[ée]moire|note pour plus tard)\\s+(?:que\\s+)?", "");
        return removed.equals(original) ? "" : removed.trim().replaceAll("[\\s]+", " ");
    }
}
