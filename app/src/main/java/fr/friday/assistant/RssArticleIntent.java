package fr.friday.assistant;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Identifie UNIQUEMENT les commandes visant un article precis du dernier bulletin. */
public final class RssArticleIntent {
    private RssArticleIntent() {}
    private static final Pattern REFERENCE = Pattern.compile(
            "\\b(?:article|page)\\s+(?:rss\\s+)?(?:(?:numero|n|no)\\s*)?(\\d{1,2}|un|deux|trois|quatre|cinq|six|sept|premier|premiere|"
            + "deuxieme|second|seconde|troisieme|quatrieme|cinquieme|sixieme|septieme)\\b");
    private static final Pattern ORDINAL = Pattern.compile(
            "\\b(premier|premiere|deuxieme|second|seconde|troisieme|quatrieme|cinquieme|"
            + "sixieme|septieme)\\s+(?:article|page)\\b");
    private static final Pattern SINGULAR = Pattern.compile(
            "\\b(?:l|le|cet|ce|dernier|premier)\\s+article\\b|"
            + "\\b(?:la|cette|derniere)\\s+page\\b");
    private static final Pattern ACTION = Pattern.compile(
            "\\b(?:lis|lit|lire|lecture|ouvre|ouvrir|raconte|raconter|"
            + "resume|resumer|synthese|analyse|analyser|analyses|"
            + "explique|expliquer|exposes?|detaille|details?)\\b");
    // Le dernier bulletin mentionne un numéro clair et « en entier » :
    // une dictée « lily l'article numéro deux en entier » reste une demande
    // explicite de lecture, même si Vosk déforme « lis » en « lily ».
    private static final Pattern COMPLETE_READING = Pattern.compile(
            "\\b(?:en\\s+entier|integralement|au\\s+complet)\\b");
    private static final Pattern ANALYSIS = Pattern.compile(
            "\\b(?:analyse|analyser|analyses|resume|resumer|synthese|"
            + "explique|expliquer|detaille)\\b");

    public static boolean isRequest(String question) {
        String text = VoiceText.normalize(question);
        boolean numbered = REFERENCE.matcher(text).find()
                || ORDINAL.matcher(text).find();
        boolean specific = numbered || SINGULAR.matcher(text).find();
        // « lily l'article numéro deux en entier » : l'article et le
        // caractère intégral sont clairs, malgré le verbe mal transcrit.
        // Ne pas traiter « article numéro de » : numéro ABSENT.
        boolean completeNumbered = numbered && COMPLETE_READING.matcher(text).find();
        // "lis les articles en entier" est général, pas un index.
        return specific && (ACTION.matcher(text).find() || completeNumbered)
                && !text.matches(".*\\b(?:code penal|code civil|loi|juridique|"
                        + "legislatif|constitution)\\b.*");
    }

    /**
     * Une courte réponse vocale « l'article numéro un » après un bulletin
     * RSS est une sélection d'article, pas une question générale à Gemma.
     * Limiter aux phrases ENTIERES, sans mention de code de loi ni autre sujet.
     * La présence du dernier bulletin est vérifiée par le lecteur chiffré.
     */
    public static boolean isBareReference(String question) {
        String q = VoiceText.normalize(question)
                .replaceAll("[’']", " ").replaceAll("[^a-z0-9 ]+", " ")
                .replaceAll("\\s+", " ").trim();
        boolean bare = q.matches("^(?:(?:jarvis|jarvise|jervis|friday)\\s+)?"
                + "(?:(?:dix|dis)\\s+)?"
                + "(?:(?:l|le|la|cet|cette|ce)\\s+)?"
                + "(?:article|page)\\s+(?:rss\\s+)?"
                + "(?:(?:numero|n|no)\\s+)?"
                + "(?:[1-7]|un|deux|trois|quatre|cinq|six|sept|"
                + "premier|premiere|deuxieme|second|seconde|troisieme|"
                + "quatrieme|cinquieme|sixieme|septieme)"
                + "(?:\\s+en\\s+entier)?$");
        return bare && index(q) >= 1 && index(q) <= 7;
    }

    public static boolean wantsAnalysis(String question) {
        return ANALYSIS.matcher(VoiceText.normalize(question)).find();
    }

    /**
     * La dictée libre Vosk peut confondre « lis l'article 1 en entier »
     * avec « c'est l'article et en adsl ». On demande une confirmation
     * sans supposer qu'une transcription ambiguë désigne un numéro précis.
     */
    public static boolean ambiguousVoiceReading(String question) {
        String q = VoiceText.normalize(question);
        return !isRequest(question) && !isBareReference(question)
                && q.matches(".*\\barticle\\b.*")
                && (q.matches(".*\\b(?:adsl|entier|entiere|integralement)\\b.*")
                    || q.matches("^(?:c est|ces|cet) l article.*"));
    }

    /** A command naming an article number but missing a valid value.
     * Never infer « deux » from « de »: ask for a number explicitly. */
    public static boolean missingNumber(String question) {
        String q = VoiceText.normalize(question);
        return q.matches(".*\\barticle\\s+numero\\s+(?:de|des|du)\\b.*")
                && !REFERENCE.matcher(q).find();
    }

    public static int index(String question) {
        String text = VoiceText.normalize(question);
        Matcher reference = REFERENCE.matcher(text);
        if (reference.find()) return number(reference.group(1));
        Matcher ordinal = ORDINAL.matcher(text);
        return ordinal.find() ? number(ordinal.group(1)) : 1;
    }

    private static int number(String value) {
        if (value.matches("\\d{1,2}")) {
            int n = Integer.parseInt(value);
            return n >= 1 && n <= 7 ? n : -1;
        }
        switch (value) {
            case "un": case "premier": case "premiere": return 1;
            case "deux": case "deuxieme": case "second": case "seconde": return 2;
            case "trois": case "troisieme": return 3;
            case "quatre": case "quatrieme": return 4;
            case "cinq": case "cinquieme": return 5;
            case "six": case "sixieme": return 6;
            case "sept": case "septieme": return 7;
            default: return -1;
        }
    }
}
