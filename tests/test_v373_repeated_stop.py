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
        self.assertIn("if (finalResult || repeated", spotter)
        self.assertIn("BargeInPolicy.startsWithWake(recognized) && confirmations >= 2", spotter)

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
        # Version displayed by cockpit must match the actual Gradle version.
        import re
        declared = re.search(r"versionName '(\d+)\.(\d+)\.(\d+)'", gradle)
        self.assertIsNotNone(declared)
        self.assertIn("v" + ".".join(declared.groups()), html)
        import re
        code = re.search(r"versionCode (\d+)", gradle)
        self.assertIsNotNone(code)
        self.assertGreaterEqual(int(code.group(1)), 62)
        version = re.search(r"versionName '(\d+)\.(\d+)\.(\d+)'", gradle)
        self.assertIsNotNone(version)
        self.assertGreaterEqual(tuple(map(int, version.groups())), (3, 7, 3))
        self.assertIn("voiceInterruptTelemetry", html)

if __name__ == "__main__":
    unittest.main()
