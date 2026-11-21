package fr.friday.assistant;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Répertoire extensible de flux RSS publics dans différentes langues.
 * Les URL sont des constantes contrôlées, jamais des données de l'historique
 * personnel. Ne prétend pas énumérer tous les flux RSS de la planète.
 *
 * Un bulletin ne consulte qu'un nombre borné de flux afin de préserver
 * la batterie et la réactivité sur Android.
 */
public final class WorldRssPolicy {
    private WorldRssPolicy() { }

    public static final class Feed {
        public final String name;
        public final String language;
        public final String region;
        public final String url;
        private Feed(String name, String language, String region, String url) {
            this.name = name;
            this.language = language;
            this.region = region;
            this.url = url;
        }
    }

    private static Feed feed(String name, String lang, String region, String url) {
        if (!url.startsWith("https://")) throw new IllegalArgumentException("HTTPS requis");
        return new Feed(name, lang, region, url);
    }

    public static final Feed FR = feed("Franceinfo Monde", "fr", "europe",
            "https://www.francetvinfo.fr/monde.rss");
    public static final Feed BBC = feed("BBC News World", "en", "europe",
            "https://feeds.bbci.co.uk/news/world/rss.xml");
    public static final Feed GUARDIAN = feed("The Guardian World", "en", "europe",
            "https://www.theguardian.com/world/rss");
    public static final Feed ALJAZEERA = feed("Al Jazeera English", "en", "moyen-orient",
            "https://www.aljazeera.com/xml/rss/all.xml");
    public static final Feed DW = feed("Deutsche Welle World", "en", "europe",
            "https://rss.dw.com/rdf/rss-en-world");
    public static final Feed NHK = feed("NHK Japon", "ja", "asie",
            "https://www.nhk.or.jp/rss/news/cat6.xml");
    public static final Feed DE = feed("Tagesschau Allemagne", "de", "europe",
            "https://www.tagesschau.de/xml/rss2/");
    public static final Feed ES = feed("Google Actualités Espagne", "es", "europe",
            "https://news.google.com/rss?hl=es&gl=ES&ceid=ES:es");
    public static final Feed BR = feed("Google Actualités Brésil", "pt", "amerique-latine",
            "https://news.google.com/rss?hl=pt-BR&gl=BR&ceid=BR:pt-419");
    public static final Feed JP = feed("Google Actualités Japon", "ja", "asie",
            "https://news.google.com/rss?hl=ja&gl=JP&ceid=JP:ja");
    public static final Feed AR = feed("Google Actualités Monde arabe", "ar", "moyen-orient",
            "https://news.google.com/rss?hl=ar&gl=SA&ceid=SA:ar");
    public static final Feed IN = feed("Google Actualités Inde", "hi", "asie",
            "https://news.google.com/rss?hl=hi&gl=IN&ceid=IN:hi");
    public static final Feed US = feed("Google News États-Unis", "en", "amerique-du-nord",
            "https://news.google.com/rss?hl=en-US&gl=US&ceid=US:en");
    public static final Feed ZA = feed("Google News Afrique du Sud", "en", "afrique",
            "https://news.google.com/rss?hl=en-ZA&gl=ZA&ceid=ZA:en");
    public static final Feed IT = feed("Google Notizie Italie", "it", "europe",
            "https://news.google.com/rss?hl=it&gl=IT&ceid=IT:it");
    public static final Feed CA = feed("Google News Canada", "en", "amerique-du-nord",
            "https://news.google.com/rss?hl=en-CA&gl=CA&ceid=CA:en");
    public static final Feed AU = feed("Google News Australie", "en", "oceanie",
            "https://news.google.com/rss?hl=en-AU&gl=AU&ceid=AU:en");

    public static final List<Feed> CATALOGUE = Collections.unmodifiableList(Arrays.asList(
            FR, BBC, GUARDIAN, ALJAZEERA, DW, NHK, DE, ES, BR, JP,
            AR, IN, US, ZA, IT, CA, AU));

