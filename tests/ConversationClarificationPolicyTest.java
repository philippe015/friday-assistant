package fr.friday.assistant;

/** Realistic French elliptical follow-ups. No guessing when context is empty. */
public final class ConversationClarificationPolicyTest {
    private static void ok(boolean good, String why) {
        if (!good) throw new AssertionError(why);
    }

    public static void main(String[] args) {
        ok(DialogueMemoryPolicy.isContextualFollowUp("Et le deuxième ?"),
                "Reference to numbered item must be local follow-up");
        ok(DialogueMemoryPolicy.keepLocal("Et pour Lyon ?"),
                "Elliptical city follow-up never sent as blind web search");
        ok(ConversationClarificationPolicy.missingContextQuestion(
                "Et le deuxième ?", "").contains("liste"),
                "No previous list -> ask which list");
        ok(ConversationClarificationPolicy.missingContextQuestion(
                "Et lui ?", "").contains("personne"),
                "No person antecedent -> clarify");
        ok(ConversationClarificationPolicy.missingContextQuestion(
                "Donne-moi plus de détails", "").contains("sujet"),
                "Details without subject -> clarify");
        ok(ConversationClarificationPolicy.missingContextQuestion(
                "Et pour Lyon ?", "").contains("sujet"),
                "City without topic -> clarify");
        ok(ConversationClarificationPolicy.missingContextQuestion(
                "Et le deuxième ?", "[ÉCHANGES] Jarvis : Deux titres récents") == null,
                "Existing conversation -> let model resolve referent");
        ok(ConversationClarificationPolicy.missingContextQuestion(
                "Analyse l'article numéro deux en entier", "") == null,
                "RSS article reading remains routed to specialized reader");
        ok(ConversationClarificationPolicy.missingContextQuestion(
                "Quels sont les symptômes d'une grippe ?", "") == null,
                "Ordinary standalone question bypasses clarification policy");
        ok(ConversationClarificationPolicy.missingContextQuestion(
                "Quelle est la météo à Paris aujourd'hui ?", "") == null,
                "Explicit city weather uses Web weather pipeline");
        System.out.println("ConversationClarificationPolicyTest OK (10 cas)");
    }
}
