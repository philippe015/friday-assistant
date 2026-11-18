"""FRIDAY 3.12.1: Bluetooth HFP is discovered before SCO input is enumerated."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/fr/friday/assistant"
ROUTER=(JAVA/"BluetoothMicRouter.java").read_text(encoding="utf-8")
SERVICE=(JAVA/"FridayService.java").read_text(encoding="utf-8")
UI=(JAVA/"MainActivity.java").read_text(encoding="utf-8")

class BluetoothSCORegression(unittest.TestCase):
    def test_legacy_hfp_detected_without_sco_input(self):
        self.assertIn("BluetoothProfile.HEADSET",ROUTER)
        self.assertIn("adapter.getProfileConnectionState(BluetoothProfile.HEADSET)",ROUTER)
        self.assertIn("legacyHandsfreeConnected(context) || hasHeadsetInput()",ROUTER)
        self.assertIn("audio.startBluetoothSco()",ROUTER)

    def test_sco_connect_is_confirmed_before_capture(self):
        self.assertIn("ACTION_SCO_AUDIO_STATE_UPDATED",ROUTER)
        self.assertIn("legacyScoState = state;",ROUTER)
        self.assertIn("legacyScoState == AudioManager.SCO_AUDIO_STATE_CONNECTED",ROUTER)
        self.assertIn("awaitBluetoothRoute(long timeoutMs)",ROUTER)
        self.assertIn("SystemClock.sleep(100L)",ROUTER)
        self.assertIn("bluetoothMicRouter.awaitBluetoothRoute(6000L)",SERVICE)
        self.assertLess(SERVICE.index("bluetoothMicRouter.awaitBluetoothRoute(6000L)"),
                        SERVICE.index("ar = new AudioRecord(src"))

    def test_safe_phone_fallback_when_sco_times_out(self):
        self.assertIn("fallbackToPhone();",ROUTER)
        self.assertIn("Microphone du téléphone",ROUTER)

    def test_tts_stays_on_call_route(self):
        self.assertIn("AudioAttributes.USAGE_VOICE_COMMUNICATION",SERVICE)
        self.assertIn("AudioAttributes.USAGE_MEDIA",SERVICE)

    def test_android_diagnostics_show_hfp_before_sco(self):
        self.assertIn("BluetoothMicRouter.legacyHandsfreeConnected(this)", UI)
        self.assertIn("profil mains libres HFP/SCO",UI)
        self.assertIn("Vérifier le microphone Bluetooth",UI)

if __name__=="__main__":
    unittest.main()
