package fr.friday.assistant;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Dialogue 3.14.27: conservative local-only follow-up routing.
 * Works on the encrypted history snapshot assembled on Android. No HTTP,
 * location, recording, persistence or probabilistic guess of an antecedent.
 */
public final class ConversationTurnPolicy {
    private ConversationTurnPolicy() {}

    public static String fold(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.FRENCH)
                .replaceAll("[^a-z0-9 ]+", " ").replaceAll("\\s+", " ").trim()
                .replaceFirst("^(?:jarvis|jarvise|jervis|friday|fraday)\\s+", "");
    }

    // Live facts and explicit requests are never rewritten into earlier
    // conversations or diverted away from weather, RSS, URL or Web tools.
    private static final Pattern EXTERNAL = Pattern.compile(
            "\\b(?:web|internet|en ligne|sources?|liens?|https?|"
          + "recherche|rechercher|cherche|chercher|verifie|verifier|"
          + "actualites?|news|rss|articles?|meteo|temperature|previsions?|"
          + "aujourd hui|demain|maintenant|actuellement|heure|horaires?|"
          + "prix|tarifs?|cours|bourse|president|elections?)\\b");

    private static final Pattern FOLLOWUP = Pattern.compile(
        "^(?:(?:et|puis|alors|donc)\\s+)?"
      + "(?:(?:pourquoi|comment ca|comment cela|et pourquoi|pourquoi donc)\\b.*"
      + "|(?:continue|continuer|poursuis|poursuivre|reprends|recommence)\\b.*"
      + "|(?:developpe|developper|approfondis|approfondir|precise|preciser|"
      + "detaille|detailler|explique davantage|explique moi davantage|"
      + "explique un peu plus|explique ca|explique cela)\\b.*"
      + "|(?:donne (?:moi )?(?:des exemples|plus de details|des precisions))\\b.*"
      + "|(?:peux tu|pourrais tu|est ce que tu peux)\\s+"
      + "(?:expliquer davantage|developper|detailler|approfondir|preciser)\\b.*"
      + "|(?:qu est ce que tu (?:veux dire|voulais dire))\\b.*"
      + "|(?:que (?:signifie|veux dire) (?:ce|cet|cette|ces|cela|ca|il|elle))\\b.*"
      + "|(?:quel(?:le)?(?:s)? (?:est|sont) (?:son|sa|ses|leur|leurs)\\b.*)"
      + "|(?:et (?:pour|avec|sur|dans|ensuite|apres)\\b.*)"
      + "|(?:la meme chose|pareil|fais pareil)\\b.*"
      + "|(?:le|la|les|l|celui|celle)\\s+"
      + "(?:premier|premiere|deuxieme|troisieme|second|seconde|suivant|suivante|autre)\\b.*"
      + "|(?:et )?(?:lui|elle|eux|elles|celui ci|celle ci)\\b.*"
      + ")");

    private static final Pattern TOPIC_RESET = Pattern.compile(
            "^(?:(?:on|nous)\\s+)?(?:changeons? de sujet|change de sujet|"
          + "passons a autre chose|nouveau sujet|autre sujet|"
          + "je veux changer de sujet|on change de sujet)$");

    public static boolean topicReset(String question) {
        return TOPIC_RESET.matcher(fold(question)).matches();
    }

    public static String topicResetAnswer(String question) {
        return topicReset(question)
                ? "D'accord, nous changeons de sujet. Quel sujet souhaitez-vous aborder ?"
                : null;
    }

    /** No inferred sensitive referent and no network lookup for a short ellipsis. */
    public static boolean isContextual(String question) {
        String q = fold(question);
        if (q.length() < 3 || q.length() > 150 || topicReset(q)
                || EXTERNAL.matcher(q).find()) return false;
        if (q.split(" ").length > 18) return false;
        return FOLLOWUP.matcher(q).matches();
    }

    /** Must have a real user/assistant pair, not only a recalled old topic. */
    public static boolean hasRecentPair(String privateDialogue) {
        if (privateDialogue == null || privateDialogue.isEmpty()) return false;
        int recent = privateDialogue.indexOf("[ÉCHANGES LES PLUS RÉCENTS]");
        if (recent < 0) return false;
        int end = privateDialogue.indexOf("[FIN CONTEXTE PRIVÉ]", recent);
        String content = privateDialogue.substring(recent,
                end >= recent ? end : privateDialogue.length());
        return content.contains("\nUtilisateur : ") && content.contains("\nJarvis : ");
    }

    public static boolean shouldUseLocalModel(String question, String privateDialogue) {
        return isContextual(question) && hasRecentPair(privateDialogue);
    }

    /** When the antecedent is missing, ask one targeted question, don't guess. */
    public static String clarification(String question, String privateDialogue) {
        if (topicReset(question)) return topicResetAnswer(question);
        if (!isContextual(question) || hasRecentPair(privateDialogue)) return null;
        String q = fold(question);
        if (q.matches(".*\\b(?:premier|deuxieme|troisieme|second|suivant|autre)\\b.*"))
            return "À quelle liste ou étape faites-vous référence ? Rappelez-moi le sujet.";
        if (q.matches(".*\\b(?:lui|elle|eux|celui|celle|son|sa|ses)\\b.*"))
            return "De quelle personne ou de quel élément parlez-vous ?";
        return "Sur quel sujet souhaitez-vous que je poursuive ? "
                + "Je n'ai pas d'échange précédent exploitable pour cette précision.";
    }

    /** This is prompt metadata only; never a public search term. */
    public static String localInstruction(String question) {
        if (!isContextual(question)) return "";
        return "\n[RELANCE CONTEXTUELLE LOCALE] Réponds à la question actuelle "
                + "en te fondant uniquement sur les échanges récents fournis sur "
                + "ce téléphone. Ne choisis pas un autre sujet. "
                + "Si la référence est ambiguë, pose une seule question de "
                + "clarification, sans inventer la réponse ni une source Web. "
                + "[FIN RELANCE]\n";
    }
}
