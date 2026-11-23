"""Review the Play Console screenshot-driven package identity safety gates."""
import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]


class PlayIdentityChecks(unittest.TestCase):
    def test_release_package_is_explicitly_set_by_play_workflow(self):
        gradle = (ROOT / "app/build.gradle").read_text()
        play = (ROOT / ".github/workflows/play-console-bundle.yml").read_text()
        self.assertIn("project.findProperty('jarvisPlayPackage')", gradle)
        self.assertIn("?: 'fr.friday.assistant'", gradle)
        self.assertIn("-PjarvisPlayPackage=jarvis.myapp :app:bundleRelease", play)
        self.assertIn("check_play_bundle_package.py \"$AAB\" jarvis.myapp", play)

    def test_android_classes_keep_java_namespace(self):
        manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text()
        self.assertIn('android:name="fr.friday.assistant.MainActivity"', manifest)
        self.assertIn('android:name="fr.friday.assistant.FridayService"', manifest)

    def test_pr_checks_real_play_identity_without_signing(self):
        preview = (ROOT / ".github/workflows/pr-android-build.yml").read_text()
        self.assertIn("-PjarvisPlayPackage=jarvis.myapp :app:bundleRelease", preview)
        self.assertIn("check_play_bundle_package.py \"$AAB\" jarvis.myapp", preview)
        self.assertNotIn("KEYSTORE_PASSWORD", preview)

    def test_release_never_automatically_deploys_to_play(self):
        play = (ROOT / ".github/workflows/play-console-bundle.yml").read_text()
        self.assertIn("workflow_dispatch:", play)
        self.assertIn("publish_to_internal:", play)
        self.assertIn("default: false", play)
        self.assertIn("github.ref != 'refs/heads/main'", play)
        self.assertIn("r0adkll/upload-google-play@", play)
        self.assertIn("GOOGLE_PLAY_SERVICE_ACCOUNT_JSON", play)
        self.assertIn("track: internal", play)
        self.assertNotIn("track: production", play)
        self.assertNotIn("androidpublisher", play)
        self.assertIn("KEYSTORE_BASE64", play)
        self.assertIn("KEYSTORE_PASSWORD", play)

    def test_docs_caution_on_account_inactivity(self):
        doc = (ROOT / "docs/GOOGLE_PLAY_PUBLICATION_2026.md").read_text()
        self.assertIn("jarvis.myapp", doc)
        self.assertIn("clôturé pour inactivité", doc)
        self.assertIn("Afficher les détails", doc)


if __name__ == "__main__":
    unittest.main()
