package fr.friday.assistant;

/** Régressions isolées pour le vrai diagnostic du 22 novembre 2026 :
 * Google News sans domaine préprogrammé et retour TTS dans les partials. */
public final class RssSourceEchoFixTest {
    private static void ok(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        String rss = "<item><title>Un titre RSS - Média Test</title>"
                + "<source url=\"https://www.media-inconnu.fr\">Média Test</source>"
                + "<description>Extrait public</description></item>";
        ok("media-inconnu.fr".equals(
                RssPublisherArticlePolicy.domainFromRssSource(rss)),
                "Domaine inconnu extrait depuis source URL Google RSS");
        ok("journal.example".equals(
                RssPublisherArticlePolicy.domainFromRssSource(
                        "<source url='http://www.journal.example'>Journal</source>")),
                "URL http utilisée seulement comme métadonnée, jamais visitée en HTTP");
        ok(RssPublisherArticlePolicy.domainFromRssSource(
                "<source url='http://localhost:3000/'>Intranet</source>").isEmpty(),
                "Aucun localhost ni port interne");
        ok(RssPublisherArticlePolicy.domainFromRssSource(
                "<source url='https://127.0.0.1/'>Privé</source>").isEmpty(),
                "Aucun hôte IP privé");
        ok(RssPublisherArticlePolicy.domainFromRssSource(
                "<source url='https://editeur.example:8080/'>Privé</source>").isEmpty(),
                "Pas de port personnalisé");
        ok(RssPublisherArticlePolicy.domainFromRssSource(
                "<source url='javascript:alert(1)'>JS</source>").isEmpty(),
                "Pas de script ni pseudo-URL");
        ok(RssPublisherArticlePolicy.domainFromRssSource(
                "<source url='https://user:pw@editeur.example/'>Identifiants</source>").isEmpty(),
                "Pas de credentials");
        ok(RssPublisherArticlePolicy.domainFromRssSource(
                "<description>Pas de source</description>").isEmpty(),
                "Absence de source n'invente jamais d'éditeur");

        String tts = "Le flux RSS contient bien l article un, mais ce site "
                + "ne permet pas de lire sa page : le lien Google Actualités "
                + "ne fournit pas de page d éditeur identifiable.";
        long now = 10_000L, earlier = 9_900L;
        ok(VoiceSelfEchoPolicy.suppressEchoPartial(
                "le flux RSS contient bien l article un mais ce site",
                tts, now, earlier), "Masquer l'écho TTS dans les partials");
        ok(VoiceSelfEchoPolicy.suppressEchoPartial(
                "ce site ne permet pas de lire sa page",
                tts, now, earlier), "Masquer un sous-passage TTS");
        ok(!VoiceSelfEchoPolicy.suppressEchoPartial(
                "et demain", tts, now, earlier),
                "Ne pas masquer une relance courte");
        ok(!VoiceSelfEchoPolicy.suppressEchoPartial(
                "Jarvis explique la météo à Paris demain",
                tts, now, earlier), "Ne pas masquer une nouvelle question");
        ok(!VoiceSelfEchoPolicy.suppressEchoPartial(
                "le flux RSS contient bien l article un",
                tts, now + 60_000L, earlier), "Fin de mémoire anti-écho");
        System.out.println("RssSourceEchoFixTest OK");
    }
}
