"""FRIDAY 3.12.2 — Bluetooth SCO recovery and AudioRecord no-data watchdog."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"
SERVICE = (JAVA / "FridayService.java").read_text(encoding="utf-8")
ROUTER = (JAVA / "BluetoothMicRouter.java").read_text(encoding="utf-8")

class BluetoothCaptureRecovery(unittest.TestCase):
    def test_initial_sco_connection_is_not_device_change(self):
        self.assertIn("private volatile boolean captureActive;", ROUTER)
        self.assertIn("if (captureActive) routeChanged.set(true)", ROUTER)
        self.assertIn("public void captureStarted()", ROUTER)
        self.assertIn("routeChanged.set(false);\n        captureActive = true;", ROUTER)
        self.assertIn("public void captureStopped()", ROUTER)
        self.assertIn("bluetoothMicRouter.captureStarted()", SERVICE)
        self.assertIn("bluetoothMicRouter.captureStopped()", SERVICE)
        self.assertLess(SERVICE.index("ar.startRecording();"), SERVICE.index("bluetoothMicRouter.captureStarted()"))

    def test_micro_read_never_blocks_forever(self):
        self.assertIn("AudioRecord.READ_NON_BLOCKING", SERVICE)
        self.assertIn("if (count == 0)", SERVICE)
        self.assertIn("nowNoPcm - noPcmSince >= 5000L", SERVICE)
        self.assertIn("SystemClock.sleep(30L)", SERVICE)
        self.assertIn("if (count < 0)", SERVICE)
        self.assertNotIn("int count = ar.read(buffer, 0, buffer.length);", SERVICE)

    def test_communication_source_has_phone_fallback_without_barge(self):
        self.assertIn("&& (bargeEnabled || btCommunication)", SERVICE)
        self.assertIn("bluetoothMicRouter.fallbackToPhone()", SERVICE)
        self.assertIn("new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION", SERVICE)

    def test_actual_micro_route_checked_and_error_detail_visible(self):
        self.assertIn("public boolean wrongInputRouted(AudioRecord recorder)", ROUTER)
        self.assertIn("bluetoothMicRouter.wrongInputRouted(ar)", SERVICE)
        self.assertIn("Bluetooth connecté mais micro non routé par Android", SERVICE)
        self.assertIn("captureError.getMessage()", SERVICE)

    def test_jarvis_mistral_and_signed_release_compatibility(self):
        gradle = (ROOT / "app/build.gradle").read_text(encoding="utf-8")
        self.assertIn("versionCode 85", gradle)
        self.assertIn("versionName '3.13.0'", gradle)
        self.assertNotIn("OpenJarvisClient.answer", SERVICE)

if __name__ == "__main__":
    unittest.main()
