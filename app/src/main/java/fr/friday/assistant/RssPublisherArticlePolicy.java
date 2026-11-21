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

    public static String publisherDomain(String publisher, String title) {
        // Pour ne pas inventer de lien, le domaine doit correspondre a une marque
        // nommee explicitement par le flux ou dans le titre.
        String name = VoiceText.normalize((publisher == null ? "" : publisher)
                + " " + (title == null ? "" : title));
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
        String s = VoiceText.normalize(strippedTitle(title))
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
