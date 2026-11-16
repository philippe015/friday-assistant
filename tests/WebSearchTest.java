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
        System.out.println("WebSearchTest OK");
    }
}
