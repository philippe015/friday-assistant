"""FRIDAY 3.13.1 — microphone et haut-parleur du téléphone, sans Bluetooth."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/fr/friday/assistant"
UI=(JAVA/"MainActivity.java").read_text(encoding="utf-8")
SERVICE=(JAVA/"FridayService.java").read_text(encoding="utf-8")
ROUTE=(JAVA/"PhoneAudioRoute.java").read_text(encoding="utf-8")
MANIFEST=(ROOT/"app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
class PhoneOnlyAudio(unittest.TestCase):
    def test_manifest_has_no_bluetooth_permissions(self):
        self.assertNotIn("android.permission.BLUETOOTH",MANIFEST)
        self.assertIn("android.permission.RECORD_AUDIO",MANIFEST)
        self.assertIn("android.permission.MODIFY_AUDIO_SETTINGS",MANIFEST)
    def test_audio_source_is_builtin_mic(self):
        self.assertIn("AudioDeviceInfo.TYPE_BUILTIN_MIC",ROUTE)
        self.assertIn("recorder.setPreferredDevice(input)",ROUTE)
        self.assertIn("PhoneAudioRoute.preferBuiltInMicrophone(this, ar)",SERVICE)
        self.assertNotIn("BluetoothMicRouter",SERVICE)
    def test_all_voice_uses_internal_speaker(self):
        self.assertIn("AudioDeviceInfo.TYPE_BUILTIN_SPEAKER",ROUTE)
        self.assertIn("audio.setCommunicationDevice(device)",ROUTE)
        self.assertIn("audio.setSpeakerphoneOn(true)",ROUTE)
        self.assertIn("PhoneAudioRoute.speechAttributes()",SERVICE)
        self.assertIn("PhoneAudioRoute.speechAttributes()",UI)
    def test_no_bluetooth_ui_or_permission_flow(self):
        self.assertNotIn("bluetoothMicEnabled",UI)
        self.assertNotIn("bluetoothMicStatus",UI)
        self.assertNotIn("REQ_BLUETOOTH_MIC",UI)
        self.assertNotIn("Vérifier le microphone Bluetooth",UI)
        self.assertIn('prefs.put("bluetooth_mic_enabled", "0")',UI)
        self.assertIn("PhoneAudioRoute.release(this)",SERVICE)
if __name__=="__main__":unittest.main()
