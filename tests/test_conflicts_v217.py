from pathlib import Path
from xml.etree import ElementTree as ET
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'app/src/main/java/fr/friday/assistant'
ASSET = ROOT / 'app/src/main/assets/index.html'

class ConflictIntegrationTest(unittest.TestCase):
    def test_explicit_package_visibility_only(self):
        manifest = ET.parse(ROOT / 'app/src/main/AndroidManifest.xml').getroot()
        ns = '{http://schemas.android.com/apk/res/android}'
        names = {x.attrib[ns+'name'] for x in manifest.findall('./queries/package')}
        self.assertEqual(names, {
            'fr.friday.assistant', 'fr.friday.assistant.preview',
            'fr.friday.assistant.smartpreview'})
        self.assertNotIn('QUERY_ALL_PACKAGES', (ROOT / 'app/src/main/AndroidManifest.xml').read_text())

    def test_warn_before_launch_without_auto_stopping_other_app(self):
        main = (JAVA / 'MainActivity.java').read_text()
        self.assertIn('FridayVersionGuard.installed(this)', main)
        self.assertIn('Démarrer quand même', main)
        self.assertIn('Gérer les versions', main)
        self.assertIn('startServiceUnchecked', main)
        guard = (JAVA / 'FridayVersionGuard.java').read_text()
        self.assertIn('ACTION_APPLICATION_DETAILS_SETTINGS', guard)
        self.assertNotIn('forceStopPackage', guard)

    def test_audio_policy_is_checked_without_fake_app_identification(self):
        service = (JAVA / 'FridayService.java').read_text()
        self.assertIn('registerAudioRecordingCallback', service)
        self.assertIn('getActiveRecordingConfiguration()', service)
        self.assertIn('isClientSilenced()', service)
        self.assertIn('if (silencedByPolicy.get())', service)
        self.assertIn('event("mic_conflict"', service)
        self.assertIn('completed = interim = "";', service)

    def test_hud_exposes_identity_and_conflict_state(self):
        html = ASSET.read_text()
        for component in ('versionState', 'versionsState', 'versionsBtn',
                          'MICRO BLOQUÉ', 'otherVersionsCount', 'currentVersion'):
            self.assertIn(component, html)
        self.assertRegex(html, r'[2-9]\.\d+\.\d+')

    def test_build_version_and_dialog_compilation_fix(self):
        build = (ROOT / 'app/build.gradle').read_text()
        # Version >= 2.2.0 (le correctif 2.2.1+ ne doit pas casser ce test)
        m = re.search(r"versionName '(\d+)\.(\d+)\.(\d+)'", build)
        self.assertIsNotNone(m, 'versionName introuvable dans app/build.gradle')
        version = tuple(int(x) for x in m.groups())
        self.assertGreaterEqual(version, (2, 2, 0))
        self.assertIn("applicationIdSuffix '.readerfixpreview'", build)
        main = (JAVA / 'MainActivity.java').read_text()
        self.assertIn('import android.app.AlertDialog;', main)

if __name__ == '__main__':
    unittest.main()
