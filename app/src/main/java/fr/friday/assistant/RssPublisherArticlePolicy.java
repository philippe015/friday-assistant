package fr.friday.assistant;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Domaine public de l'éditeur réellement identifié dans le titre ou le RSS.
 * Les résultats Bing sont des pistes ; le domaine et le titre sont vérifiés.
 */
public final class RssPublisherArticlePolicy {
    private RssPublisherArticlePolicy() {}

    public static boolean googleRssUrl(String url) {
        try {
            String host = URI.create(url).getHost();
            return host != null && (host.equalsIgnoreCase("news.google.com"));
        } catch (Exception malformed) { return false; }
    }

    /**
     * Google News RSS donne habituellement l'adresse d'origine dans
     * <source url="https://editeur.example">. Cette métadonnée fiable comme
     * piste ne constitue PAS la preuve d'un article : le résultat de recherche
     * doit encore appartenir à ce domaine et correspondre au titre.
     *
     * Ne transforme jamais cette URL en requête HTTP. Le navigateur HTTPS
     * vérifie séparément l'hôte public, les redirections et le contenu.
     */
    public static String domainFromRssSource(String itemXml) {
        if (itemXml == null || itemXml.isEmpty()) return "";
        java.util.regex.Matcher source = java.util.regex.Pattern.compile(
                "(?is)<source\\b[^>]*?\\burl\\s*=\\s*([\"'])(.*?)\\1")
                .matcher(itemXml);
        if (!source.find()) return "";
        String raw = source.group(2).trim().replace("&amp;", "&");
        try {
            URI uri = URI.create(raw);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("https")
                    && !scheme.equalsIgnoreCase("http"))
                    || uri.getUserInfo() != null || uri.getPort() != -1)
                return "";
            String host = uri.getHost();
            if (host == null) return "";
            // Normalise seulement un nom de domaine, pas un chemin du flux RSS.
            String safe = WebPageReader.checkedUri("https://" + host + "/")
                    .getHost().toLowerCase(Locale.ROOT);
            return safe.startsWith("www.") ? safe.substring(4) : safe;
        } catch (Exception invalid) {
            return "";
        }
    }

    public static String publisherDomain(String publisher, String title) {
        // Pour ne pas inventer de lien, le domaine doit correspondre a une marque
        // nommee explicitement par le flux ou dans le titre.
        String name = VoiceText.normalize((publisher == null ? "" : publisher)
                + " " + (title == null ? "" : title));
        if (name.matches("(?s).*(?:\\bbfm\\b|bfmtv|rmc sport).*")) return "bfmtv.com";
        if (name.contains("ouest-france")) return "ouest-france.fr";
        if (name.contains("france 3") || name.contains("france3")
                || name.contains("france tv info")) return "franceinfo.fr";
        if (name.contains("franceinfo")) return "franceinfo.fr";
        if (name.contains("bbc")) return "bbc.com";
        if (name.contains("the guardian") || name.contains("guardian")) return "theguardian.com";
        if (name.contains("al jazeera")) return "aljazeera.com";
        if (name.contains("deutsche welle")) return "dw.com";
        if (name.contains("tagesschau")) return "tagesschau.de";
        if (name.contains("nhk")) return "nhk.or.jp";
        return "";
    }

    /** Les titres « DIRECT. » du flux renvoient parfois à un fil en continu
     * dont le titre global est différent de l'entrée Google News. */
    public static boolean isLiveHeadline(String title) {
        String t = VoiceText.normalize(title == null ? "" : title).trim();
        return t.matches("^(?:direct|en direct|live)[. :!–-].*")
                || t.matches("^suivez en direct\\b.*");
    }

    /** Public keywords only; no profile, GPS, history, or private conversation. */
    public static String shortPublisherQuery(String title, String domain) {
        String safeTitle = VoiceText.normalize(strippedTitle(title == null ? "" : title))
                .replaceFirst("^(?:direct|en direct|live)[. :!–-]+", "")
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ").trim();
        String[] tokens = safeTitle.split(" ");
        StringBuilder words = new StringBuilder();
        Set<String> used = new HashSet<>();
        for (String token : tokens) {
            if (token.length() < 3 || token.matches("^(?:direct|suivez|live|journal|"
                    + "pour|dans|avec|depuis|info|infos|comme|jamais|avoir|"
                    + "dit|dites|etre|est|sont|pas|des|les|une|sur|son|"
                    + "aux|par|que|qui|leur|elle|ils|son|cet|cette)$")
                    || !used.add(token)) continue;
            if (words.length() > 0) words.append(' ');
            words.append(token);
            if (used.size() >= 9) break;
        }
        return words.toString() + " site:" + domain;
    }

    /**
     * Find a text excerpt on a public live page that matches this SPECIFIC
     * Google News item; never claim the entire live blog is the article.
     * Matching is bounded to 1600-character windows (not to the full blog).
     * Returns empty on weak or absent evidence.
     */
    public static String relevantLivePassage(String expectedTitle, String publicText) {
        if (expectedTitle == null || publicText == null
                || expectedTitle.isEmpty() || publicText.isEmpty()
                || !isLiveHeadline(expectedTitle)) return "";
        String topic = strippedTitle(expectedTitle)
                .replaceFirst("(?i)^(?:direct|en direct|live)[. :!–-]+", "");
        Set<String> expected = significant(topic);
        if (expected.size() < 5) return "";
        String text = publicText.length() > 36000
                ? publicText.substring(0, 36000) : publicText;
        for (int start = 0; start < text.length(); start += 350) {
            String window = text.substring(start, Math.min(text.length(), start + 1600));
            Set<String> actual = significant(window);
            int common = 0;
            for (String token : expected) if (actual.contains(token)) common++;
            if (common >= 5 && common >= Math.ceil(0.64 * expected.size()))
                return window.trim();
        }
        return "";
    }

    public static String strippedTitle(String title) {
        if (title == null) return "";
        String clean = title.trim();
        int last = clean.lastIndexOf(" - ");
        if (last >= 35) clean = clean.substring(0, last);
        int mdash = clean.lastIndexOf(" — ");
        if (mdash >= 35) clean = clean.substring(0, mdash);
        return clean.length() > 155 ? clean.substring(0, 155) : clean;
    }

    public static boolean publisherHost(String candidateUrl, String domain) {
        try {
            if (domain == null || domain.isEmpty()) return false;
            URI u = WebPageReader.checkedUri(candidateUrl);
            String host = u.getHost().toLowerCase(Locale.ROOT);
            String d = domain.toLowerCase(Locale.ROOT);
            return host.equals(d) || host.endsWith("." + d);
        } catch (Exception invalid) { return false; }
    }

    public static boolean matchingTitle(String expected, String found) {
        Set<String> a = significant(expected), b = significant(found);
        if (a.size() < 4 || b.size() < 4) return false;
        int common = 0;
        for (String word : a) if (b.contains(word)) common++;
        return common >= 4 && common >= Math.ceil(0.65 * Math.min(a.size(), b.size()));
    }

    private static Set<String> significant(String title) {
        Set<String> words = new HashSet<>();
        String s = VoiceText.normalize(title)
                .replaceAll("[^a-z0-9]+"," ");
        for (String token : s.split("\\s+")) {
            if (token.length() >= 4 && !token.matches(
                    "^(?:dans|pour|avec|sans|sont|cette|dont|plus|apres|avant|depuis|"
                    + "elle|elles|tout|tous|leur|leurs|vous|nous|cela|cest|"
                    + "entre|comme|article|actualites|france)$"))
                words.add(token);
        }
        return words;
    }
}
