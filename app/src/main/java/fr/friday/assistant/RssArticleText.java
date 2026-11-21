package fr.friday.assistant;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extrait le corps lisible d'un article public, sans exécuter du JavaScript
 * ni prétendre récupérer les passages masqués/payants.
 */
public final class RssArticleText {
    private RssArticleText() {}
    private static final Pattern ARTICLE = Pattern.compile(
            "(?is)<article\\b[^>]*>(.*?)</article\\s*>");
    private static final Pattern MAIN = Pattern.compile(
            "(?is)<main\\b[^>]*>(.*?)</main\\s*>");
    private static final Pattern BODY = Pattern.compile(
            "(?is)<(?:div|section)\\b[^>]*(?:itemprop\\s*=\\s*[\"']articleBody[\"']"
            + "|class\\s*=\\s*[\"'][^\"']*(?:article-body|story-body|entry-content|post-content)"
            + "[^\"']*[\"'])[^>]*>(.*?)</(?:div|section)\\s*>");

    public static final class Extracted {
        public final String text;
        public final boolean detected;
        Extracted(String text, boolean detected) {
            this.text = text; this.detected = detected;
        }
    }

    public static Extracted extract(String html) {
        if (html == null || html.trim().isEmpty()) return new Extracted("", false);
        Extracted article = best(ARTICLE, html);
        if (article.text.length() >= 240) return article;
        Extracted body = best(BODY, html);
        if (body.text.length() >= 240) return body;
        Extracted main = best(MAIN, html);
        if (main.text.length() >= 240) return main;
        // L'absence de balise <article> ne prouve pas que la page est vide.
        // Le lecteur doit indiquer qu'il s'agit du texte disponible de la page.
        return new Extracted(WebPageReader.plainText(html), false);
    }

    private static Extracted best(Pattern pattern, String html) {
        Matcher matcher = pattern.matcher(html);
        String best = "";
        int examined = 0;
        while (matcher.find() && examined++ < 12) {
            String fragment = matcher.group(1);
            fragment = fragment.replaceAll(
                    "(?is)<(?:aside|header|footer|nav|form)\\b[^>]*>.*?</(?:aside|header|footer|nav|form)\\s*>", " ");
            String extracted = WebPageReader.plainText(fragment);
            if (extracted.length() > best.length()) best = extracted;
        }
        return new Extracted(best, !best.isEmpty());
    }
}
