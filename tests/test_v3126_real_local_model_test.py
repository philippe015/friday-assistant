"""FRIDAY 3.12.6: the main 'tester réponse locale' must exercise a real on-device LLM."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
JAV=(ROOT/"app/src/main/java/fr/friday/assistant")
UI=(JAV/"MainActivity.java").read_text(encoding="utf-8")
ENGINE=(JAV/"LocalJarvisEngine.java").read_text(encoding="utf-8")
KOTLIN=(JAV/"LiteRtJarvisEngine.kt").read_text(encoding="utf-8")

class RealLocalTest(unittest.TestCase):
    def test_main_button_does_not_fall_back_to_clock_or_mistral(self):
        self.assertIn('button("✓ Tester IA Jarvis locale", this::testLocalAnswer)',UI)
        handler=UI.split("private void testLocalAnswer() {",1)[1].split("private void sendQuestion(",1)[0]
        self.assertIn("testLocalJarvis();",handler)
        self.assertNotIn("sendQuestion(",handler)
        self.assertNotIn("Quelle heure est-il",handler)

    def test_test_requires_actual_model_and_inference(self):
        body=UI.split("private void testLocalJarvis() {",1)[1].split("private void enableAndTestWeb()",1)[0]
        self.assertIn("LocalJarvisEngine.ready(this)",body)
        self.assertIn("LocalJarvisEngine.answer(getApplicationContext(), prefs,",body)
        self.assertIn("TEST IA LOCALE RÉUSSI",body)
        self.assertIn("TEST IA LOCALE ÉCHOUÉ",body)
        self.assertIn("prefs.put(\"last_error\", result)",body)
        self.assertNotIn("NativeAI.answer(",body)
        self.assertIn("new Thread(",body)

    def test_local_inference_is_real_and_does_not_use_cloud(self):
        self.assertIn("cached.generateResponse(prompt)",ENGINE)
        self.assertIn('if ("litertlm".equals(format(context)))',ENGINE)
        self.assertIn("LiteRtJarvisEngine.answer(context, modelFile(context),",ENGINE)
        self.assertIn("conversation.sendMessage(prompt)",KOTLIN)
        self.assertNotIn("HttpURLConnection",KOTLIN)

    def test_litert_reports_native_failure_and_gpu_only_warning(self):
        self.assertIn("lastFailure?.let",KOTLIN)
        self.assertIn("Un modèle compilé uniquement pour GPU",KOTLIN)
        self.assertIn("Cause native :",KOTLIN)

    def test_version(self):
        gradle=(ROOT/"app/build.gradle").read_text(encoding="utf-8")
        self.assertIn("versionCode 94",gradle)
        self.assertIn("versionName '3.13.9'",gradle)

if __name__=="__main__": unittest.main()
