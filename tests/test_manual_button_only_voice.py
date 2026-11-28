"""Regression: vocal STOP is removed; explicit button still interrupts.

These tests verify the Android Java integration statically. Actual speech
playback and hardware button behavior still require an Android device test.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app/src/main/java/fr/friday/assistant"
SERVICE = (SRC / "FridayService.java").read_text(encoding="utf-8")
ACTIVITY = (SRC / "MainActivity.java").read_text(encoding="utf-8")
COCKPIT = (ROOT / "app/src/main/assets/index.html").read_text(encoding="utf-8")


class ManualButtonOnlyVoiceTest(unittest.TestCase):
    def test_voice_stop_forced_off_even_if_old_preference_was_enabled(self):
        self.assertIn('settings.put("barge_in_enabled", "0")', SERVICE)
        self.assertIn("boolean bargeEnabled = false;", SERVICE)
        self.assertIn('prefs.put("barge_in_enabled", "0")', ACTIVITY)
        self.assertNotIn("bargeEnabled = new CheckBox(this)", ACTIVITY)

    def test_spoken_stops_ignored_without_cancelling_response(self):
        start = SERVICE.index("private void handlePhrase(String heard)")
        end = SERVICE.index("private void answerTurn(", start)
        handler = SERVICE[start:end]
        stop_guard = handler.index("if (BargeInPolicy.isStopListening(heard)")
        old_stop_handler = handler.index('event("status", "Arrêt complet de l\'écoute demandé.")')
        self.assertLess(stop_guard, old_stop_handler)
        self.assertIn("|| BargeInPolicy.isStopAnswer(heard)", handler)
        self.assertIn("|| BargeInPolicy.isBareStopAnswer(heard)) return;", handler)
        self.assertIn("if (currentTurn != null) {", handler)

    def test_button_interrupts_and_auto_listening_remains(self):
        self.assertIn('setAction(FridayService.ACTION_INTERRUPT)', ACTIVITY)
        self.assertIn('if (running) interruptResponse("Réponse interrompue.', SERVICE)
        self.assertIn("id=\"interruptBtn\"", COCKPIT)
        self.assertIn("id=\"bargeState\"", COCKPIT)
        self.assertIn("<small>Interruption par bouton</small>", COCKPIT)
        self.assertIn("TtsEchoGate.followupUntil(finishedAt, false)", SERVICE)

    def test_tts_segments_are_queued_instead_of_overwritten(self):
        self.assertIn("int queueMode = turn.ttsQueueStarted.get()", SERVICE)
        self.assertIn("TextToSpeech.QUEUE_ADD : TextToSpeech.QUEUE_FLUSH", SERVICE)


if __name__ == "__main__":
    unittest.main()
