package fr.friday.assistant;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conservative lexical-semantic lookup (synonyms + token overlap), offline.
 *
 * This is NOT neural embeddings or retraining. Its strict threshold avoids
 * applying a correction to another place, date or numeric identifier.
 * It treats corrections as unverified USER data, not as system commands.
 */
public final class LocalSemanticMemoryPolicy {
    private LocalSemanticMemoryPolicy() { }

    private static final Set<String> STOP = new HashSet<>(Arrays.asList(
            "le","la","les","de","des","du","d","un","une","en","au","aux",
            "a","et","ou","pour","sur","par","dans","avec","est","sont",
            "il","elle","ils","elles","ce","cette","ces","cet","que",
            "qui","quoi","quel","quelle","quels","quelles","me","moi",
            "tu","vous","je","j","nous","ton","ta","tes","mon","ma",
            "mes","notre","nos","svp","s","te","se","fait","faire",
            "donne","dis","peux","peut","pourrais","merci","stp",
            "friday","jarvis","jervis","fraday","explique","concernant"));
    private static final Pattern NUMBER = Pattern.compile("\\b\\d+\\b");

    public static String canonical(String text) {
        String v = Normalizer.normalize(text == null ? "" : text,
                Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.FRENCH);
        // Only unambiguous paraphrases: "weather in Paris" ≈ "what's
        // the weather in Paris"; do not change "time" in other contexts.
        v = v.replaceAll("\\bquel\\s+temps\\s+(?:fait\\s+il|fait\\s+elle|fait)\\b",
                "meteo");
        v = v.replaceAll("\\b(?:bulletin\\s+meteo|previsions?\\s+meteo"
                + "|meteo\\s+actuelle)\\b", "meteo");
        v = v.replaceAll("\\b(?:temperature\\s+exterieure)\\b", "temperature");
        v = v.replaceAll("\\b(?:actualites|nouvelles\\s+actualites)\\b", "actualite");
        v = v.replaceAll("\\b(?:comment\\s+va\\s+la\\s+meteo)\\b", "meteo");
        return v.replaceAll("[^a-z0-9]+", " ").replaceAll("\\s+", " ").trim();
    }

    private static Set<String> tokens(String text) {
        Set<String> tokens = new HashSet<>();
        for (String word : canonical(text).split(" ")) {
            if (word.length() >= 2 && !STOP.contains(word)) tokens.add(word);
        }
        return tokens;
    }

    private static Set<String> numbers(String text) {
        Set<String> n = new HashSet<>();
        Matcher m = NUMBER.matcher(canonical(text));
        while (m.find()) n.add(m.group());
        return n;
    }

    private static boolean sameCriticalQualifiers(Set<String> a, Set<String> b) {
        // Temporal scopes and polarity are never silently merged.
        Set<String> qualifiers = new HashSet<>(Arrays.asList(
                "demain","hier","aujourdhui","maintenant","avant","apres",
                "jamais","toujours","pas","sans","non","avec",
                "matin","soir","nuit","semaine","mois","annee"));
        for (String q : qualifiers)
            if (a.contains(q) != b.contains(q)) return false;
        return true;
    }

    /** 0..1, use only >= 0.86 with >=2 meaningful tokens. */
    public static double similarity(String a, String b) {
        if (a == null || b == null || a.length() > 260 || b.length() > 260)
            return 0.0;
        Set<String> x = tokens(a), y = tokens(b);
        if (x.size() < 2 || y.size() < 2) return 0.0;
        if (!numbers(a).equals(numbers(b))
                || !sameCriticalQualifiers(x, y)) return 0.0;
        int shared = 0;
        for (String token : x) if (y.contains(token)) shared++;
        return shared / (double)Math.max(x.size(), y.size());
    }

    public static boolean similar(String a, String b) {
        return similarity(a, b) >= 0.86;
    }

    public static boolean sameCorrection(String a, String b) {
        return canonical(a).equals(canonical(b));
    }
}
