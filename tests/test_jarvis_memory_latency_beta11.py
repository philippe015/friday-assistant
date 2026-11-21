"""Regression tests for JARVIS memory depth, fast public research and stable voice."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"
MEMORY = (JAVA / "DialogueContext.java").read_text(encoding="utf-8")
MODEL = (JAVA / "LocalJarvisEngine.java").read_text(encoding="utf-8")
WEB = (JAVA / "JarvisInternet.java").read_text(encoding="utf-8")
VOICE = (JAVA / "FridayService.java").read_text(encoding="utf-8")
MAIN = (JAVA / "MainActivity.java").read_text(encoding="utf-8")
WEATHER = (JAVA / "WeatherForecast.java").read_text(encoding="utf-8")


class MemoryLatencyAndSourceReliability(unittest.TestCase):
    def test_context_adapts_to_followup_without_leaking_private_data(self):
        self.assertIn("MAX_DIALOGUE_MESSAGES = 14", MEMORY)
        self.assertIn("NORMAL_DIALOGUE_MESSAGES = 6", MEMORY)
        self.assertIn("MAX_DIALOGUE_CHARACTERS = 2600", MEMORY)
        self.assertIn("NORMAL_DIALOGUE_CHARACTERS = 1250", MEMORY)
        self.assertIn("DialogueMemoryPolicy.isContextualFollowUp(currentQuestion)", MEMORY)
        self.assertIn("DialogueMemoryPolicy.isPrivateDialogue(currentQuestion)", MEMORY)
        self.assertIn("maxMessages = followup ? MAX_DIALOGUE_MESSAGES", MEMORY)
        self.assertIn("PrivateConversationHistory.read(context, prefs)", MEMORY)
        self.assertIn("privateDialogue.length() <= limit", MODEL)
        self.assertIn("int limit = hasWebSources ? 800 : 2900", MODEL)
        self.assertIn("si le référent", MODEL.lower().replace("Si le référent", "si le référent"))
        self.assertNotIn("PrivateConversationHistory.", WEB)
        self.assertIn("research(question, cancellation)", WEB)
        self.assertNotIn("research(privateDialogue", WEB)

    def test_search_stops_only_with_source_diversity_and_preserves_freshness(self):
        self.assertIn("sufficientIndependentSources(found)", WEB)
        self.assertIn("hosts.size() >= 2", WEB)
        self.assertIn("sources.size() < 3", WEB)
        self.assertIn("!WebSearch.requiresCurrentEvidence(q)", WEB)
        self.assertIn("!WebSearch.scientificResearch(q)", WEB)
        self.assertIn("OfficialSourcePolicy.preferredSites(q).length == 0", WEB)
        self.assertIn("OfficialSourcePolicy.sourceLabel(source.url)", WEB)
        self.assertIn("RecentNewsPolicy.civilTodayStartMillis(", WEB)
        self.assertIn("RssFreshness.withinHours(", WEB)
        self.assertIn("int pagesToRead", WEB)

    def test_unchanged_proven_microphone_weather_and_stop(self):
        self.assertIn("BargeInPolicy.isStopListening(heard)", VOICE)
        self.assertIn("TtsEchoGate.suppressGeneralAsr(", VOICE)
        self.assertIn("WeatherDialogue.cityFollowup(settings, heard)", VOICE)
        self.assertIn("WeatherPlacePolicy.sameCountry(", WEATHER)
        self.assertIn("visibleListenWatchdog", MAIN)
        self.assertIn("AudioRecord", VOICE)
        self.assertIn("speak(speechAnswer, turn)", VOICE)


if __name__ == "__main__":
    unittest.main()
