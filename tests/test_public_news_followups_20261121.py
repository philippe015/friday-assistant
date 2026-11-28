"""News follow-ups reuse only public RSS headlines, and hourly news stays factual."""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/fr/friday/assistant"
FOLLOW=(JAVA/"NewsFollowUpPolicy.java").read_text(encoding="utf-8")
WEB=(JAVA/"JarvisInternet.java").read_text(encoding="utf-8")
FRESH=(JAVA/"RssFreshness.java").read_text(encoding="utf-8")

class PublicNewsFollowUpTest(unittest.TestCase):
    def test_rss_dates_and_anchored_headline_followups_in_java(self):
        if not shutil.which("javac") or not shutil.which("java"):
            self.skipTest("JDK unavailable")
        smoke=r"""
import fr.friday.assistant.NewsFollowUpPolicy;
import fr.friday.assistant.RecentNewsPolicy;
import fr.friday.assistant.RssFreshness;
import java.time.OffsetDateTime;

public final class NewsFollowUpSmoke {
    static void require(boolean yes, String reason) {
        if (!yes) throw new AssertionError(reason);
    }
    public static void main(String[] args) {
        String previous="donne-moi les actualités sur les cinq dernières heures";
        String source = "J'ai trouvé 3 titres dans les cinq dernières heures.\n"
           + "Sources Internet consultées :\n"
           + "[1] Professeur menacé par un adolescent — source secondaire — "
           + "horodatage RSS 21/11/2026 14:01 GMT+01:00 — "
           + "https://news.google.com/rss/articles/abc?oc=5\n"
           + "[2] L’armée finlandaise a détecté une attaque de drone contre "
           + "un cargo russe dans le golfe d — source secondaire — "
           + "horodatage RSS 21/11/2026 13:59 GMT+01:00 — "
           + "https://news.google.com/rss/articles/def?oc=5\n"
           + "[3] Marco Bezzecchi en pole-position du GP — source secondaire — "
           + "horodatage RSS 21/11/2026 13:11 GMT+01:00 — "
           + "https://news.google.com/rss/articles/ghi?oc=5";
        String question = "donne-moi plus de précisions sur les attaques de drones";
        String enriched = NewsFollowUpPolicy.enrich(question, previous, source);
        require(!enriched.equals(question), "must resolve referenced public title");
        require(enriched.contains("cargo") && enriched.contains("russe")
                && enriched.contains("finlandaise"), "must select cargo RSS item");
        require(!enriched.contains("professeur") && !enriched.contains("bezzecchi"),
                "must not select unrelated articles");
        require(RecentNewsPolicy.newsWindowHours(enriched)==5,
                "follow-up must inherit 5-hour rolling window");
        require(NewsFollowUpPolicy.enrich(question, "ma santé personnelle", source)
                .equals(question), "private prior user data never used");
        require(NewsFollowUpPolicy.enrich(question, previous,
                "Discussion privée, sans source publique").equals(question),
                "private chat not a public source");
        require(NewsFollowUpPolicy.enrich("donne des détails sur le professeur",
                previous, source).equals("donne des détails sur le professeur"),
                "no hijack for unrelated follow-up");
        long now=OffsetDateTime.parse("2026-11-21T14:45:00+01:00")
                .toInstant().toEpochMilli();
        for (String t : new String[]{
                "2026-11-21T14:01:00+01:00",
                "2026-11-21T13:59:00+01:00",
                "2026-11-21T13:11:00+01:00"}) {
            long stamp=OffsetDateTime.parse(t).toInstant().toEpochMilli();
            require(RssFreshness.withinHours(stamp,now,5),
                    "user RSS timestamp should be in 5-hour window: "+t);
        }
        require(!RssFreshness.withinHours(
                OffsetDateTime.parse("2026-11-21T08:01:00+01:00")
                   .toInstant().toEpochMilli(),now,5),"old RSS must be rejected");
    }
}
"""
        with tempfile.TemporaryDirectory() as folder:
            t=Path(folder)
            (t/"NewsFollowUpSmoke.java").write_text(smoke,encoding="utf-8")
            cmd=["javac","-encoding","UTF-8","-d",str(t),
                str(JAVA/"NewsFollowUpPolicy.java"),
                str(JAVA/"RecentNewsPolicy.java"),
                str(JAVA/"RssFreshness.java"),
                str(t/"NewsFollowUpSmoke.java")]
            subprocess.run(cmd,check=True,text=True,capture_output=True)
            subprocess.run(["java","-cp",str(t),"NewsFollowUpSmoke"],
                           check=True,text=True,capture_output=True)

    def test_online_search_receives_only_public_headline_not_history(self):
        self.assertIn("PrivateConversationHistory.read(context, prefs)", WEB)
        self.assertIn("NewsFollowUpPolicy.enrich(currentQuestion",WEB)
        self.assertIn("sources = research(publicNewsQuery, cancellation)",WEB)
        self.assertIn("sources = research(question, cancellation)",WEB)
        self.assertNotIn("research(privateDialogue",WEB)
        self.assertNotIn("research(previousAssistant",WEB)
        self.assertNotIn("research(history",WEB)
        self.assertIn("https://news",FOLLOW)  # public RSS only

    def test_deterministic_timed_news_digest_never_guesses_publication_time(self):
        self.assertIn("private static String verifiedRecentHeadlines(",WEB)
        self.assertIn("RssFreshness.withinHours(source.publishedAt, now, hours)",WEB)
        self.assertIn("int newsHours = RecentNewsPolicy.newsWindowHours(publicNewsQuery)",WEB)
        self.assertIn("return new Result(spoken +",WEB)
        self.assertIn("Cela ne prouve ni l'heure de l'événement",WEB)
        self.assertIn("l'horodatage RSS se situe dans les",WEB)
        self.assertIn("publishedAt >= now - hours * 3600_000L",FRESH)

if __name__=="__main__": unittest.main()
