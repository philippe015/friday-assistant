"""FRIDAY v3.7.1 — stop audio, clarification et micro actif pendant la réponse."""
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"

def read(name):
    return (JAVA / name).read_text(encoding="utf-8")

class VoiceInterruptionRegression(unittest.TestCase):
    def test_tts_stop_comes_before_network_cancel(self):
        service = read("FridayService.java")
        start = service.index("private synchronized void interruptResponse(String status)")
        end = service.index("private static double[] confidenceStats", start)
        section = service[start:end]
        self.assertLess(section.index("tts.stop()"), section.index("previous.cancel()"))
        self.assertLess(section.index("currentTurn = null"), section.index("tts.stop()"))
        self.assertIn("synchronized (ttsControlLock)", section)

    def test_no_tts_speak_after_cancellation(self):
        service = read("FridayService.java")
        position = service.index("code = tts.speak(part,")
        before = service[position-400:position]
        self.assertIn("synchronized (ttsControlLock)", before)
        self.assertIn("!live(turn)", before)

    def test_no_long_http_disconnect_under_micro_lock(self):
        native = read("NativeAI.java")
        start = native.index("public void cancel()")
        end = native.index("synchronized void bind(", start)
        self.assertIn("cancelled = true;", native[start:end])
        self.assertIn("close.setDaemon(true)", native[start:end])
        self.assertIn("close.start()", native[start:end])
        self.assertNotIn("public synchronized void cancel()", native)

    def test_bare_commands_during_active_response_only(self):
        service = read("FridayService.java")
        self.assertIn("BargeInPolicy.explicitControlWhileSpeaking(candidate, spokenText)", service)
        self.assertIn("BargeInPolicy.earlyBarge(candidate, spokenText)", service)
        self.assertIn("BargeInPolicy.isBareStopAnswer(heard)", service)
        self.assertIn("currentTurn != null || now < bargePendingUntil", service)
        self.assertIn("BargeInPolicy.wantsToAddDetails(heard)", service)
        self.assertIn("oneShotTriggered = false;", service)

    def test_one_shot_keeps_micro_until_answer_is_done(self):
        service = read("FridayService.java")
        self.assertIn("if (oneShotTriggered && currentTurn == null)", service)
        self.assertNotIn("while (running && currentTurn != null)", service)
        activity = read("MainActivity.java")
        self.assertIn("micro reste actif", activity)

    def test_version_is_371(self):
        gradle = (ROOT / "app/build.gradle").read_text()
        import re
        code = re.search(r"versionCode (\d+)", gradle)
        self.assertIsNotNone(code)
        self.assertGreaterEqual(int(code.group(1)), 60)
        version = re.search(r"versionName '(\d+)\.(\d+)\.(\d+)'", gradle)
        self.assertIsNotNone(version)
        self.assertGreaterEqual(tuple(map(int, version.groups())), (3, 7, 1))

if __name__ == "__main__":
    unittest.main()
