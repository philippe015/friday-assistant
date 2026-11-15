"""FRIDAY v3.7.3 - repetitive stop and real mic signal diagnostics."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"

class RepeatedStopRegression(unittest.TestCase):
    def test_stop_grammar_and_policy_handle_repeated_words(self):
        policy = (JAVA / "BargeInPolicy.java").read_text()
        spotter = (JAVA / "VoiceStopSpotter.java").read_text()
        self.assertIn("0,9", policy)
        self.assertIn("stop stop stop", spotter)
        self.assertIn("arrete arrete", spotter)
        self.assertIn("finalResult || confirmations >= 2 || repeated", spotter)

    def test_meter_measures_real_pcm_captured_from_android(self):
        service = (JAVA / "FridayService.java").read_text()
        self.assertIn("MicSignalMeter.level(buffer, count)", service)
        self.assertIn("Micro", service)
        self.assertIn('"/100 (voix ou écho)"', service)
        self.assertIn("silencedByPolicy.get()", service)
        meter = (JAVA / "MicSignalMeter.java").read_text()
        self.assertIn("Math.sqrt(squared / samples)", meter)
        self.assertNotIn("http", meter.lower())

    def test_ui_and_version(self):
        html = (ROOT / "app/src/main/assets/index.html").read_text()
        gradle = (ROOT / "app/build.gradle").read_text()
        self.assertIn("v3.7.3", html)
        self.assertIn("versionCode 62", gradle)
        self.assertIn("versionName '3.7.3'", gradle)
        self.assertIn("voiceInterruptTelemetry", html)

if __name__ == "__main__":
    unittest.main()
