"""FRIDAY 3.11.2: continuous listening, TTS echo safety, Android microphone recovery."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"
SERVICE = (JAVA / "FridayService.java").read_text(encoding="utf-8")
UI = (JAVA / "MainActivity.java").read_text(encoding="utf-8")

class MicrophoneRecovery(unittest.TestCase):
    def test_continuous_migration_only_once(self):
        self.assertIn('settings.get("voice_safety_3112", "0")', SERVICE)
        self.assertIn('settings.put("voice_safety_3112", "1")', SERVICE)
        self.assertIn('settings.put("listen_once", "0")', SERVICE)
        self.assertIn('settings.put("barge_in_enabled", "0")', SERVICE)
        self.assertIn('settings.put("stream_enabled", "0")', SERVICE)

    def test_not_stuck_muted_after_two_retries(self):
        self.assertIn("if (mutedAt - mutedSince >= 8000L)", SERVICE)
        self.assertNotIn("mutedMicroReopens < 2)", SERVICE)
        self.assertIn('throw new IllegalStateException("Microphone rendu muet par Android")', SERVICE)
        self.assertIn("consecutiveErrors = 0;", SERVICE)
        self.assertIn("if (mutedSince != 0 || micSilenced)", SERVICE)
        self.assertIn("bluetoothMicRouter.routedStatus(ar)", SERVICE)

    def test_capture_errors_no_longer_kill_service(self):
        self.assertNotIn("if (consecutiveErrors >= 3) throw captureError", SERVICE)
        self.assertIn("Math.min(10000L, 500L * Math.min(consecutiveErrors, 20))", SERVICE)
        self.assertIn("Thread.sleep(retryMs)", SERVICE)
        self.assertIn("if (!running) break;", SERVICE)
        self.assertIn("finally { shutdown(); stopSelf(); }", SERVICE)

    def test_tts_cannot_shut_down_micro_from_false_stop(self):
        start=SERVICE.index("VoiceStopSpotter.Action action =")
        end=SERVICE.index("} else if (stopRecognizer != null)", start)
        block=SERVICE[start:end]
        self.assertNotIn("shutdown();", block)
        self.assertIn("VoiceStopSpotter.Action.STOP_MIC", block)
        self.assertIn("interruptResponse(", block)
        self.assertIn('settings.get("barge_in_enabled", "0")', SERVICE)
        self.assertIn('prefs.get("barge_in_enabled", "0")', UI)

    def test_version(self):
        gradle=(ROOT/"app/build.gradle").read_text(encoding="utf-8")
        self.assertIn("versionCode 85", gradle)
        self.assertIn("versionName '3.13.0'", gradle)

if __name__ == "__main__":
    unittest.main()
