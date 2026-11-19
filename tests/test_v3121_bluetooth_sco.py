"""FRIDAY 3.13.1 — anciennes connexions SCO/HFP totalement retirées."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/fr/friday/assistant"
ROUTE=(JAVA/"PhoneAudioRoute.java").read_text(encoding="utf-8")
SERVICE=(JAVA/"FridayService.java").read_text(encoding="utf-8")
UI=(JAVA/"MainActivity.java").read_text(encoding="utf-8")
class NoMoreBluetoothSCO(unittest.TestCase):
    def test_old_bluetooth_router_removed(self):
        self.assertFalse((JAVA/"BluetoothMicRouter.java").exists())
        self.assertNotIn("bluetoothMicRouter",SERVICE)
        self.assertNotIn("startBluetoothSco",ROUTE)
        self.assertNotIn("BluetoothProfile",ROUTE)
        self.assertNotIn("BluetoothMicRouter",UI)
    def test_android_routes_builtin_speaker(self):
        self.assertIn("if (Build.VERSION.SDK_INT >= 31)",ROUTE)
        self.assertIn("audio.setCommunicationDevice(device)",ROUTE)
        self.assertIn("audio.setSpeakerphoneOn(true)",ROUTE)
        self.assertIn("AudioAttributes.USAGE_VOICE_COMMUNICATION",ROUTE)
    def test_real_input_device_reported(self):
        self.assertIn("recorder.getRoutedDevice()",ROUTE)
        self.assertIn("Microphone intégré du téléphone actif",ROUTE)
        self.assertIn("PhoneAudioRoute.inputStatus(ar)",SERVICE)
if __name__=="__main__":unittest.main()
