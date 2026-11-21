package fr.friday.assistant;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Sépare la matière recherchée des instructions (« aujourd'hui », « cite les
 * sources », « publiées »). Java pur, sans réseau ou données personnelles.
 */
public final class NewsTopicPolicy {
    private NewsTopicPolicy() { }

    private static String fold(String input) {
        return Normalizer.normalize(input == null ? "" : input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.FRENCH)
                .replaceAll("[^a-z0-9 ]+", " ").replaceAll("\\s+", " ").trim();
    }

    private static final Pattern SCIENCE = Pattern.compile(
            "\\b(?:scientifiques?|sciences?|scientific|decouvertes? scientifiques?)\\b");

    private static final Pattern INSTRUCTION = Pattern.compile(
            "\\b(?:jarvis|friday|fridai|fridey|freday|cherche|recherche|trouve|montre|"
            + "donne|fais|resume|parle|moi|nous|tu|peux|pourrais|voudrais|"
            + "actualites?|actus?|nouvelles?|infos?|informations?|news|"
            + "titres?|journal|flash|evenements?|"
            + "dernieres?|dernier|precedentes?|precedents?|recents?|recentes?|"
            + "aujourd|hui|jour|matin|soir|actuelles?|nouveautes?|"
            + "publiees?|publies?|publication|parues?|parus?|articles?|"
            + "cite[rs]?|citation|references?|sources?|liens?|urls?|medias?|"
            + "d|l|un|une|des?|de|du|la|le|les|sur|au|aux|en|dans|pendant|cours|"
            + "heures?|[0-9]{1,2}|sept|six|douze|trois|quatre|vingt|"
            + "quelles?|quels?|sont|et|a|avec|depuis|cette|ce)\\b");

    public static String topic(String question) {
        String folded = fold(question);
        boolean science = SCIENCE.matcher(folded).find();
        String bare = SCIENCE.matcher(INSTRUCTION.matcher(folded).replaceAll(" "))
                .replaceAll(" ").replaceAll("\\s+", " ").trim();
        if (science) return bare.isEmpty() ? "science" : bare + " science";
        return bare;
    }

    /** Vérification supplémentaire pour les flux généralistes non ciblés. */
    public static boolean matchesGeneralFeed(String topic, String title, String summary) {
        if ("science".equals(topic)) {
            String corpus = fold(title + " " + summary);
            return corpus.matches(".*\\b(?:science|scientifique|scientifiques|recherche|"
                    + "nasa|astronomie|astronomique|spatial|spatiale|spatiales|espace|"
                    + "biologie|biologique|physique|physiciens|laboratoire|universite|"
                    + "medecine|medical|medicale|sante|climat|climatique|"
                    + "robotique|intelligence artificielle|satellite|quantique|"
                    + "archeologie|decouverte|fossile|chimie|genetique)\\b.*");
        }
        return WebSearch.relevantResult(topic, title, summary);
    }
}
