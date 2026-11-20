"""FRIDAY: recherche multi-source avant les questions générales, hors données privées."""
from pathlib import Path
import unittest

BASE = Path(__file__).resolve().parents[1]
JAVA = BASE / "app/src/main/java/fr/friday/assistant"
WEB = (JAVA / "JarvisInternet.java").read_text(encoding="utf-8")
SEARCH = (JAVA / "WebSearch.java").read_text(encoding="utf-8")
UI = (JAVA / "MainActivity.java").read_text(encoding="utf-8")
VOICE = (JAVA / "FridayService.java").read_text(encoding="utf-8")

class WebEveryAnswer(unittest.TestCase):
    def test_enabled_by_default_but_user_can_disable(self):
        self.assertIn('settings.get("jarvis_web_every_answer", "1")', WEB)
        self.assertIn('prefs.get("jarvis_web_every_answer", "1")', UI)
        self.assertIn('prefs.put("jarvis_web_every_answer",', UI)
        self.assertIn("jarvisWebEveryAnswerEnabled.isChecked()", UI)

    def test_private_operations_remain_local(self):
        self.assertIn("MemoryIntent.kind(question) != MemoryIntent.NONE", WEB)
        self.assertIn("LocationIntent.nearby(question)", WEB)
        self.assertIn("WebSearch.safeForGeneralSearch(question)", WEB)
        self.assertNotIn("NativeAI.answer(", WEB)
        self.assertNotIn("FridayLocation.", WEB)
        self.assertNotIn("PersonalMemory.search(", WEB)

    def test_federated_search_providers(self):
        for host in ("www.bing.com", "html.duckduckgo.com", "fr.wikipedia.org",
                     "www.wikidata.org", "api.crossref.org", "news.google.com"):
            self.assertIn(host, WEB)
        self.assertIn("parseWikidataSearch", WEB)
        self.assertIn("parseCrossrefSearch", WEB)
        self.assertIn("parseWikipediaSearch", WEB)
        self.assertIn("mergeRelevant", WEB)
        self.assertIn("WebPageReader.checkedUri(url)", WEB)
        self.assertIn("setInstanceFollowRedirects(false)", WEB)

    def test_time_sensitive_protection_and_offline_fallback(self):
        self.assertIn("WebSearch.requiresCurrentEvidence(question)", WEB)
        self.assertIn("Je ne peux pas vérifier les informations", WEB)
        self.assertIn("Réponse générale locale, non vérifiée sur Internet", WEB)
        self.assertIn("HTTP 202", WEB)
        self.assertIn("FRIDAY", SEARCH)

    def test_both_voice_and_text_share_same_routing(self):
        self.assertIn("JarvisInternet.shouldResearch(settings, question)", VOICE)
        self.assertIn("JarvisInternet.shouldResearch(prefs, request)", UI)
        self.assertIn("JarvisInternet.answer(this, settings, question, turn.api)", VOICE)
        self.assertIn("JarvisInternet.answer(", UI)

if __name__ == "__main__":
    unittest.main()
