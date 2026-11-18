"""FRIDAY v3.8.2: URL parlée, refus de site et analyse écrite hors micro."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"

def read(name):
    return (JAVA / name).read_text(encoding="utf-8")

class WebUrlV382(unittest.TestCase):
    def test_malformed_https_is_fixed_then_checked(self):
        page = read("WebPageReader.java")
        search = read("WebSearch.java")
        self.assertIn("normalizeUrlInput(String value)", page)
        self.assertIn('return "https://" + url', page)
        self.assertIn("new URI(normalizeUrlInput(url))", page)
        self.assertIn("WebPageReader.normalizeUrlInput(candidate)", search)
        self.assertIn("checkedUri(String url)", page)
        self.assertIn("checkPublicDns(target)", page)

    def test_real_http_denials_not_ignored(self):
        page = read("WebPageReader.java")
        self.assertIn("explainHttpStatus(int status)", page)
        self.assertIn("Accès refusé par le site", page)
        self.assertIn("if (code != 200)", page)
        self.assertIn("throw new IllegalArgumentException(explainHttpStatus(code))", page)
        self.assertIn("La page répond HTTP", page)

    def test_manual_page_analysis_without_microphone(self):
        ui = read("MainActivity.java")
        begin = ui.index("private void analyzeWebPage()")
        end = ui.index("Une liste de modèles", begin)
        fn = ui[begin:end]
        self.assertIn("WebPageReader.checkedUri(", fn)
        self.assertIn("NativeAI.answer(prefs", fn)
        self.assertIn("java.util.Collections.emptyList()", fn)
        self.assertIn("new Thread(", fn)
        self.assertIn("setMessage(answer)", fn)
        self.assertNotIn("FridayService.isActive()", fn)
        self.assertNotIn("sendQuestion(", fn)

    def test_privacy_and_version(self):
        memory = read("PersonalMemory.java")
        self.assertIn('cfg.get("memory_share_mistral", "0")', memory)
        self.assertIn("cfg.putPrivateSecret(STORE, json)", memory)
        manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text()
        self.assertIn('android:allowBackup="false"', manifest)
        gradle = (ROOT / "app/build.gradle").read_text()
        self.assertRegex(gradle, r"versionCode (?:65|66|67|68|69|70|71|72|73)")
        self.assertRegex(gradle, r"versionName '(?:3\.8\.[23456789]|3\.9\.0)'")

if __name__ == "__main__":
    unittest.main()
