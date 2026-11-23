import fr.friday.assistant.RecentNewsPolicy;
import java.util.TimeZone;

public final class RecentNewsPolicyTest {
    private static void ok(boolean b, String name) {
        if (!b) throw new AssertionError(name);
    }
    public static void main(String[] args) {
        ok(RecentNewsPolicy.requiresRecentNews("actualités des 7 dernières heures"),
                "sept dernières heures avec chiffres");
        ok(RecentNewsPolicy.requiresRecentNews("friday, actualité des 7 dernier heure"),
                "reconnaissance vocale imparfaite");
        ok(RecentNewsPolicy.requiresRecentNews("les informations des sept dernières heures"),
                "sept écrit en toutes lettres");
        ok(RecentNewsPolicy.requestedHours("nouvelles des 12 dernières heures") == 12,
                "période 12h");
        ok(!RecentNewsPolicy.requiresRecentNews("quelle heure est-il ?"), "question locale");
        ok(!RecentNewsPolicy.requiresRecentNews("actualités du jour"), "pas de période horaire");
        ok(RecentNewsPolicy.newsWindowHours(
                "j'ai envie d'un mode les informations de la nuit") == 24,
                "Bulletin de nuit reconnu malgré transcription imparfaite");
        ok(RecentNewsPolicy.cleanVoiceNewsRequest(
                "j'ai envie d'un mode les informations de la nuit")
                .equals("les informations de la nuit"),
                "L'amorce envie n'est pas un sujet de bulletin");
        ok(RecentNewsPolicy.cleanVoiceNewsRequest(
                "j'ai envie donne les informations les dernières informations")
                .equals("les informations les dernieres informations"),
                "La demande de nouvelles générales ne doit pas devenir envie");
        ok(RecentNewsPolicy.cleanVoiceNewsRequest(
                "actualités sur le manque d'envie").contains("envie"),
                "Ne pas effacer un vrai sujet parlant d'envie");
        ok(RecentNewsPolicy.newsWindowHours("actualités du jour") == 24,
                "Les actualités du jour déclenchent un flux daté sur 24 h");
        ok(RecentNewsPolicy.newsWindowHours("Journal des nouvelles") == 24,
                "Le journal récent doit chercher des titres datés");
        ok(RecentNewsPolicy.newsWindowHours("Quelles sont les dernières informations sur DeepSeek") == 24,
                "Dernières informations est une demande d'actualité");
        ok(RecentNewsPolicy.newsWindowHours("actualités des 7 dernières heures") == 7,
                "La période horaire explicite doit être respectée");
        ok(RecentNewsPolicy.newsWindowHours("Donne-moi des informations sur les batteries") == 0,
                "Les informations générales ne sont pas un bulletin d'actualités");
        ok(RecentNewsPolicy.newsWindowHours("Explique les informations du code du travail") == 0,
                "Le droit général doit rester une recherche non temporelle");
        ok(RecentNewsPolicy.requestedHours("actualités des 99 dernières heures") == 0,
                "limite de recherche raisonnable");
        ok(RecentNewsPolicy.genericWebDenial("Je n'ai pas accès à Internet."),
                "dénégation erronée");
        ok(RecentNewsPolicy.genericWebDenial("Je ne peux pas accéder aux pages web."),
                "dénégation d'accès aux pages");
        ok(!RecentNewsPolicy.genericWebDenial("Voici 3 articles trouvés sur Internet."),
                "réponse sourcée");
        String p = RecentNewsPolicy.searchPrompt("actualités des 7 dernières heures",
                1763334000000L, TimeZone.getTimeZone("Europe/Paris"));
        ok(p.contains("dernières 7 heures"), "indiquer fenêtre");
        ok(p.contains("date/heure de publication"), "horodatage source");
        ok(p.contains("web_search"), "demande recherche réelle");
        System.out.println("RecentNewsPolicyTest OK");
    }
}
