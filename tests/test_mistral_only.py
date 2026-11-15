"""Test statique de migration : aucun appel DeepSeek dans le code exécuté."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"

class MistralOnlyTest(unittest.TestCase):
    def test_no_deepseek_endpoint_or_branding(self):
        runtime = [
            JAVA / "NativeAI.java",
            JAVA / "FridayService.java",
            ROOT / "app/src/main/assets/index.html",
        ]
        for filename in runtime:
            with self.subTest(filename=filename.name):
                text = filename.read_text(encoding="utf-8").lower()
                self.assertNotIn("deepseek", text)
                self.assertNotIn("platform.deepseek", text)

    def test_legacy_key_removed_without_erasing_mistral_key(self):
        code = (JAVA / "MainActivity.java").read_text(encoding="utf-8")
        self.assertIn('prefs.setSecret("openai", "");', code)
        self.assertNotIn('prefs.setSecret("mistral", "");', code)
        self.assertIn('prefs.put("mistral_only_v330", "1")', code)
        self.assertIn('prefs.secret("mistral")', code)
        self.assertIn('https://api.mistral.ai/v1', code)

    def test_api_provider_and_web_search(self):
        code = (JAVA / "NativeAI.java").read_text(encoding="utf-8")
        self.assertIn("https://api.mistral.ai/v1", code)
        self.assertIn('"/chat/completions"', code)
        self.assertIn('"/conversations"', code)
        self.assertIn('"web_search"', code)
        self.assertIn('"tool_reference"', code)
        self.assertIn('settings.secret("mistral")', code)
        self.assertNotIn('secret("openai")', code)
        self.assertNotIn('api.deepseek.com', code)

    def test_version(self):
        version = (ROOT / "app/build.gradle").read_text(encoding="utf-8")
        import re
        code = re.search(r"versionCode (\d+)", version)
        name = re.search(r"versionName '(\d+)\.(\d+)\.(\d+)'", version)
        self.assertIsNotNone(code)
        self.assertIsNotNone(name)
        self.assertGreaterEqual(int(code.group(1)), 54)
        self.assertGreaterEqual(tuple(int(x) for x in name.groups()), (3, 3, 0))

if __name__ == "__main__":
    unittest.main()
