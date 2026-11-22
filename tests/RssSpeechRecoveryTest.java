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
                "Demande sans numéro incomprise : clarification locale");
        ok(!RssArticleIntent.ambiguousVoiceReading("Que dit l'article de loi ?"),
                "Ne pas intercepter les questions générales");
        ok(!RssArticleIntent.ambiguousVoiceReading("Quelle est ma position GPS ?"),
                "GPS indépendant des commandes RSS");
        // Régression d'un rapport chiffré v3.14.14, 22/11/2026 :
        // le mot « lis » a été entendu comme « lily ».
        ok(RssArticleIntent.isRequest("lily l'article numéro deux en entier"),
                "Lily + numéro deux + en entier doit déclencher le lecteur");
        ok(RssArticleIntent.index("lily l'article numéro deux en entier") == 2,
                "L'article deux doit être sélectionné, jamais le premier");
        ok(RssArticleIntent.wantsFullText("lily l'article numéro deux en entier"),
                "Lecture entière -> extraction profonde");
        ok(!RssArticleIntent.ambiguousVoiceReading(
                "lily l'article numéro deux en entier"),
                "Ne plus demander de répéter une référence claire");
        ok(RssArticleIntent.isRequest("article numéro deux en entier"),
                "Une référence explicite est suffisante même sans verbe");
        ok(RssArticleIntent.isBareReference("article numéro deux"),
                "Après clarification, dire simplement article deux");
        ok(!RssArticleIntent.isRequest("article numéro de en entier"),
                "De ne doit pas être interprété comme deux");
        ok(RssArticleIntent.ambiguousVoiceReading("article numéro de en entier"),
                "Numéro mal entendu -> clarification");
        ok(RssArticleIntent.missingNumber("article numéro de en entier"),
                "Identifier explicitement numéro de comme erreur ASR");
        ok(!RssArticleIntent.missingNumber("article numéro deux en entier"),
                "Numéro deux présent et fiable");
        ok(!RssArticleIntent.isRequest("le code pénal article deux en entier"),
                "Pas de capture d'un article juridique");
        ok(!RssArticleIntent.isRequest("je lis les articles en entier"),
                "Lecture générique ne désigne pas un article numéroté");
        System.out.println("RssSpeechRecoveryTest OK");
    }
}
