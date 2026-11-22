package fr.friday.assistant;

/**
 * Regression from Nov 22: "Libye : explosion sur un navire russe dans le port
 * de Benghazi après une probable attaque de drones - Le Figaro".
 * Synthetic public HTML fixture, not an assertion about actual news.
 */
public final class RssPublisherBodyMatchTest {
    private static void check(boolean value, String why) {
        if (!value) throw new AssertionError(why);
    }
    public static void main(String[] args) {
        String headline = "Libye : explosion sur un navire russe dans le port "
                + "de Benghazi après une «probable» attaque de drones - Le Figaro";
        String publisherPageTitle = "Actualité internationale | Le Figaro";
        check(!RssPublisherArticlePolicy.isLiveHeadline(headline),
                "Non-DIRECT article route");
        check(!RssPublisherArticlePolicy.matchingTitle(
                headline, publisherPageTitle), "Search index title mismatch reproduced");
        check("lefigaro.fr".equals(
                RssPublisherArticlePolicy.domainFromRssSource(
                    "<item><source url=\"https://www.lefigaro.fr/\">"
                        + "Le Figaro</source></item>")), "RSS source host extracted");
        check(RssPublisherArticlePolicy.publisherHost(
                "https://www.lefigaro.fr/international/example-20261122",
                "lefigaro.fr"), "Official subdomain accepted");
        check(!RssPublisherArticlePolicy.publisherHost(
                "https://lefigaro.fr.evil.invalid/example", "lefigaro.fr"),
                "Lookalike forbidden");
        String page = ("Menu Accueil Abonnements International Économie "
                + "Toute l'actualité sans rapport. ").repeat(9)
                + "Une explosion a eu lieu dans le port de Benghazi "
                + "en Libye, touchant un navire russe. Une probable "
                + "attaque de drones est évoquée selon les autorités "
                + "locales. Un rapport de terrain reste attendu."
                + (" Contacts Navigation Événements.").repeat(20);
        String passage = RssPublisherArticlePolicy.verifiedArticlePassage(headline, page);
        check(!passage.isEmpty(), "Matching public passage found despite different title");
        check(passage.length() <= 1900, "Bounded passage instead of entire article");
        check(passage.contains("Benghazi"), "Distinctive topic preserved");
        check(RssPublisherArticlePolicy.verifiedArticlePassage(headline,
                "Un navire russe a été touché près d'un port en mer Baltique. "
                    + "Les autorités évoquent des drones.").isEmpty(),
                "Nearby but unrelated report must be rejected");
        check(RssPublisherArticlePolicy.verifiedArticlePassage(headline,
                "La Libye a publié le bilan économique de Benghazi."
                    + " Plusieurs ports sont sous contrôle local.").isEmpty(),
                "Missing explosion and vessel is not the requested article");
        check(RssPublisherArticlePolicy.verifiedArticlePassage(
                "DIRECT. Marine Le Pen dit n'avoir jamais reçu d'information "
                  + "confidentielle du préfet Hugues Moutouh - BFM",
                page).isEmpty(), "DIRECT still uses its separate safety guard");
        check(RssPublisherArticlePolicy.shortPublisherQuery(headline, "lefigaro.fr")
                .endsWith("site:lefigaro.fr"), "Publisher search uses exact host");
        System.out.println("RssPublisherBodyMatchTest OK");
    }
}
