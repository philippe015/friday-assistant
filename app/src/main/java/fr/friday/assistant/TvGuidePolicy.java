package fr.friday.assistant;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Small, conservative TV-guide extractor for the public French TNT schedule.
 * A TV homepage is not proof of actual programs: require a day-specific
 * 20-22h schedule and time/channel/title before reporting a broadcast.
 * No API keys, cookies, HTML execution, third-party data or Web search engine.
 */
public final class TvGuidePolicy {
    private TvGuidePolicy() {}

    public static final String GUIDE_URL = "https://www.programme-tv.net/";
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    private static final Pattern CHANNEL = Pattern.compile(
        "(?i)\\bN\\s*[°º]\\s*\\d{1,2}\\s+"
      + "(TF1|France\\s+[2345]|M6|Arte|W9|TMC|TFX|LCP|Gulli|"
      + "TF1 Series Films|RMC Story|RMC Decouverte|6ter|Chérie 25|"
      + "NRJ\\s*12|BFM TV|CNews)\\b");
    private static final Pattern HOUR = Pattern.compile("\\b(20|21|22)h([0-5][0-9])\\b");
    private static final Pattern GENRE = Pattern.compile(
        "(?i)\\b(?:Cinema|Cinéma|Serie TV|Série TV|Telefilm|Téléfilm|"
      + "Divertissement|Culture Infos|Culture-infos|Sport|"
      + "Évènement TV|Evenement TV|Autre|Jeunesse|Documentaire)\\b");
    private static final Pattern STOP_TAG = Pattern.compile(
        "(?i)\\b(?:Coup de Télé-Loisirs|Nos membres adorent|Lire le résumé|"
      + "Inédit|Rediffusion)\\b");

    private static String fold(String input) {
        return Normalizer.normalize(input == null ? "" : input, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "").toLowerCase(Locale.FRENCH)
            .replaceAll("[^a-z0-9 ]+", " ").replaceAll("\\s+", " ").trim();
    }

    public static boolean isRequest(String question) {
        String q = fold(question);
        if (q.length() < 9 || q.length() > 220) return false;
        boolean schedule = q.matches(".*\\b(?:programme|programmes|grille|diffusion|"
                + "chaine|chaines)\\b.*")
                && q.matches(".*\\b(?:tele|television|tv|tnt|film|films|"
                + "soiree|ce soir|diffusion)\\b.*");
        boolean eveningMovie = q.matches(".*\\b(?:films?|cinema)\\b.*")
                && q.matches(".*\\b(?:ce soir|a la tele|a la television|a la tv|"
                + "passent|passe ce soir|diffuse ce soir)\\b.*");
        return schedule || eveningMovie;
    }

    public static boolean filmsOnly(String question) {
        String q = fold(question);
        return q.matches(".*\\b(?:film|films|cinema)\\b.*");
    }

    public static boolean asksOtherCountry(String question) {
        String q = fold(question);
        return q.matches(".*\\b(?:belgique|suisse|canada|quebec|luxembourg|"
                + "royaume uni|angleterre|espagne|allemagne|italie|"
                + "etats unis|maroc|algerie|tunisie)\\b.*");
    }

    /** The guide date must match the current date in metropolitan France. */
    public static String expectedDate(long epochMillis) {
        LocalDate day = Instant.ofEpochMilli(epochMillis).atZone(PARIS).toLocalDate();
        return fold(day.format(DateTimeFormatter.ofPattern("d MMMM", Locale.FRENCH)));
    }

    public static boolean confirmsEveningDate(String pageText, long epochMillis) {
        String text = fold(pageText);
        String day = expectedDate(epochMillis);
        // Source contains its own programme/date selector. Only accept a
        // matching day immediately followed by the 20-22 h time window.
        return text.contains(day + " de 20h a 22h");
    }

    public static final class Entry {
        public final String channel;
        public final String hour;
        public final String title;
        public final boolean movie;
        private Entry(String channel, String hour, String title, boolean movie) {
            this.channel = channel; this.hour = hour; this.title = title;
            this.movie = movie;
        }
        public String display() {
            return channel + " à " + hour + " : " + title
                    + (movie ? " (cinéma)" : "");
        }
    }

    /** Only a verified same-day page produces listings; never infer times. */
    public static List<Entry> extract(String pageText, boolean movies,
                                      long epochMillis) {
        ArrayList<Entry> result = new ArrayList<>();
        if (pageText == null || !confirmsEveningDate(pageText, epochMillis))
            return result;
        Matcher channels = CHANNEL.matcher(pageText);
        ArrayList<Integer> starts = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();
        while (channels.find()) {
            starts.add(channels.end());
            names.add(channels.group(1).replaceAll("\\s+", " ").trim());
        }
        for (int i = 0; i < starts.size() && result.size() < 6; i++) {
            int begin = starts.get(i);
            int end = i + 1 < starts.size() ? starts.get(i + 1) : pageText.length();
            // Hard cap: don't let a later news article leak into a schedule.
            String segment = pageText.substring(begin, Math.min(end, begin + 570));
            Matcher hour = HOUR.matcher(segment);
            if (!hour.find()) continue;
            String time = hour.group(1) + "h" + hour.group(2);
            int tailStart = hour.end();
            Matcher nextHour = HOUR.matcher(segment);
            int tailEnd = segment.length();
            if (nextHour.find(tailStart)) tailEnd = nextHour.start();
            String tail = segment.substring(tailStart, tailEnd).trim();
            Matcher genre = GENRE.matcher(tail);
            if (!genre.find()) continue;
            boolean movie = fold(genre.group()).equals("cinema");
            if (movies && !movie) continue;
            String title = tail.substring(0, genre.start()).trim();
            Matcher stop = STOP_TAG.matcher(title);
            if (stop.find()) title = title.substring(0, stop.start()).trim();
            title = title.replaceAll("(?i)^(?:lire le resume|lire le résumé)\\s*", "")
                    .replaceAll("\\s+", " ").trim();
            if (title.length() < 3 || title.length() > 110
                    || title.matches("(?i)^(?:autre|cinema|programme tv)$")) continue;
            result.add(new Entry(names.get(i), time, title, movie));
        }
        return result;
    }

    public static String describe(String pageText, String question, long epochMillis) {
        if (!confirmsEveningDate(pageText, epochMillis))
            return "J'ai pu consulter le guide TV, mais je n'ai pas trouvé "
                + "une grille datée de ce soir pour les chaînes françaises. "
                + "Je ne vais pas inventer de programme ou d'horaire. "
                + "Consultez le guide : " + GUIDE_URL;
        List<Entry> entries = extract(pageText, filmsOnly(question), epochMillis);
        if (entries.isEmpty())
            return "Le guide TV français consulté ne fournit pas de "
                + (filmsOnly(question) ? "film" : "programme")
                + " avec chaîne, titre et heure confirmables pour ce soir. "
                + "Je ne vais pas inventer de diffusion. "
                + "Consultez la grille complète : " + GUIDE_URL;
        StringBuilder answer = new StringBuilder(
                "Guide Télé-Loisirs, programmes des chaînes françaises "
              + "pour ce soir (heure de Paris, date du ")
                .append(expectedDate(epochMillis)).append("). ");
        for (Entry entry : entries) answer.append(entry.display()).append(". ");
        answer.append("Ces horaires sont ceux affichés par le guide, "
                + "non une garantie de diffusion sans changement. "
                + "Voir la grille complète : ").append(GUIDE_URL);
        return answer.toString();
    }
}
