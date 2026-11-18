"""Contrôle structurel du décodeur STOP distinct de la dictée générale."""
import re
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"

class StopDetectorIntegration(unittest.TestCase):
    def test_same_pcm_source_two_vosk_recognizers(self):
        service = (JAVA / "FridayService.java").read_text()
        stop = service.index("stopRecognizer.acceptWaveForm(buffer, count)")
        normal = service.index("recognizer.acceptWaveForm(buffer, count)")
        self.assertLess(stop, normal)
        self.assertIn("stopRecognizer.setGrammar(VoiceStopSpotter.GRAMMAR)", service)
        self.assertIn("if (currentTurn != null && bargeEnabled", service)
        self.assertIn("stopSpotter.accept(", service)
        self.assertIn("stopRecognizer.close()", service)
        self.assertIn("interruptResponse(action == VoiceStopSpotter.Action.STOP_MIC", service)
        self.assertIn('event("barge_diag", interruptDiagnostics)', service)

    def test_cockpit_diagnostics_are_live(self):
        activity = (JAVA / "MainActivity.java").read_text()
        html = (ROOT / "app/src/main/assets/index.html").read_text()
        self.assertIn("public String voiceInterruptTelemetry()", activity)
        self.assertIn("FridayService.interruptionDiagnostics()", activity)
        self.assertIn("id=\"bargeState\"", html)
        self.assertIn("call('voiceInterruptTelemetry')", html)
        self.assertIn("kind==='barge_diag'", html)

    def test_grammar_static_bounded_and_unknown_fallback(self):
        src = (JAVA / "VoiceStopSpotter.java").read_text()
        self.assertIn('[unk]', src)
        self.assertIn('enum Action { NONE, STOP, CLARIFY, STOP_MIC }', src)
        self.assertIn("confirmations >= 2", src)
        self.assertIn("BargeInPolicy.explicitControlWhileSpeaking", src)

    def test_version_and_profile_gps_regressions(self):
        gradle = (ROOT / "app/build.gradle").read_text()
        code = re.search(r"versionCode (\d+)", gradle)
        self.assertIsNotNone(code)
        self.assertGreaterEqual(int(code.group(1)), 61)
        version = re.search(r"versionName '(\d+)\.(\d+)\.(\d+)'", gradle)
        self.assertIsNotNone(version)
        self.assertGreaterEqual(tuple(map(int, version.groups())), (3, 7, 2))
        native = (JAVA / "NativeAI.java").read_text()
        self.assertIn("PersonalityProfiles.activePrompt", native)
        service = (JAVA / "FridayService.java").read_text()
        self.assertIn('settings.get("location_share", "0")', service)

if __name__ == "__main__":
    unittest.main()