    private static String fold(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.FRENCH)
                .replaceAll("[^a-z0-9]+", " ").replaceAll("\\s+", " ").trim();
    }

    /** Cible uniquement les demandes de nouvelles mondiales, pas les sujets
     * privés, ni les questions sur la météo, ni les détails d'un conflit. */
    public static boolean isWorldNews(String question) {
        if (question == null || question.length() > 500
                || !RecentNewsPolicy.isNews(question)) return false;
        String q = fold(question);
        if (q.matches(".*\\b(?:scientifiqu|medicin|sante personnelle|"
                + "frappes?|bombardements?|drones?|ssiap)\\w*\\b.*"))
            return false;
        return q.matches(".*\\b(?:monde|mondial(?:es|e|s)?|international(?:es|e|s)?|"
                + "planete|etranger|afrique|africain(?:e|s)?|asie|asiatiqu\\w*|"
                + "europe|europeenn?\\w*|amerique|americain\\w*|"
                + "etats unis|usa|canada|bresil|espagne|allemagne|japon|inde|"
                + "chine|australie|italie|royaume uni|angleterre|"
                + "moyen orient|proche orient|arab\\w*|"
                + "portugal|portugais\\w*|russie|ukraine)\\b.*");
    }

    /** Environ 6 flux par demande ; les 17 flux du catalogue restent
     * disponibles comme sources ou fournisseurs de repli. */
    public static List<Feed> select(String question) {
        String q = fold(question);
        if (!isWorldNews(question)) return Collections.emptyList();
        if (q.matches(".*\\b(?:etats unis|usa|americain\\w*|amerique du nord)\\b.*"))
            return Arrays.asList(US, BBC, CA, FR);
        if (q.matches(".*\\b(?:canada)\\b.*"))
            return Arrays.asList(CA, US, BBC, FR);
        if (q.matches(".*\\b(?:bresil|amerique latine|portugais\\w*|portugal)\\b.*"))
            return Arrays.asList(BR, ES, BBC, FR);
        if (q.matches(".*\\b(?:espagne)\\b.*"))
            return Arrays.asList(ES, FR, BBC, GUARDIAN);
        if (q.matches(".*\\b(?:allemagne)\\b.*"))
            return Arrays.asList(DE, DW, FR, BBC);
        if (q.matches(".*\\b(?:japon)\\b.*"))
            return Arrays.asList(NHK, JP, BBC, FR);
        if (q.matches(".*\\b(?:inde)\\b.*"))
            return Arrays.asList(IN, BBC, FR, ALJAZEERA);
        if (q.matches(".*\\b(?:afrique|africain\\w*)\\b.*"))
            return Arrays.asList(ZA, ALJAZEERA, BBC, FR);
        if (q.matches(".*\\b(?:moyen orient|proche orient|arab\\w*)\\b.*"))
            return Arrays.asList(AR, ALJAZEERA, BBC, FR);
        if (q.matches(".*\\b(?:australie|oceanie)\\b.*"))
            return Arrays.asList(AU, BBC, FR, GUARDIAN);
        if (q.matches(".*\\b(?:italie)\\b.*"))
            return Arrays.asList(IT, FR, BBC, DW);
        if (q.matches(".*\\b(?:europe|europeenn?\\w*)\\b.*"))
            return Arrays.asList(FR, BBC, DE, ES, IT, GUARDIAN);
        // Une demande mondiale couvre délibérément plusieurs continents.
        return Arrays.asList(FR, BBC, ES, BR, JP, AR, ZA);
    }

    public static String normalizedLanguage(String code) {
        if (code == null || code.isEmpty()) return "fr";
        switch (code) {
            case "en": case "es": case "de": case "ja": case "pt":
            case "ar": case "hi": case "it": case "zh": case "ru":
                return code;
            default: return "fr";
        }
    }
}
