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
        // RSS Google News renvoie parfois l'application Angular/minified JS au lieu
        // de l'article. Ne jamais envoyer ce code au moteur TTS ou Gemma.
        if (looksLikeExecutableCode(html)) return new Extracted("", false);
        Extracted article = best(ARTICLE, html);
        if (article.text.length() >= 240) return article;
        Extracted body = best(BODY, html);
        if (body.text.length() >= 240) return body;
        Extracted main = best(MAIN, html);
        if (main.text.length() >= 240) return main;
        // L'absence de balise <article> ne prouve pas que la page est vide.
        // Le lecteur doit indiquer qu'il s'agit du texte disponible de la page.
        String fallback = WebPageReader.plainText(html);
        return looksLikeExecutableCode(fallback)
                ? new Extracted("", false) : new Extracted(fallback, false);
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
        return looksLikeExecutableCode(best)
                ? new Extracted("", false) : new Extracted(best, !best.isEmpty());
    }

    /** Non-content minified code seen in real v3.14.4 Google News diagnostics. */
    public static boolean looksLikeExecutableCode(String text) {
        if (text == null || text.isEmpty()) return false;
        String head = text.substring(0, Math.min(text.length(), 2200)).trim();
        String low = head.toLowerCase(java.util.Locale.ROOT);
        if (low.startsWith("\"use strict\";") || low.startsWith("'use strict';")
                || low.startsWith("javascript:")
                || low.contains("this.default_dotssplashui")
                || low.contains("copyright the closure library")
                || low.contains("webpackjsonp")
                || low.contains("function(_){var window=this")) return true;
        if (low.contains("function(") && low.contains("var ") && low.contains("this.")
                && low.contains("=>")) return true;
        // Huge unbroken identifiers/operators should not become spoken prose.
        if (head.length() >= 800 && head.matches("(?s).*[A-Za-z_$][A-Za-z0-9_$]{300,}.*"))
            return true;
        return false;
    }
}
