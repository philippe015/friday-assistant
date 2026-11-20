"""FRIDAY v3.13.4 regression: continued listening after a TTS reply."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"
SERVICE = (JAVA / "FridayService.java").read_text(encoding="utf-8")
GATE = (JAVA / "TtsEchoGate.java").read_text(encoding="utf-8")
UI = (JAVA / "MainActivity.java").read_text(encoding="utf-8")

class ContinuousDialogue(unittest.TestCase):
    def test_rearm_in_finally_after_speaking(self):
        suffix = SERVICE.split("private void answerTurn(", 1)[1].split(
            "private String streamAndSpeak(", 1)[0]
        after = suffix.split("} finally {", 1)[1]
        self.assertIn("TtsEchoGate.followupUntil(finishedAt, false)", after)
        self.assertIn("wakeOnlyUntil = Math.max(wakeOnlyUntil", after)
        self.assertIn('settings.get("listen_once", "0")', after)
        self.assertIn("echoBlockUntil = TtsEchoGate.afterVoice(finishedAt)", after)
        self.assertIn("currentTurn = null;", after)
        self.assertNotIn("shutdown();", after)

    def test_vosk_gate_and_rearm(self):
        self.assertIn("TtsEchoGate.followupAccepts(now, echoBlockUntil, wakeOnlyUntil)", SERVICE)
        self.assertIn("TtsEchoGate.suppressGeneralAsr(", SERVICE)
        self.assertIn("generalInputBlocked = false;", SERVICE)
        self.assertIn('voiceState(TtsEchoGate.followupAccepts(', SERVICE)
        self.assertIn("RECORDSTATE_RECORDING", SERVICE)
        self.assertIn("if (running && retryMs > 0)", SERVICE)
        self.assertIn("if (running && !oneShotTriggered)", SERVICE)

    def test_20s_after_echo_no_infinite_handsfree(self):
        self.assertIn("return oneShot ? 0L : afterVoice(finishedAtMs) + 20_000L;", GATE)
        self.assertIn("nowMs >= echoBlockUntilMs && nowMs < followupUntilMs", GATE)
        self.assertIn("TAIL_MS = 2500L", GATE)

    def test_user_can_still_explicitly_choose_one_shot(self):
        self.assertIn('prefs.put("listen_once", oneShotEnabled.isChecked() ? "1" : "0")', UI)
        self.assertIn("Mode une question (case cochée)", UI)
        self.assertIn("posez directement votre prochaine question", SERVICE)

if __name__ == "__main__":
    unittest.main()
