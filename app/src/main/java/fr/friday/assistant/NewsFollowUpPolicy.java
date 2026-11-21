package fr.friday.assistant;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Contextual follow-up about PUBLIC news titles only.
 * The full local/private conversation is never sent to search engines.
 * No network, Android or storage dependency: testable with javac.
 */
public final class NewsFollowUpPolicy {
    private NewsFollowUpPolicy() {}

    private static final Pattern SOURCE = Pattern.compile(
        "(?m)^\\[(\\d+)\\]\\s*(.{8,150}?)\\s+—\\s+"
      + "(?:source secondaire|domaine institutionnel)\\s+—\\s+"
      + "horodatage RSS[^\\n]*?—\\s+(https://news\\.google\\.com/rss/articles/[^\\s]+)\\s*$");
    private static final Pattern FOLLOWUP = Pattern.compile(
        "^(?:(?:donne|donnez|dis|dites|apporte|apportez|peux tu donner|"
      + "peux tu apporter|pourrais tu donner)\\s+)?"
      + "(?:(?:moi|nous)\\s+)?(?:plus de |davantage de |d autres? )?"
      + "(?:precisions?|details?|informations?|infos?|explications?)\\s+"
      + "(?:sur|concernant|a propos de)\\b.*"
      + "|^(?:peux tu|pourrais tu|est ce que tu peux)\\s+"
      + "(?:preciser|detailler|approfondir|expliquer)\\b.*"
      + "|^(?:et|parle moi de)\\s+(?:cette|ces|cet)\\s+"
      + "(?:attaque|attaques|incident|incidents|drones?|evenement|evenements)\\b.*");

    private static final String COMMON =
        " l le les la de des d du un une a au aux en dans pour par sur contre "
      + " concernant maintenant aujourd hui derniere dernieres dernier derniers "
      + " armee detecte indique selon et fait aux avec plus infos informations "
      + " explique details precisions nouvelles actualites attaquees ";

    private static String fold(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.FRENCH)
                .replaceAll("[^a-z0-9]+", " ").replaceAll("\\s+", " ").trim();
    }

    public static boolean isPublicNewsFollowUp(String question) {
        String normalized = fold(question);
        return normalized.length() >= 12 && normalized.length() < 220
                && FOLLOWUP.matcher(normalized).matches();
    }

    /** Requires immediate previous question with a timed news request
     * and immediate previous answer with real PUBLIC RSS news links.
     * No private notes, arbitrary answer text or conversation content
     * are appended to the returned public search phrase. */
    public static String enrich(String question, String previousUser,
                                String previousAssistant) {
        if (!isPublicNewsFollowUp(question)
                || RecentNewsPolicy.newsWindowHours(previousUser) <= 0
                || previousAssistant == null
                || !previousAssistant.contains("Sources Internet consultées :")) {
            return question;
        }
        String current = fold(question);
        boolean drone = current.matches(".*\\bdrones?\\b.*");
        boolean attack = current.matches(".*\\b(?:attaques?|frappes?|bombardements?)\\b.*");
        // Ne jamais choisir le premier titre au hasard lorsque l'utilisateur
        // précise un sujet. Comparer au moins un terme de la question.
        String subject = current.replaceFirst(
                "^.*?\\b(?:sur|concernant|a propos de)\\b\\s*", "");
        LinkedHashSet<String> requestedTerms = new LinkedHashSet<>();
        for (String word : subject.split(" ")) {
            if (word.length() < 4 || COMMON.contains(" " + word + " ")) continue;
            requestedTerms.add(word.endsWith("s") ? word.substring(0, word.length() - 1) : word);
        }
        String bestTitle = "";
        Matcher m = SOURCE.matcher(previousAssistant);
        while (m.find()) {
            String title = m.group(2);
            String topic = fold(title);
            if (drone && !topic.matches(".*\\bdrones?\\b.*")) continue;
            if (attack && !topic.matches(".*\\b(?:attaques?|frappes?|bombardements?)\\b.*"))
                continue;
            if (!requestedTerms.isEmpty()) {
                boolean match = false;
                for (String term : requestedTerms) {
                    if (topic.contains(term)) { match = true; break; }
                }
                if (!match) continue;
            }
            bestTitle = title;
            break;
        }
        if (bestTitle.isEmpty()) return question;
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        for (String word : fold(bestTitle).split(" ")) {
            if (word.length() < 4 || COMMON.contains(" " + word + " ")) continue;
            keywords.add(word);
            if (keywords.size() >= 7) break;
        }
        // A real news headline must contain enough public search terms.
        if (keywords.size() < 3) return question;
        int hours = RecentNewsPolicy.requestedHours(question);
        if (hours <= 0) hours = RecentNewsPolicy.newsWindowHours(previousUser);
        StringBuilder query = new StringBuilder("actualités ");
        for (String word : keywords) query.append(word).append(' ');
        query.append("dans les ").append(hours).append(" dernières heures");
        return query.toString().trim();
    }
}
