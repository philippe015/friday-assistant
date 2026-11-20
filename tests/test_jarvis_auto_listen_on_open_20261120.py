"""JARVIS Android: immediate listen-on-app-open, never background boot."""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
ACTIVITY = (ROOT / "app/src/main/java/fr/friday/assistant/MainActivity.java").read_text(encoding="utf-8")
SERVICE = (ROOT / "app/src/main/java/fr/friday/assistant/FridayService.java").read_text(encoding="utf-8")
MANIFEST = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
POLICY = ROOT / "app/src/main/java/fr/friday/assistant/AutoListenPolicy.java"


class AutoListeningStartup(unittest.TestCase):
    def test_policy_java_behaviour(self):
        if not shutil.which("javac") or not shutil.which("java"):
            self.skipTest("Java not available")
        smoke = """
            import fr.friday.assistant.AutoListenPolicy;
            public class AutoListenSmoke {
                private static void expect(boolean actual, boolean expected) {
                    if (actual != expected) throw new AssertionError();
                }
                public static void main(String[] args) {
                    expect(AutoListenPolicy.shouldStart("1", false, false, false), true);
                    expect(AutoListenPolicy.shouldStart("", false, false, false), true);
                    expect(AutoListenPolicy.shouldStart("0", false, false, false), false);
                    expect(AutoListenPolicy.shouldStart("1", true, false, false), false);
                    expect(AutoListenPolicy.shouldStart("1", false, true, false), false);
                    expect(AutoListenPolicy.shouldStart("1", false, false, true), false);
                }
            }
        """
        with tempfile.TemporaryDirectory() as td:
            p = Path(td)
            (p / "AutoListenSmoke.java").write_text(smoke, encoding="utf-8")
            subprocess.run(["javac", "-encoding", "UTF-8", "-d", str(p),
                            str(POLICY), str(p / "AutoListenSmoke.java")],
                           check=True, capture_output=True, text=True)
            subprocess.run(["java", "-cp", str(p), "AutoListenSmoke"],
                           check=True, capture_output=True, text=True)

    def test_start_only_after_visible_on_resume(self):
        self.assertIn("startupListenAttempted = false", ACTIVITY)
        self.assertIn("manuallyStoppedListening = false", ACTIVITY)
        self.assertIn("if (!startupListenAttempted)", ACTIVITY)
        self.assertIn("startupListenAttempted = true;", ACTIVITY)
        self.assertIn("AutoListenPolicy.shouldStart(", ACTIVITY)
        self.assertIn("status.post(() -> {", ACTIVITY)
        self.assertIn("startListening(true);", ACTIVITY)
        self.assertIn("!isFinishing() && !isDestroyed()", ACTIVITY)
        self.assertNotIn("BOOT_COMPLETED", MANIFEST)

    def test_permissions_models_and_user_stop_respected(self):
        self.assertIn("ModelInstaller.installed(this)", ACTIVITY)
        self.assertIn("Manifest.permission.RECORD_AUDIO", ACTIVITY)
        self.assertIn("promptStartAfterPermission = true", ACTIVITY)
        self.assertIn("installModel(true)", ACTIVITY)
        self.assertIn("manuallyStoppedListening = true", ACTIVITY)
        self.assertIn("FridayService.ACTION_STOP", ACTIVITY)
        self.assertIn("!manuallyStoppedListening", ACTIVITY)
        self.assertIn("jarvis_auto_listen_on_open", ACTIVITY)
        self.assertIn("autoListenOnOpen.setOnCheckedChangeListener", ACTIVITY)
        self.assertIn("FOREGROUND_SERVICE_TYPE_MICROPHONE", SERVICE)
        self.assertIn("LocalJarvisEngine.ready(this)", ACTIVITY)

    def test_gps_not_blocking_voice(self):
        self.assertNotIn('status.setText("Localisation demandée : autorisez-la, puis redémarrez l\'écoute.");', ACTIVITY)
        self.assertIn("Écoute JARVIS sans GPS", ACTIVITY)


if __name__ == "__main__":
    unittest.main()
