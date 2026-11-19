"""Regression checks: unsolicited Vosk phrases must never become questions by default."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"

class VoiceFalseTriggersV311(unittest.TestCase):
    def test_followups_require_opt_in(self):
        src = (JAVA / "FridayService.java").read_text(encoding="utf-8")
        self.assertIn('settings.get("followup_handsfree", "0")', src)
        self.assertIn('BargeInPolicy.startsWithWake(heard) && RustWake.detect(heard)', src)
        self.assertIn("now < wakeOnlyUntil", src)
        self.assertIn("now - lastSpeechEndedAt >= TtsEchoGate.TAIL_MS", src)
        self.assertIn("wakeOnlyUntil = now + 12_000L", src)
        self.assertIn("wakeOnlyUntil = 0;", src)
        self.assertNotIn('} else if (wakeArmed && (now - lastReplyAt <= followupMs', src)

    def test_settings_are_explicit_and_nonstreaming_by_default(self):
        ui = (JAVA / "MainActivity.java").read_text(encoding="utf-8")
        service = (JAVA / "FridayService.java").read_text(encoding="utf-8")
        self.assertIn('prefs.get("followup_handsfree", "0")', ui)
        self.assertIn('prefs.put("followup_handsfree"', ui)
        self.assertIn('followupHandsfreeEnabled.isChecked()', ui)
        self.assertIn('prefs.put("stream_enabled", "0")', ui)
        self.assertIn('LocalJarvisEngine.answer(this, settings, question, turn.api)', service)
        self.assertIn('prefs.get("listen_once", "0")', ui)
        self.assertIn('settings.get("barge_in_enabled", "0")', service)

if __name__ == "__main__":
    unittest.main()
