package fr.friday.assistant;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Politique de recherche et reformulation des relances pour le moteur Web.
 * Sans dépendance Android : testable par javac et utilisable hors connexion.
 */
public final class WebSearch {
    private static final Pattern URL = Pattern.compile("https?://[^\\s<>\"']+", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXPLICIT = Pattern.compile(
            "\\b(?:cherche|chercher|recherche|rechercher|verifie|verifier|consulte|consulter|trouve|trouver|regarde|regarder|sources?|internet|web|en ligne)\\b");
    private static final Pattern CURRENT = Pattern.compile(
            "\\b(?:actualites?|actus?|news|aujourd hui|demain|hier|en ce moment|maintenant|dernieres?|recents?|nouveautes?|"
            + "prix|tarifs?|cout|meteo|temperatures?|previsions?|classement|resultats?|scores?|"
            + "elections?|president|gouvernement|bourse|cryptos?|bitcoin|population|"
            + "horaires?|ouverture|fermeture|sortie|sorties|officiel|disponibilite|mise a jour)\\b"
            + "|\\b20[2-9][0-9]\\b");
    private static final Pattern FACTUAL = Pattern.compile(
            "^(?:(?:(?:dis moi|peux tu me dire|peux tu m expliquer|je voudrais savoir|j aimerais savoir|"
            + "explique moi|explique|decris|liste|compare|donne moi des informations)\\s+)?"
            + "(?:qui|quoi|que|qu|quel|quelle|quels|quelles|quand|ou|combien|comment|pourquoi|"
            + "est ce que|qu est ce que)\\b|"
            + "(?:explique|decris|compare|liste|parle moi de|donne moi des informations sur)\\b)");
    private static final Pattern FOLLOW_UP = Pattern.compile(
            "^(?:et\\b|et pour\\b|et a\\b|a (?:la|l |le|les)?\\b|pour\\b|"
            + "qu en est il\\b|la meme chose\\b|pareil\\b|et demain\\b|demain\\b|"
            + "et lui\\b|et elle\\b|la bas\\b)");
    private static final Pattern LOCAL_CHAT = Pattern.compile(
            "^(?:bonjour|salut|merci|bonsoir|bonne nuit|comment vas tu|ca va|"
            + "raconte moi une blague|que sais tu de moi|qui es tu|tu es qui)"
            + "(?:\\b|$)");

    private WebSearch() { }

    private static String fold(String value) {
        String lower = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.FRENCH);
        return lower.replaceAll("[^a-z0-9 ]+", " ").replaceAll("\\s+", " ").trim();
    }

    public static String firstUrl(String question) {
        Matcher matcher = URL.matcher(question == null ? "" : question);
        if (!matcher.find()) return "";
        return matcher.group().replaceAll("[.,;!?)]+$", "");
    }

    public static boolean explicit(String question) {
        return EXPLICIT.matcher(fold(question)).find();
    }

    public static boolean needsWeb(String question) {
        if (firstUrl(question).length() > 0) return true;
        String clean = fold(question).replaceFirst("^(?:friday|frida|freiday|freday|fride)\\s+", "");
        if (clean.isEmpty() || LOCAL_CHAT.matcher(clean).find()) return false;
        return EXPLICIT.matcher(clean).find() || CURRENT.matcher(clean).find()
                || FACTUAL.matcher(clean).find();
    }

    public static boolean isFollowUp(String question) {
        String clean = fold(question).replaceFirst("^(?:friday|frida|freiday|freday|fride)\\s+", "");
        return FOLLOW_UP.matcher(clean).find() && clean.split(" ").length <= 11;
    }

    public static boolean needsWeb(String question, String previousUserQuestion) {
        return needsWeb(question) || (isFollowUp(question) && needsWeb(previousUserQuestion));
    }

    /**
     * Conserve le sujet de la question précédente pour les questions elliptiques.
     * Ex. : « météo demain à Paris » puis « et à Lyon ? ».
     * Cette chaîne est uniquement transmise au moteur de recherche, pas à la réponse locale.
     */
    public static String searchQuery(String question, String previousUserQuestion) {
        String current = question == null ? "" : question.trim();
        current = current.replaceFirst("(?i)^(?:friday|frida|freiday|freday|fride)[\\s,.:;!?-]+", "");
        current = current.replaceFirst("(?i)^(?:cherche|recherche|verifie|trouve|regarde|consulte)\\s+"
                + "(?:(?:sur\\s+)?(?:internet|le\\s+web)\\s+)?", "").trim();
        if (isFollowUp(current) && previousUserQuestion != null && !previousUserQuestion.trim().isEmpty()) {
            String previous = previousUserQuestion.trim();
            int remaining = Math.max(0, 127 - current.length());
            if (previous.length() > remaining) previous = previous.substring(0, remaining);
            current = previous + " " + current;
        }
        return current.length() <= 128 ? current : current.substring(0, 128);
    }
}
