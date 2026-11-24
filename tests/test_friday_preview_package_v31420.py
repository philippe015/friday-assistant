"""Release isolation: the current DEBUG preview must not alter Play release metadata."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
GRADLE = (ROOT / "app/build.gradle").read_text(encoding="utf-8")
MANIFEST = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")


class FridayPreviewPackage(unittest.TestCase):
    def test_preview_has_separate_application_id_and_label(self):
        self.assertIn("applicationId 'fr.friday.assistant'", GRADLE)
        self.assertIn("applicationIdSuffix '.v31421matrix'", GRADLE)
        self.assertIn('android:label="FRIDAY · Terminal Matrix"', MANIFEST)
        self.assertIn("fr.friday.assistant.v31421matrix", MANIFEST)

    def test_debug_has_true_version_but_production_metadata_is_unchanged(self):
        self.assertIn("versionCode 112", GRADLE)
        self.assertIn("versionName '3.14.18'", GRADLE)
        self.assertIn('onVariants(selector().withBuildType("debug"))', GRADLE)
        self.assertIn("output.versionCode.set(116)", GRADLE)
        self.assertIn('output.versionName.set("3.14.22-auto-critique-preview")', GRADLE)

    def test_models_and_secrets_are_not_bundled_by_preview(self):
        self.assertIn("implementation 'com.alphacephei:vosk-android", GRADLE)
        self.assertNotIn("KEYSTORE_BASE64", GRADLE)
        self.assertNotIn("GOOGLE_PLAY_SERVICE_ACCOUNT_JSON", GRADLE)


if __name__ == "__main__":
    unittest.main()
