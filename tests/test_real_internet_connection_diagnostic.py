"""FRIDAY: Internet status must be measured, not described from a toggle."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"
WEB = (JAVA / "WebSearch.java").read_text(encoding="utf-8")
SEARCH = (JAVA / "JarvisInternet.java").read_text(encoding="utf-8")
VOICE = (JAVA / "FridayService.java").read_text(encoding="utf-8")
TEXT = (JAVA / "MainActivity.java").read_text(encoding="utf-8")


class VerifiedConnectivity(unittest.TestCase):
    def test_voice_and_text_route_status_to_live_diagnostic(self):
        self.assertIn("WebSearch.requestsLiveConnectivityCheck(question)", VOICE)
        self.assertIn("JarvisInternet.liveConnectionDiagnostic(settings, turn.api)", VOICE)
        self.assertIn("WebSearch.requestsLiveConnectivityCheck(request)", TEXT)
        self.assertIn("JarvisInternet.liveConnectionDiagnostic(", TEXT)
        self.assertIn("PersonalMemory.command(settings, question)", VOICE)
        self.assertIn("PersonalMemory.command(prefs, request)", TEXT)
        self.assertIn("WebSearch.assistantAnswer", VOICE)
        self.assertIn("WebSearch.assistantAnswer", TEXT)

    def test_verify_real_https_not_toggle_alone(self):
        section = SEARCH.split("public static String liveConnectionDiagnostic(", 1)[1].split(
            "/** Vérification réseau courte", 1
        )[0]
        self.assertIn("quickConnectivitySources(cancellation)", section)
        self.assertIn("if (!enabled(settings))", section)
        self.assertIn("recherche HTTPS réelle", section)
        self.assertIn("la recherche HTTPS", section)
        self.assertNotIn("PersonalMemory", section)
        self.assertNotIn("FridayLocation", section)
        self.assertNotIn("historySnapshot()", section)

    def test_safe_fixed_probe_and_distinct_sources(self):
        section = SEARCH.split("private static List<Source> quickConnectivitySources(", 1)[1].split(
            "/** Test du moteur", 1
        )[0]
        self.assertIn('String query = "systeme solaire"', section)
        self.assertIn("fr.wikipedia.org/w/api.php", section)
        self.assertIn("www.bing.com/search?q=", section)
        self.assertIn("www.wikidata.org/w/api.php", section)
        self.assertIn("fetch(wiki, true)", section)
        self.assertIn("fetch(wikidata, true)", section)
        self.assertIn("mergeRelevant", section)
        self.assertIn("cancellation.check()", section)
        self.assertIn("4_000", SEARCH)
        self.assertIn("6_000", SEARCH)
        self.assertIn("WebPageReader.checkedUri(url)", SEARCH)
        self.assertIn("setInstanceFollowRedirects(false)", SEARCH)
        self.assertNotIn("api.mistral.ai", SEARCH)

    def test_existing_news_and_web_routes_remain(self):
        self.assertIn("public static String probeRecentNews()", SEARCH)
        self.assertIn("RecentNewsPolicy.newsWindowHours(q)", SEARCH)
        self.assertIn("JarvisInternet.shouldResearch(settings, question, privateDialogue)", VOICE)
        self.assertIn("JarvisInternet.shouldResearch(prefs, request, privateDialogue)", TEXT)
        self.assertIn("this, settings, question, privateDialogue, turn.api)", VOICE)
        self.assertIn("JarvisInternet.answer(", TEXT)


if __name__ == "__main__":
    unittest.main()
