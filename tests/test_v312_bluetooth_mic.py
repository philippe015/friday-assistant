"""FRIDAY v3.12.0: Bluetooth microphone, permissions and safe fallback."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app/src/main/java/fr/friday/assistant"
def code(name):
    return (SRC / name).read_text(encoding="utf-8")

class BluetoothMicV312(unittest.TestCase):
    def test_manifest_requires_only_audio_bluetooth_permissions(self):
        manifest=(ROOT/"app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        self.assertIn("android.permission.BLUETOOTH_CONNECT",manifest)
        self.assertIn('android.permission.MODIFY_AUDIO_SETTINGS',manifest)
        self.assertIn('android.permission.BLUETOOTH" android:maxSdkVersion="30"',manifest)
        self.assertNotIn('android.permission.BLUETOOTH_SCAN',manifest)

    def test_actual_route_not_unverified_bluetooth_toggle(self):
        router=code("BluetoothMicRouter.java")
        self.assertIn("getAvailableCommunicationDevices()", router)
        self.assertIn("setCommunicationDevice(headset)",router)
        self.assertIn("startBluetoothSco()",router)
        self.assertIn("ACTION_SCO_AUDIO_STATE_UPDATED", router)
        self.assertIn("recorder.setPreferredDevice(info)",router)
        self.assertIn("recorder.getRoutedDevice()",router)
        self.assertIn("TYPE_BLUETOOTH_SCO",router)
        self.assertIn("TYPE_BLE_HEADSET",router)
        self.assertNotIn("TYPE_BLUETOOTH_A2DP",router)
        self.assertIn("clearCommunicationDevice()", router)
        self.assertIn("stopBluetoothSco()",router)
        self.assertIn("unregisterAudioDeviceCallback",router)

    def test_mic_service_survives_unpaired_or_disconnected_headset(self):
        service=code("FridayService.java")
        self.assertIn("bluetoothMicRouter.start(",service)
        self.assertIn("bluetoothMicRouter.preferInput(ar)",service)
        self.assertIn("bluetoothMicRouter.routedStatus(ar)",service)
        self.assertIn("bluetoothMicRouter.consumeRouteChange()",service)
        self.assertIn("bluetoothMicRouter.close()",service)
        self.assertIn('settings.get("bluetooth_mic_enabled", "1")',service)
        self.assertIn("Capture vocale interrompue",service)

    def test_explicit_permission_ui_and_diagnostic(self):
        ui=code("MainActivity.java")
        self.assertIn("Manifest.permission.BLUETOOTH_CONNECT",ui)
        self.assertIn("REQ_BLUETOOTH_MIC",ui)
        self.assertIn('prefs.put("bluetooth_mic_enabled"',ui)
        self.assertIn("Vérifier le microphone Bluetooth",ui)
        self.assertIn("FridayService.bluetoothMicStatus()",ui)
        self.assertIn("BluetoothMicRouter.allowed(this)",ui)
        self.assertIn('prefs.put("openjarvis_enabled", "0")',ui)
        self.assertNotIn("OpenJarvisClient.answer",code("FridayService.java"))

if __name__ == "__main__":
    unittest.main()
