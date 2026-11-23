package fr.friday.assistant;

/** Couverture des formulations réelles observées dans le rapport JARVIS. */
public final class RssFollowupV31418Test {
    private static void ok(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        for (String prompt : new String[]{
                "Actualités 2 en entier", "Articles 2 en entier",
                "Lis l'article deux en entier", "l'article numéro 2 en entier",
                "Jarvis lis l'article 2 en entier"}) {
            ok(RssArticleIntent.isRequest(prompt) || RssArticleIntent.isBareReference(prompt),
                    "Relance RSS non reconnue : " + prompt);
            ok(RssArticleIntent.index(prompt) == 2,
                    "Mauvais index conservé : " + prompt);
        }
        ok(RssArticleIntent.isRequest("dit l'article entier numéro un"),
                "Vosk : lire l'article entier numéro un");
        ok(RssArticleIntent.index("dit l'article entier numéro un") == 1,
                "L'article numéro un doit être conservé");
        ok(RssArticleIntent.wantsFullText("dit l'article entier numéro un"),
                "Lecture intégrale demandée par l'utilisateur");
        ok(!RssArticleIntent.isRequest("de en entier"),
                "Sans numéro, ne jamais deviner l'article à lire");
        ok(RssArticleIntent.isBareReference("Actualités 2"),
                "Sélection courte du second titre");
        ok(RssArticleIntent.index("articles trois en entier") == 3,
                "Lecture de l'article trois");
        ok(!RssArticleIntent.isRequest("Quels sont les deux articles du Code civil ?"),
                "Ne pas intercepter des références juridiques");
        ok(!RssArticleIntent.isRequest("Donne les actualités du jour"),
                "Ne pas transformer un bulletin général en article numéroté");
        ok(RssArticleIntent.wantsFullText("Actualités 2 en entier"),
                "Lecture complète demandée");
        System.out.println("RssFollowupV31418Test OK");
    }
}
