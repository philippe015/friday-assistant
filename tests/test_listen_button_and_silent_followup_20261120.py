"""Regression du bouton d'écoute et des nouvelles questions pendant une génération silencieuse."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"
UI = (JAVA / "MainActivity.java").read_text(encoding="utf-8")
SERVICE = (JAVA / "FridayService.java").read_text(encoding="utf-8")
POLICY = (JAVA / "VoiceTurnPolicy.java").read_text(encoding="utf-8")
SCRIPT = (ROOT / "scripts/tester_sources.sh").read_text(encoding="utf-8")


class NoAnswerRecovery(unittest.TestCase):
    def test_listen_button_reopens_direct_voice_window(self):
        segment = UI.split("private void startListening(boolean automatically) {", 1)[1].split(
            "private void startServiceUnchecked()", 1
        )[0]
        self.assertIn("if (FridayService.isActive()) {", segment)
        self.assertIn(".setAction(FridayService.ACTION_INTERRUPT)", segment)
        self.assertIn("posez une question sans dire Jarvis", segment)
        self.assertIn("startServiceUnchecked();", segment)

    def test_silent_inference_does_not_block_new_wake(self):
        self.assertIn("VoiceTurnPolicy.canReplace(activeResponse, speakingNow,", SERVICE)
        self.assertIn("BargeInPolicy.canBarge(heard, spokenText)", SERVICE)
        self.assertIn("!speakingNow || bargeEnabled", POLICY)
        self.assertIn("responseActive && confirmedWakeNotEcho", POLICY)

    def test_voice_playback_stays_protected(self):
        self.assertIn('if (speakingNow && !', (JAVA / "TtsEchoGate.java").read_text(encoding="utf-8")
                      + "if (speakingNow && !")
        self.assertIn("VoiceTurnPolicyTest.java", SCRIPT)
        self.assertIn("VoiceTurnPolicyTest RssFreshnessTest", SCRIPT)

if __name__ == "__main__":
    unittest.main()
