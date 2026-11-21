"""Régression du mot d'activation français et des diagnostics éphémères."""
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app/src/main/java/fr/friday/assistant"
SERVICE = (SRC / "FridayService.java").read_text(encoding="utf-8")
BARGE = (SRC / "BargeInPolicy.java").read_text(encoding="utf-8")
VOICE = (SRC / "VoiceText.java").read_text(encoding="utf-8")
HUD = (ROOT / "app/src/main/assets/index.html").read_text(encoding="utf-8")


class FrenchWakeRegression(unittest.TestCase):
    def test_one_wake_policy_for_all_names(self):
        self.assertIn("VoiceText.startsWithWakeWord(phrase)", BARGE)
        self.assertIn("friday|fridai|fridey|freday", VOICE)
        self.assertIn("jar vis|jar vies", VOICE)
        self.assertIn("WAKE.matcher(normalize(heard).trim()).lookingAt()", VOICE)
        self.assertIn("BargeInPolicy.startsWithWake(heard) && RustWake.detect(heard)", SERVICE)

    def test_ignored_transcription_visible_and_ephemeral(self):
        self.assertIn('event("asr_rejected", heard.trim());', SERVICE)
        self.assertIn('if(kind===\'asr_rejected\')', HUD)
        self.assertIn("state.previewRejected=true", HUD)
        self.assertIn("}},15000)", HUD)
        self.assertIn("non conservée", HUD)
        self.assertNotIn("log(s)", HUD.split("if(kind==='asr_rejected')", 1)[1].split("}else if(kind==='error')", 1)[0])

    def test_no_private_speech_in_logs(self):
        self.assertIn('asrDiagnostic("Phrase transcrite ; mot-clé Jarvis ou Friday absent")', SERVICE)
        self.assertNotIn('asrDiagnostic("Phrase transcrite : " + heard)', SERVICE)
        self.assertIn("if (kind.equals(\"user\") || kind.equals(\"assistant\")) replayVoiceHistory();",
                      (SRC / "MainActivity.java").read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
