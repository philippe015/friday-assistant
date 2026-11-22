package fr.friday.assistant;

/** Diagnostic 22 novembre 2026: Google News DIRECT (BFM) sans lien d'éditeur. */
public final class RssLivePublisherPolicyTest {
    private static void yes(boolean ok, String explanation) {
        if (!ok) throw new AssertionError(explanation);
    }
    public static void main(String[] args) {
        String headline = "DIRECT. Marine Le Pen dit n'avoir \"jamais reçu "
                + "d'information confidentielle\" du préfet Hugues Moutouh - BFM";
        yes(RssPublisherArticlePolicy.isLiveHeadline(headline), "Titre DIRECT reconnu");
        yes(!RssPublisherArticlePolicy.isLiveHeadline("Marine Le Pen dit ..."),
                "Article ordinaire distinct");
        yes("bfmtv.com".equals(RssPublisherArticlePolicy.publisherDomain("BFM BFM", headline)),
                "Alias BFM associé au domaine officiel");
        String query = RssPublisherArticlePolicy.shortPublisherQuery(headline, "bfmtv.com");
        yes(query.contains("site:bfmtv.com"), "Recherche limitée au site de l'éditeur");
        yes(query.contains("moutouh"), "Nom rare toujours présent dans les mots-clés");
        yes(query.contains("marine"), "Sujet essentiel conservé");
        yes(!query.contains("direct."), "Mot générique DIRECT retiré");
        yes(RssPublisherArticlePolicy.publisherHost(
                "https://www.bfmtv.com/politique/exemple.html", "bfmtv.com"),
                "Domaine officiel sous-domaine autorisé");
        yes(!RssPublisherArticlePolicy.publisherHost(
                "https://bfmtv.com.attacker.example/phishing", "bfmtv.com"),
                "Pas de faux sous-domaine");
        String unrelated = ("Autres informations. Nouvelles politiques sans lien "
                + "avec la demande RSS et plusieurs résultats sportifs. ").repeat(17);
        String matchingUpdate = "15h10. Marine Le Pen assure n'avoir jamais reçu "
                + "d'information confidentielle du préfet Hugues Moutouh. "
                + "Cette déclaration a été recueillie dans une émission de radio. "
                + "La question porte sur les documents transmis au parti.";
        String publicLivePage = unrelated + "\n" + matchingUpdate + "\n"
                + "15h20. Les informations économiques et la météo du jour.";
        String passage = RssPublisherArticlePolicy.relevantLivePassage(
                headline, publicLivePage);
        yes(!passage.isEmpty(), "Mise à jour retrouvée dans le DIRECT");
        yes(passage.contains("Moutouh"), "L'extrait contient les faits du sujet");
        yes(passage.length() <= 1600, "L'extrait reste borné");
        String unrelatedLive = "DIRECT. Une visite à Paris ce dimanche. "
                + "Un débat sur les élections et les transports.";
        yes(RssPublisherArticlePolicy.relevantLivePassage(
                headline, unrelatedLive).isEmpty(), "Jamais inventer l'article absent");
        yes(RssPublisherArticlePolicy.relevantLivePassage(
                "Un article ordinaire", publicLivePage).isEmpty(),
                "Aucune substitution de passage pour un article non-DIRECT");
        System.out.println("RssLivePublisherPolicyTest OK");
    }
}
