"""Checks the JARVIS dialogue-memory release gate against the actual Android build metadata."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
BUILD = (ROOT / "app/build.gradle").read_text(encoding="utf-8")
FLOW = (ROOT / ".github/workflows/publish-v3.13.8.yml").read_text(encoding="utf-8")
PREVIOUS_FLOW = (ROOT / ".github/workflows/publish-v3.13.7.yml").read_text(encoding="utf-8")
MEMORY = (ROOT / "app/src/main/java/fr/friday/assistant/DialogueContext.java").read_text(encoding="utf-8")
HISTORY = (ROOT / "app/src/main/java/fr/friday/assistant/PrivateConversationHistory.java").read_text(encoding="utf-8")

class JarvisMemoryReleaseRegression(unittest.TestCase):
    def test_package_name_and_version_gate_match_real_build(self):
        version_code = re.search(r"versionCode\s+(\d+)", BUILD).group(1)
        version_name = re.search(r"versionName\s+'([^']+)'", BUILD).group(1)
        package = re.search(r"applicationId\s+'([^']+)'", BUILD).group(1)
        self.assertEqual((package, version_code, version_name),
                         ("fr.friday.assistant", "93", "3.13.8"))
        self.assertIn(
            "name='" + package + "' versionCode='" + version_code
            + "' versionName='" + version_name + "'", FLOW
        )
        self.assertIn("versionCode='92' versionName='3.13.7'", PREVIOUS_FLOW)
        self.assertNotIn("versionCode='92' versionName='3.13.7'", FLOW)

    def test_update_never_publishes_without_matching_release_certificate(self):
        self.assertIn('if gh release view "$TAG"', FLOW)
        self.assertIn('test "$PREV_CERT" = "$NEW_CERT"', FLOW)
        self.assertIn('FRIDAY-release-signed-apk', FLOW)
        self.assertIn('tests and signed APK from exactly this commit', FLOW)
        self.assertIn("DialogueContext.recent(", (ROOT /
            "app/src/main/java/fr/friday/assistant/FridayService.java").read_text(encoding="utf-8"))

    def test_dialogue_is_encrypted_locally(self):
        self.assertIn("PrivateConversationHistory.read(context, prefs)", MEMORY)
        self.assertIn("prefs.putPrivateSecret(KEY, clean.toString())", HISTORY)

if __name__ == "__main__":
    unittest.main()
