package fr.friday.assistant;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Commande explicite de lecture de l'article RSS sélectionné, sans LLM distant. */
public final class RssArticleIntent {
    private RssArticleIntent() {}
    private static final Pattern ARTICLE = Pattern.compile("\\b(?:article|articles|page|pages)\\b");
    private static final Pattern ACTION = Pattern.compile(
            "\\b(?:lis|lire|lecture|raconte|raconter|ouvre|ouvrir|detail|details|"
            + "contenu|integral|integrale|entier|entiere|entierement|complet|complete)\\b");
    private static final Pattern AFTER = Pattern.compile(
            "\\b(?:article|page)\\s+(?:(?:numero|n|no)\\s*)?(\\d{1,2}|"
            + "premier|premiere|deuxieme|second|seconde|troisieme|quatrieme|"
            + "cinquieme|sixieme|septieme)\\b");
    private static final Pattern BEFORE = Pattern.compile(
            "\\b(premier|premiere|deuxieme|second|seconde|troisieme|"
            + "quatrieme|cinquieme|sixieme|septieme)\\s+article\\b");

    public static boolean isRequest(String question) {
        String normalized = VoiceText.normalize(question);
        return ARTICLE.matcher(normalized).find() && ACTION.matcher(normalized).find()
                && !normalized.matches(".*\\b(?:code penal|code civil|loi|juridique|"
                + "legislatif|constitution)\\b.*");
    }

    public static int index(String question) {
        String normalized = VoiceText.normalize(question);
        Matcher after = AFTER.matcher(normalized);
        if (after.find()) return number(after.group(1));
        Matcher before = BEFORE.matcher(normalized);
        return before.find() ? number(before.group(1)) : 1;
    }

    private static int number(String word) {
        if (word.matches("\\d{1,2}")) {
            int n = Integer.parseInt(word);
            return n >= 1 && n <= 7 ? n : -1;
        }
        switch (word) {
            case "premier": case "premiere": return 1;
            case "deuxieme": case "second": case "seconde": return 2;
            case "troisieme": return 3;
            case "quatrieme": return 4;
            case "cinquieme": return 5;
            case "sixieme": return 6;
            case "septieme": return 7;
            default: return -1;
        }
    }
}
