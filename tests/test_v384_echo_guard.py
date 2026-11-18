"""FRIDAY v3.8.4: prevent assistant TTS from becoming a new user request."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"

class EchoRegression(unittest.TestCase):
    def test_general_asr_is_gated_after_stop_recognizer(self):
        src = (JAVA / "FridayService.java").read_text(encoding="utf-8")
        start = src.index("stopRecognizer.acceptWaveForm(buffer, count)")
        gate = src.index("TtsEchoGate.suppressGeneralAsr(", start)
        general = src.index("recognizer.acceptWaveForm(buffer, count)", gate)
        self.assertLess(start, gate)
        self.assertLess(gate, general)
        block = src[gate:general]
        self.assertIn("if (blocked)", block)
        self.assertIn("continue;", block)
        self.assertIn("recognizer.close(); recognizer = createRecognizer(model);", block)
        self.assertIn("if (generalInputBlocked)", block)

    def test_stop_remains_available_and_cooldown_precedes_release(self):
        src = (JAVA / "FridayService.java").read_text(encoding="utf-8")
        self.assertIn("stopRecognizer.setGrammar(VoiceStopSpotter.GRAMMAR)", src)
        self.assertIn("stopSpotter.accept(", src)
        self.assertIn("interruptResponse(action == VoiceStopSpotter.Action.STOP_MIC", src)
        start = src.index("private synchronized void interruptResponse(String status)")
        end = src.index("private static double[] confidenceStats", start)
        interrupt = src[start:end]
        self.assertLess(interrupt.index("echoBlockUntil = TtsEchoGate.afterStop(stoppedAt)"),
                        interrupt.index("currentTurn = null"))
        self.assertLess(interrupt.index("tts.stop()"), interrupt.index("previous.cancel()"))
        final = src[src.index("} finally {", src.index("private void answerTurn(")):
                    src.index("/** File bornée", src.index("private void answerTurn("))]
        self.assertLess(final.index("echoBlockUntil = TtsEchoGate.afterVoice(finishedAt)"),
                        final.index("currentTurn = null"))

    def test_version_and_privacy(self):
        gradle = (ROOT / "app/build.gradle").read_text(encoding="utf-8")
        self.assertRegex(gradle, r"versionCode (?:67|68|69|70|71|72|73|74|75|76|77|78|79)")
        self.assertRegex(gradle, r"versionName '(?:3\.8\.[456789]|3\.9\.0|3\.10\.0|3\.11\.0|3\.11\.1|3\.11\.2|3\.12\.0|3\.12\.1)'")
        memory = (JAVA / "PersonalMemory.java").read_text(encoding="utf-8")
        self.assertIn('cfg.get("memory_share_mistral", "0")', memory)
        ai = (JAVA / "NativeAI.java").read_text(encoding="utf-8")
        self.assertIn('MISTRAL_BASE + "/conversations"', ai)
        self.assertIn('new JSONObject().put("type", "web_search")', ai)

if __name__ == "__main__":
    unittest.main()
