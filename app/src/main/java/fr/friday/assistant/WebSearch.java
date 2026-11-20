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
    private static final Pattern URL = Pattern.compile("(?<![a-zA-Z0-9])(?:https?\\s*:?/{1,2}|www\\.)[^\\s<>\"']+", Pattern.CASE_INSENSITIVE);
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

    private static final String[] QUERY_STOP_WORDS = {
            "quel", "quelle", "quels", "quelles", "est", "sont", "etre", "pourquoi",
            "comment", "quoi", "qui", "que", "qu", "quand", "ou", "avec", "sans",
            "vous", "nous", "moi", "mes", "tes", "ton", "ta", "mon", "ma",
            "les", "des", "une", "dans", "pour", "sur", "par", "aux", "chez",
            "leur", "leurs", "de", "du", "la", "le", "un", "et", "a", "au",
            "en", "il", "elle", "je", "j", "tu", "ne", "pas", "toujours",
            "peux", "peut", "faire", "donne", "dire", "cherche", "recherche",
            "internet", "web", "regarde", "trouve", "verifie", "consulte",
            "friday", "fraday", "freddy", "fredi", "jarvis", "jervis"
    };
    private WebSearch() { }

    private static String withoutWake(String value) {
        return fold(value).replaceFirst("^(?:friday|fraday|frida|freiday|freday|freddy|fredi|fride|vendredi|jarvis|jarvise|jervis|jervi)\\s+", "");
    }

    /** Réponse vérifiable localement aux questions sur l'assistant lui-même.
     * Évite d'envoyer « quels sont tes capacités » à un moteur de recherche. */
    public static String assistantAnswer(String question, boolean internetEnabled) {
        String q = withoutWake(question);
        if (q.isEmpty() || firstUrl(question).length() > 0
                || q.matches("^(?:cherche|recherche|verifie|consulte|trouve)\\b.*"))
            return null;
        boolean refersToAssistant = q.matches(".*\\b(?:tu|toi|ton|ta|tes|vous|votre|vos|jarvis|friday|fraday)\\b.*");
        boolean ability = q.matches(".*\\b(?:capacites?|fonctionnalites?|possibilites?|competences?|capable)\\b.*")
                || q.matches("^(?:que|qu est ce que) (?:peux tu|sais tu|pouvez vous) faire.*")
                || q.matches("^(?:a quoi sers tu|que sais tu faire|presente toi).*");
        boolean accessComplaint = (q.contains("internet") || q.matches(".*\\bweb\\b.*"))
                && (q.matches(".*\\b(?:acces|connexion|connecte|connexion|naviguer|limite|limitee|recherche)\\b.*")
                    || q.contains("pas acces") || q.contains("pas d acces"))
                && (refersToAssistant || q.contains("toujours pas")
                    || q.startsWith("acces internet") || q.startsWith("a de "));
        if (accessComplaint) {
            return internetEnabled
                    ? "Le module Internet de FRIDAY est activé. L'application peut chercher "
                      + "des pages HTTPS publiques avec Bing et DuckDuckGo, et des actualités "
                      + "horodatées avec Google Actualités. Ces recherches sont faites par "
                      + "l'application ; c'est Jarvis local qui produit mes réponses. "
                      + "Je n'ai pas un navigateur illimité : des pages privées, bloquées "
                      + "ou dépendantes de JavaScript peuvent rester inaccessibles. "
                      + "Une option activée ne garantit pas que chaque recherche aboutit. "
                      + "Pour vérifier la connexion actuelle, utilisez « Tester l'accès Internet Jarvis »."
                    : "L'accès Internet de FRIDAY est désactivé dans les réglages. "
                      + "Jarvis local peut encore répondre hors ligne. Activez "
                      + "« Autoriser Jarvis à rechercher des informations sur Internet » "
                      + "pour consulter des sources HTTPS publiques.";
        }
        if (refersToAssistant && ability) {
            return "Je suis FRIDAY, assistant Jarvis local sur ce téléphone. "
                    + "Je peux répondre aux questions et parler en français avec le modèle "
                    + "installé, utiliser l'écoute Vosk, aider à expliquer des sujets, "
                    + "et utiliser la mémoire locale chiffrée si elle est activée. "
                    + (internetEnabled
                       ? "Le module Internet est autorisé : il peut consulter certaines "
                         + "sources HTTPS publiques et des actualités datées. "
                       : "Les recherches Internet sont actuellement désactivées. ")
                    + "Je ne peux pas garantir l'accès à tout le Web, ni aux sites "
                    + "nécessitant une connexion à un compte. Mes réponses peuvent "
                    + "comporter des erreurs ; vérifiez les informations importantes.";
        }
        return null;
    }

    /** Écarte les résultats qui ne reprennent aucun sujet significatif.
     * Un mot grammatical (« quels », « de ») ne suffit jamais à faire source. */
    public static boolean relevantResult(String query, String title, String summary) {
        String corpus = " " + fold(title + " " + summary) + " ";
        String[] terms = fold(searchQuery(query, "")).split(" ");
        java.util.LinkedHashSet<String> topics = new java.util.LinkedHashSet<>();
        for (String word : terms) {
            if (word.length() < 4) continue;
            boolean stop = false;
            for (String common : QUERY_STOP_WORDS) {
                if (common.equals(word)) { stop = true; break; }
            }
            if (!stop) topics.add(word);
        }
        if (topics.isEmpty()) return false;
        int hits = 0;
        for (String term : topics) {
            if (corpus.contains(" " + term + " ")) hits++;
        }
        return hits >= (topics.size() >= 4 ? 2 : 1);
    }

    private static String fold(String value) {
        String lower = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.FRENCH);
        return lower.replaceAll("[^a-z0-9 ]+", " ").replaceAll("\\s+", " ").trim();
    }

    public static String firstUrl(String question) {
        Matcher matcher = URL.matcher(question == null ? "" : question);
        if (!matcher.find()) return "";
        String candidate = matcher.group().replaceAll("[.,;!?)]+$", "");
        return WebPageReader.normalizeUrlInput(candidate);
    }

    public static boolean explicit(String question) {
        return EXPLICIT.matcher(fold(question)).find();
    }

    public static boolean needsWeb(String question) {
        if (firstUrl(question).length() > 0) return true;
        String clean = withoutWake(question);
        if (assistantAnswer(question, true) != null) return false;
        if (clean.isEmpty() || LOCAL_CHAT.matcher(clean).find()) return false;
        // Une transcription Vosk réduite à « quels »/« quelle » ne doit
        // jamais déclencher une recherche de dictionnaire.
        if (clean.matches("^(?:qui|quoi|que|quel|quelle|quels|quelles|comment|pourquoi|de|le|la)$"))
            return false;
        return EXPLICIT.matcher(clean).find() || CURRENT.matcher(clean).find()
                || FACTUAL.matcher(clean).find();
    }

    /** L'option Toujours demande le Web pour les questions non locales, hors URL directe. */
    public static boolean shouldSearch(String mode, String question, String previousUserQuestion) {
        return "always".equals(mode) || needsWeb(question, previousUserQuestion);
    }

    public static boolean isFollowUp(String question) {
        String clean = withoutWake(question);
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
        current = current.replaceFirst("(?i)^(?:friday|fraday|frida|freiday|freday|freddy|fredi|fride|vendredi|jarvis|jarvise|jervis|jervi)[\\s,.:;!?-]+", "");
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
