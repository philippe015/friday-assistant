"""FRIDAY v3.13.4: previous Mistral Web settings must no longer be exposed."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/"app/src/main/java/fr/friday/assistant"
UI=(SRC/"MainActivity.java").read_text(encoding="utf-8")
CLOUD=(SRC/"NativeAI.java").read_text(encoding="utf-8")
HTML=(ROOT/"app/src/main/assets/index.html").read_text(encoding="utf-8")
class WebDisabledJarvisExclusive(unittest.TestCase):
    def test_public_ui_does_not_expose_mistral_api_settings(self):
        visible=UI.split("private void buildUi() {",1)[1].split("private void save() {",1)[0]
        for forbidden in ["config.addView(mistralModel)", "config.addView(mistralKey)",
                          "config.addView(webEnabled)", "config.addView(webAlways)",
                          "config.addView(webDiagnosticLabel)"]:
            self.assertNotIn(forbidden,visible)
        self.assertIn("MOTEUR UNIQUE",visible)
        self.assertNotIn("Question envoyée à Mistral",HTML)
    def test_old_web_api_is_not_callable(self):
        self.assertIn("private static boolean cloudDisabled() { return true; }",CLOUD)
        self.assertIn("private static void blockCloud() throws IOException",CLOUD)
        self.assertGreaterEqual(CLOUD.count("blockCloud();"),5)
        self.assertIn('prefs.put("web_enabled", "0")',UI)
        self.assertIn('prefs.put("web_mode", "off")',UI)
    def test_version(self):
        gradle=(ROOT/"app/build.gradle").read_text(encoding="utf-8")
        self.assertIn("versionCode 107",gradle)
        self.assertIn("versionName '3.14.13'",gradle)
if __name__=="__main__": unittest.main()
