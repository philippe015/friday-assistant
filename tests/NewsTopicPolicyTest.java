import fr.friday.assistant.NewsTopicPolicy;
import fr.friday.assistant.RecentNewsPolicy;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class NewsTopicPolicyTest {
    private static void check(boolean b, String label) {
        if (!b) throw new AssertionError(label);
    }
    public static void main(String[] args) {
        check("science".equals(NewsTopicPolicy.topic(
                "Cherche les actualités scientifiques publiées aujourd'hui et cite les sources.")),
                "ne pas chercher « scientifiques publiées cite sources »");
        check("mars science".equals(NewsTopicPolicy.topic(
                "Quelles sont les dernières actualités scientifiques sur Mars aujourd'hui ?")),
                "préserver Mars comme sujet précis");
        check("energie".equals(NewsTopicPolicy.topic("Actualités de l'énergie du jour")),
                "préserver les sujets non scientifiques");
        check(NewsTopicPolicy.matchesGeneralFeed("science",
                "Une découverte en physique quantique", "Publié par un laboratoire"),
                "science avec vocabulaire du domaine");
        check(!NewsTopicPolicy.matchesGeneralFeed("science",
                "Assemblée nationale : débats budgétaires", "Politique et impôts"),
                "ne pas confondre actualités politiques et scientifiques");
        long now = ZonedDateTime.of(2026, 11, 21, 8, 10, 0, 0,
                ZoneId.of("Europe/Paris")).toInstant().toEpochMilli();
        long today = ZonedDateTime.of(2026, 11, 21, 0, 0, 0, 0,
                ZoneId.of("Europe/Paris")).toInstant().toEpochMilli();
        check(RecentNewsPolicy.civilTodayStartMillis("Actualités scientifiques publiées aujourd'hui",
                now, ZoneId.of("Europe/Paris")) == today,
                "aujourd'hui signifie minuit local");
        check(RecentNewsPolicy.civilTodayStartMillis("actualités des 7 dernières heures",
                now, ZoneId.of("Europe/Paris")) == 0,
                "une fenêtre de 7 h ne doit pas devenir une journée entière");
        System.out.println("NewsTopicPolicyTest OK");
    }
}
