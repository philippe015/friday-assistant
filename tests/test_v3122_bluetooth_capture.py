"""FRIDAY 3.13.1 — recover AudioRecord without Bluetooth route changes."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/fr/friday/assistant"
S=(JAVA/"FridayService.java").read_text(encoding="utf-8")
R=(JAVA/"PhoneAudioRoute.java").read_text(encoding="utf-8")
class LocalCapture(unittest.TestCase):
    def test_no_bluetooth_route_restarts(self):
        self.assertNotIn("consumeRouteChange()",S)
        self.assertNotIn("wrongInputRouted(ar)",S)
        self.assertNotIn("awaitBluetoothRoute",S)
    def test_capture_read_has_no_data_watchdog(self):
        self.assertIn("AudioRecord.READ_NON_BLOCKING",S)
        self.assertIn("if (count == 0)",S)
        self.assertIn("nowNoPcm - noPcmSince >= 5000L",S)
        self.assertIn("SystemClock.sleep(30L)",S)
        self.assertIn("if (count < 0)",S)
    def test_phone_input_selection_before_start(self):
        self.assertLess(S.index("PhoneAudioRoute.preferBuiltInMicrophone(this, ar)"),S.index("ar.startRecording();"))
        self.assertIn("PhoneAudioRoute.inputStatus(ar)",S)
    def test_tts_uses_phone_speaker(self):
        self.assertIn("PhoneAudioRoute.usePhoneSpeaker(this)",S)
        self.assertIn("PhoneAudioRoute.speechAttributes()",S)
        self.assertIn("AudioAttributes.USAGE_VOICE_COMMUNICATION",R)
    def test_error_diagnostics_and_version(self):
        self.assertIn("captureError.getMessage()",S)
        gradle=(ROOT/"app/build.gradle").read_text(encoding="utf-8")
        self.assertIn("versionCode 96",gradle)
        self.assertIn("versionName '3.13.11'",gradle)
if __name__=="__main__":unittest.main()
