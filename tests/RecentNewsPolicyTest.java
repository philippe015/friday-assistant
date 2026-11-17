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
