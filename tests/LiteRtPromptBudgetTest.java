package fr.friday.assistant;

/** Régression du crash réel : 2151 tokens >= limite LiteRT 2048. */
public final class LiteRtPromptBudgetTest {
    private static void yes(boolean result, String msg) {
        if (!result) throw new AssertionError(msg);
    }
    public static void main(String[] args) {
        String header = "Tu es JARVIS. Réponds en français et ne fabrique pas de faits.\n";
        String profile = "[PROFIL ACTIF — PERSONNALITÉ]\nNom du profil : LEGRAND\n"
                + "Personnalité JARVIS locale. ".repeat(180)
                + "\n[FIN DU PROFIL ACTIF]";
        String dialogue = "\n[CONTEXTE DE DIALOGUE PRIVÉ]\n"
                + "Mémoire récente chiffrée, uniquement sur téléphone. ".repeat(70);
        String current = "\nDemande ACTUELLE : Analyse l'article numéro un "
                + "en tenant compte des informations suivantes. "
                + "Données publiques sur un article. ".repeat(130)
                + "\nJARVIS :";
        String prompt = header + profile + dialogue + current;
        yes(LiteRtPromptBudget.utf8Bytes(prompt) > 12_000,
                "Le test reproduit un contexte trop long");
        String first = LiteRtPromptBudget.fit(prompt,
                LiteRtPromptBudget.INITIAL_UTF8_BYTES);
        String retry = LiteRtPromptBudget.fit(prompt,
                LiteRtPromptBudget.RETRY_UTF8_BYTES);
        for (String candidate : new String[]{first, retry}) {
            yes(candidate.contains("LEGRAND"), "Profil actif préservé");
            yes(candidate.contains("Demande ACTUELLE"), "Question présente");
            yes(candidate.contains("Analyse l'article numéro un"), "Intention préservée");
            yes(candidate.endsWith("\nJARVIS :"), "Suffixe moteur intact");
            yes(!candidate.contains("Mémoire récente chiffrée, uniquement sur téléphone. "
                    .repeat(15)), "Ancien historique abrégé");
        }
        yes(LiteRtPromptBudget.utf8Bytes(first) <= 3600, "Premier budget UTF-8");
        yes(LiteRtPromptBudget.utf8Bytes(retry) <= 1400, "Budget de secours UTF-8");
        yes(LiteRtPromptBudget.utf8Bytes(
                LiteRtPromptBudget.fit("Éléphant 😀", 1400))
                == LiteRtPromptBudget.utf8Bytes("Éléphant 😀"), "Pas de découpage inutile");

        yes(RssArticleIntent.isBareReference("l'article numéro un"),
                "Référence orale précise routée au lecteur RSS");
        yes(RssArticleIntent.isBareReference("dix l'article numéro un"),
                "Erreur Vosk « dix » reconnue dans cette phrase exacte");
        yes(RssArticleIntent.isBareReference("Jarvis, article numéro 2"),
                "Commande courte avec mot-clé");
        yes(!RssArticleIntent.isBareReference("article numéro un du code pénal"),
                "Ne pas détourner une question juridique");
        yes(!RssArticleIntent.isBareReference("j'ai vu l'article numéro un du journal"),
                "Ne pas absorber une phrase générale");
        yes(!RssArticleIntent.isBareReference("lis les articles"),
                "Pas de requête RSS plurielle inventée");
        System.out.println("LiteRtPromptBudgetTest OK");
    }
}
