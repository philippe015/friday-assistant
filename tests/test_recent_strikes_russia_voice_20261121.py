"""FRIDAY: les frappes en Russie des dix dernières heures.

Tests déterministes sans réseau : le filtre RSS n'invente aucun article.
"""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"
POLICY = (JAVA / "RecentNewsPolicy.java").read_text(encoding="utf-8")
WEB = (JAVA / "JarvisInternet.java").read_text(encoding="utf-8")
FRESH = (JAVA / "RssFreshness.java").read_text(encoding="utf-8")

class RussiaStrikeFollowup(unittest.TestCase):
    def test_java_french_hour_words_and_military_event_queries(self):
        if not shutil.which("javac") or not shutil.which("java"):
            self.skipTest("JDK absent")
        smoke = r"""
import fr.friday.assistant.RecentNewsPolicy;
public class StrikesSmoke {
    private static void check(boolean condition, String why) {
        if (!condition) throw new AssertionError(why);
    }
    public static void main(String[] args) {
        String asr = "les disputes frappe de la russie dans les dix dernières heures";
        check(RecentNewsPolicy.requestedHours(asr)==10, "10 hours ASR");
        check(RecentNewsPolicy.newsWindowHours(asr)==10, "spoken strikes use RSS");
        check(RecentNewsPolicy.requiresRecentNews(asr), "web evidence required");
        check(RecentNewsPolicy.newsWindowHours(
              "les dernières frappes russes des 10 dernières heures")==10,
              "numeric hours");
        check(RecentNewsPolicy.newsWindowHours(
              "les bombardements en Ukraine durant les huit dernières heures")==8,
              "8h strikes");
        check(RecentNewsPolicy.newsWindowHours("actualités de la Russie")==24,
              "daily news unchanged");
        check(RecentNewsPolicy.newsWindowHours(
              "parle-moi des drones de la Russie")==0,
              "no hourly constraint -> no forced RSS");
        check(RecentNewsPolicy.newsWindowHours("je pars dans dix heures")==0,
              "non-news time");
        check(RecentNewsPolicy.relevantStrikeReport(
              "frappes russie", "Attaque de drones russes signalée", ""),
              "related attack");
        check(!RecentNewsPolicy.relevantStrikeReport(
              "frappes russie", "La Russie équipe ses drones d'une nouvelle technologie", ""),
              "equipment not a strike");
        check(!RecentNewsPolicy.relevantStrikeReport(
              "frappes russie", "Pourquoi l'Ukraine privilégie le Mirage 2000", ""),
              "irrelevant airplane");
    }
}
"""
        with tempfile.TemporaryDirectory() as tmp:
            p = Path(tmp)
            (p/"StrikesSmoke.java").write_text(smoke, encoding="utf-8")
            subprocess.run(["javac","-encoding","UTF-8","-d",str(p),
                 str(JAVA/"RecentNewsPolicy.java"),str(p/"StrikesSmoke.java")],
                capture_output=True,text=True,check=True)
            subprocess.run(["java","-cp",str(p),"StrikesSmoke"],
                capture_output=True,text=True,check=True)

    def test_rss_routing_preserves_time_and_source_validation(self):
        self.assertIn("RecentNewsPolicy.newsWindowHours(q)", WEB)
        self.assertIn("boolean strikeRequest = RecentNewsPolicy.isStrikeTopic(topic)", WEB)
        self.assertIn('strikeRequest ? topic : ""', WEB)
        self.assertIn("RecentNewsPolicy.relevantStrikeReport(", WEB)
        self.assertIn("RssFreshness.withinHours(published, now, recentHours)", WEB)
        self.assertIn("candidate.publishedAt <= 0", WEB)
        self.assertIn("pubDate", WEB)
        self.assertIn("Aucun article correspondant trouvé avec un horodatage", WEB)
        self.assertIn("publishedAt >= now - hours * 3600_000L", FRESH)

    def test_transcription_fix_is_narrow_and_private_context_is_not_searched(self):
        self.assertIn('words.replaceAll("\\\\bdisputes?\\\\s+frappes?\\\\b", "frappes")', WEB)
        self.assertIn("sources = research(question, cancellation)", WEB)
        self.assertNotIn("research(privateDialogue", WEB)
        self.assertNotIn("search(privateDialogue", WEB)

if __name__=="__main__":
    unittest.main()
