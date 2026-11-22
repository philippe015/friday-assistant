package fr.friday.assistant;

/** Vosk parfois transcrit « article numéro un » ou « c'est l'article et en adsl ». */
public final class RssSpeechRecoveryTest {
    private static void ok(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        ok(RssArticleIntent.isRequest("Jarvis lis l'article numero un en entier"),
                "Numéro un reconnu");
        ok(RssArticleIntent.index("Jarvis lis l'article numero un en entier") == 1,
                "Numéro un vaut index 1");
        ok(RssArticleIntent.isRequest("analyse l'article deux"),
                "Numéro deux compris");
        ok(RssArticleIntent.index("analyse l'article deux") == 2,
                "Numéro deux vaut index 2");
        ok(RssArticleIntent.index("lis l'article sept") == 7,
                "Numéro sept vaut index 7");
        ok(RssArticleIntent.isRequest("Jarvis lis l'article 1 en entier"),
                "Numéro en chiffres toujours reconnu");
        ok(!RssArticleIntent.isRequest("c'est l'article et en adsl"),
                "Pas de lecture d'un article inventé");
        ok(RssArticleIntent.ambiguousVoiceReading("c'est l'article et en adsl"),
                "Vosk ambigu demande une reformulation locale");
        ok(RssArticleIntent.ambiguousVoiceReading("lis l'article en entier"),
                "Numéro absent, reformulation demandée");
        ok(!RssArticleIntent.ambiguousVoiceReading("Que dit l'article de loi ?"),
                "Ne pas intercepter les questions générales");
        ok(!RssArticleIntent.ambiguousVoiceReading("Quelle est ma position GPS ?"),
                "GPS indépendant des commandes RSS");
        System.out.println("RssSpeechRecoveryTest OK");
    }
}
