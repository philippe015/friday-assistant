import fr.friday.assistant.WebSearch;

/** Test de non-régression de la politique Web, sans Android ni accès réseau. */
public final class WebSearchTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        check(WebSearch.needsWeb("Cherche sur Internet les lois sur le télétravail"),
                "Recherche explicite non détectée");
        check(WebSearch.needsWeb("Quel est le dernier prix du carburant ?"),
                "Prix actuel non détecté");
        check(WebSearch.needsWeb("Comment fonctionne le système solaire ?"),
                "Question scientifique non détectée");
        check(WebSearch.needsWeb("Pourquoi les feuilles sont-elles vertes ?"),
                "Question ouverte non détectée");
        check(WebSearch.needsWeb("Donne-moi des informations sur les batteries lithium"),
                "Demande d'information non détectée");
        check(WebSearch.needsWeb("Quelle est la météo de demain à Paris ?"),
                "Météo non détectée");
        check(WebSearch.needsWeb("Lis https://example.org/article?id=123"),
                "URL non détectée");
        check(!WebSearch.needsWeb("Bonjour !"), "Salutation = appel Web inutile");
        check(!WebSearch.needsWeb("Comment vas-tu ?"), "Conversation personnelle = appel Web inutile");
        check(!WebSearch.needsWeb("Merci"), "Remerciement = appel Web inutile");
        check(!WebSearch.needsWeb("Je voudrais jouer de la guitare"), "Affirmation = appel Web inutile");

        String previous = "Quelle est la météo demain à Paris ?";
        String followUp = "Et à Lyon ?";
        check(WebSearch.isFollowUp(followUp), "Relance géographique non reconnue");
        check(WebSearch.needsWeb(followUp, previous), "Relance doit rechercher le Web");
        check(WebSearch.searchQuery(followUp, previous).contains(previous),
                "Le contexte précédent doit être conservé");
        check(WebSearch.searchQuery(followUp, previous).contains("Lyon"),
                "La nouvelle ville ne doit pas être perdue");
        check(WebSearch.searchQuery("Cherche sur Internet les dernières nouvelles", "")
                        .startsWith("les dernières nouvelles"),
                "La recherche ne doit pas envoyer la commande vocale");
        check(WebSearch.firstUrl("Lis https://example.org/news?id=12.").equals("https://example.org/news?id=12"),
                "Extraction URL");
        check(WebSearch.firstUrl("aucune URL").isEmpty(), "URL inventée");
        check(WebSearch.shouldSearch("always", "Bonjour", ""),
                "Mode Toujours doit demander le Web même pour une question ordinaire");
        check(!WebSearch.shouldSearch("auto", "Bonjour", ""),
                "Mode Automatique ne doit pas rechercher une salutation");
        check(WebSearch.shouldSearch("auto", "Quelle est la météo demain ?", ""),
                "Mode Automatique conserve les questions d'actualité");
        check(!WebSearch.shouldSearch("off", "Bonjour", ""),
                "Mode Off est limité à l'évaluation du besoin : désactivation gérée par le service");
        check(WebSearch.firstUrl("Analyse la page https//www.lci.fr/").equals(
                "https://www.lci.fr/"), "HTTPS oral dépourvu de deux-points");
        check(WebSearch.firstUrl("Analyse la page https:/www.lci.fr/").equals(
                "https://www.lci.fr/"), "HTTPS oral avec un seul slash");
        check(WebSearch.firstUrl("Analyse www.lci.fr/").equals(
                "https://www.lci.fr/"), "URL www sans schéma");
        check(WebSearch.needsWeb("Analyse https//www.lci.fr/"),
                "détection du Web avec HTTPS oral imparfait");

        StringBuilder longPrior = new StringBuilder();
        for (int i = 0; i < 100; i++) longPrior.append("actualité ");
        String query = WebSearch.searchQuery("Et demain ?", longPrior.toString());
        check(query.length() <= 128 && query.endsWith("Et demain ?"),
                "Question actuelle perdue après coupe à 128 caractères");
        check(!WebSearch.needsWeb("Quels"),
                "Vosk : un mot interrogatif isolé ne doit pas déclencher Bing");
        check(!WebSearch.needsWeb("de"),
                "Vosk : une préposition isolée ne doit pas déclencher Bing");
        check(!WebSearch.needsWeb("Quels sont tes capacités ?"),
                "Question sur les capacités de Jarvis ne doit pas ouvrir Bing");
        check(!WebSearch.needsWeb("Fraday, quelles sont tes fonctionnalités ?"),
                "Transcription wake + capacité doit rester locale");
        check(!WebSearch.needsWeb("A de toujours pas accès Internet"),
                "Une plainte sur l'accès Web n'est pas un mot-clé de recherche");
        check(!WebSearch.needsWeb("Jarvis, tu as accès au web ?"),
                "Le statut Internet de Jarvis doit être un diagnostic local");
        check(WebSearch.assistantAnswer("Quels sont tes capacités ?", true)
                        .contains("FRIDAY"),
                "Les capacités doivent recevoir une réponse locale déterministe");
        check(WebSearch.assistantAnswer("tu n'as toujours pas accès Internet", false)
                        .contains("désactivé"),
                "La réponse doit respecter le réglage Internet réel");
        check(WebSearch.assistantAnswer("Cherche sur Internet une source SSIAP", true) == null,
                "Une commande de recherche explicite doit rester une recherche");
        check(WebSearch.needsWeb("Recherche sur Internet la réglementation SSIAP 2"),
                "Une vraie recherche doit fonctionner");
        check(!WebSearch.relevantResult("Quels sont tes capacités ?",
                        "Quels : définition française", "Accord de quel"),
                "Ne jamais citer une définition de mot interrogatif hors sujet");
        check(!WebSearch.relevantResult("Accès Internet de Jarvis",
                        "De : préposition", "règles grammaticales"),
                "Ne pas citer une définition de préposition");
        check(WebSearch.relevantResult("Quelle est la météo à Paris ?",
                        "Météo Paris demain", "Prévisions météo parisiennes"),
                "Conserver un vrai résultat météorologique");
        check(WebSearch.relevantResult("Code du travail durée de pause",
                        "Code du travail : repos et pause", "Code du travail en France"),
                "Conserver des sources du thème réellement demandé");
        System.out.println("WebSearchTest OK");
    }
}
