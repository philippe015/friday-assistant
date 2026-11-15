"""Vérifie le vrai mode une question et la grammaire optionnelle de v3.7.0."""
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"

class VocabularyAndBatteryIntegration(unittest.TestCase):
    def test_one_shot_mode_stops_micro_after_question(self):
        service = (JAVA / "FridayService.java").read_text()
        self.assertIn('settings.get("listen_once", "0")', service)
        self.assertIn("oneShotTriggered = true", service)
        self.assertIn("while (running && !oneShotTriggered)", service)
        self.assertIn("ar.stop();", service)
        self.assertIn("while (running && currentTurn != null)", service)

    def test_vosk_mode_opt_in_and_unknown_word_fallback(self):
        activity = (JAVA / "MainActivity.java").read_text()
        service = (JAVA / "FridayService.java").read_text()
        vocab = (JAVA / "VoiceVocabulary.java").read_text()
        self.assertIn('settings.get("vosk_hints_enabled", "0")', service)
        self.assertIn('settings.secret("vosk_hints")', service)
        self.assertIn("recognizer.setGrammar(grammar)", service)
        self.assertIn('prefs.setSecret("vosk_hints", hints)', activity)
        self.assertIn("[unk]", vocab)

    def test_ui_diagnostics(self):
        html = (ROOT / "app/src/main/assets/index.html").read_text()
        self.assertIn("listeningModeState", html)
        self.assertIn("vocabularyModeState", html)
        self.assertIn("call('listeningMode')", html)
        self.assertIn("call('vocabularyMode')", html)

    def test_version(self):
        gradle = (ROOT / "app/build.gradle").read_text()
        self.assertIn("versionCode 59", gradle)
        self.assertIn("versionName '3.7.0'", gradle)

if __name__ == "__main__":
    unittest.main()
