"""FRIDAY v3.8.7: voice request 'actualités des 7 dernières heures' must search Web."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"

def source(name):
    return (JAVA / name).read_text(encoding="utf-8")

class RecentVoiceNewsV387(unittest.TestCase):
    def test_spoken_news_reaches_real_web_even_when_streaming(self):
        service = source("FridayService.java")
        ai = source("NativeAI.java")
        self.assertIn('RecentNewsPolicy.requiresRecentNews(prepared)', service)
        self.assertIn("String prepared = PythonBrain.prepareQuestion(question)", service)
        self.assertIn("NativeAI.answerStreaming(settings, question, history,", service)
        self.assertIn('WebSearch.shouldSearch(settings.get("web_mode", "always"), question, previous)', ai)
        self.assertIn('MISTRAL_BASE + "/conversations"', ai)
        self.assertIn('new JSONObject().put("type", "web_search")', ai)

    def test_news_checks_absolute_time_window_and_avoids_old_chat_denials(self):
        ai = source("NativeAI.java")
        policy = source("RecentNewsPolicy.java")
        self.assertIn("RecentNewsPolicy.searchPrompt(input, System.currentTimeMillis()", ai)
        self.assertIn("recentNews ? Collections.emptyList() : history", ai)
        self.assertIn('Calendar.HOUR_OF_DAY, -hours', policy)
        self.assertIn("date/heure de publication", policy)
        self.assertIn("ne remplace pas", policy)

    def test_retry_when_tool_executed_without_references(self):
        ai = source("NativeAI.java")
        self.assertIn("boolean freshNews = RecentNewsPolicy.requiresRecentNews(question)", ai)
        self.assertIn("if (initial.references > 0", ai)
        self.assertIn("if (!freshNews && initial.executed", ai)
        self.assertIn("return conversation(cfg, key, DEFAULT_MODEL, targeted, history);", ai)
        self.assertIn("if (recentNews && (found.references == 0", ai)
        self.assertIn("RecentNewsPolicy.genericWebDenial(found.answer)", ai)
        self.assertIn("Je ne vais pas inventer des actualités", ai)
        self.assertIn("|| RecentNewsPolicy.requiresRecentNews(question)", ai)

    def test_preserve_privacy_and_other_functions(self):
        ai = source("NativeAI.java")
        voice = source("FridayService.java")
        memory = source("PersonalMemory.java")
        self.assertIn('cfg.get("memory_share_mistral", "0")', memory)
        self.assertIn("TtsEchoGate.suppressGeneralAsr(", voice)
        self.assertIn("Recherche Jarvis Internet désactivée : actualités non vérifiables", voice)
        self.assertIn('new JSONObject().put("type", "web_search")', ai)

    def test_version(self):
        gradle = (ROOT / "app/build.gradle").read_text(encoding="utf-8")
        self.assertIn("versionCode 105", gradle)
        self.assertIn("versionName '3.14.11'", gradle)

if __name__ == "__main__":
    unittest.main()
