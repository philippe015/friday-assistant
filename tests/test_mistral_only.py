"""FRIDAY 3.13.0: Mistral is retired; Jarvis only (local on-device inference)."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/"app/src/main/java/fr/friday/assistant"
class JarvisOnlyProvider(unittest.TestCase):
    def test_no_deepseek_or_new_cloud_provider(self):
        for f in ["NativeAI.java","FridayService.java"]:
            text=(SRC/f).read_text(encoding="utf-8").lower()
            self.assertNotIn("api.deepseek.com",text)
            self.assertNotIn("openai.com/v1/",text)
    def test_retired_provider_key_is_cleared(self):
        ui=(SRC/"MainActivity.java").read_text(encoding="utf-8")
        self.assertIn('prefs.setSecret("mistral", "");',ui)
        self.assertIn('prefs.put("provider", "jarvis_local")',ui)
        self.assertIn('prefs.put("mode", "jarvis_local")',ui)
        self.assertIn('prefs.put("local_jarvis_all", "1")',ui)
    def test_no_mistral_request_may_succeed_even_via_legacy_entrypoint(self):
        ai=(SRC/"NativeAI.java").read_text(encoding="utf-8")
        self.assertIn("private static boolean cloudDisabled() { return true; }",ai)
        for signature in ["public static String answer(","public static List<String> listModels(",
                          "public static String testWeb(","public static String answerStreaming(",
                          "private static JSONObject postOnce("]:
            self.assertIn(signature,ai)
        self.assertGreaterEqual(ai.count("blockCloud();"),5)
    def test_version(self):
        gradle=(ROOT/"app/build.gradle").read_text(encoding="utf-8")
        self.assertIn("versionCode 91",gradle)
        self.assertIn("versionName '3.13.6'",gradle)
if __name__=="__main__": unittest.main()
