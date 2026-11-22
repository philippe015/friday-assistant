package fr.friday.assistant;

/**
 * Only asks for a missing antecedent when the encrypted local conversation
 * has no usable context. This never sends personal dialogue to the Internet.
 * Avoids hallucinating an unnamed person, place, article or previous answer.
 */
public final class ConversationClarificationPolicy {
    private ConversationClarificationPolicy() {}

    public static String missingContextQuestion(String question, String privateDialogue) {
        if (privateDialogue != null && !privateDialogue.trim().isEmpty()) return null;
        String q = DialogueMemoryPolicy.fold(question);
        if (q.isEmpty() || q.length() > 125) return null;
        if (RssArticleIntent.isRequest(question) || RssArticleIntent.isBareReference(question))
            return null;
        if (q.matches("^(?:et )?(?:le|la|les|l|celui|celle) "
                + "(?:premier|deuxieme|troisieme|second|suivant|autre)\\b.*"))
            return "À quelle liste ou à quel article faites-vous référence ? "
                    + "Rappelez-moi le sujet pour que je choisisse le bon élément.";
        if (q.matches("^(?:et )?(?:lui|elle|eux|celui ci|celle ci)\\b.*"))
            return "De quelle personne ou de quel élément parlez-vous ?";
        if (DialogueMemoryPolicy.isContextualFollowUp(question)) {
            if (q.matches("^(?:et |pour |et pour |a |et a ).*"))
                return "À propos de quel sujet souhaitez-vous cette précision ?";
            return "Sur quel sujet précédent voulez-vous que je développe ? "
                    + "Je n'ai pas de contexte de conversation exploitable pour le moment.";
        }
        return null;
    }
}
