"""FRIDAY v3.10.0: genuine on-device model, no desktop dependency."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"
def java(name):
    return (JAVA / name).read_text(encoding="utf-8")

class JarvisOnDeviceV310(unittest.TestCase):
    def test_real_android_inference_engine(self):
        src = java("LocalJarvisEngine.java")
        gradle = (ROOT / "app/build.gradle").read_text(encoding="utf-8")
        self.assertIn("com.google.mediapipe:tasks-genai:0.10.35", gradle)
        self.assertIn('LlmInference.createFromOptions(context,', src)
        self.assertIn('.setModelPath(model.getAbsolutePath())', src)
        self.assertIn('.generateResponse(prompt)', src)
        self.assertIn("LocalJarvisArithmetic.tryAnswer(cleaned)", src)
        self.assertIn('PersonalMemory.search(cfg, cleaned, 3)', src)
        self.assertNotIn("HttpURLConnection", src)
        self.assertNotIn("new URL(", src)
        self.assertNotIn('settings.secret("mistral")', src)

    def test_model_import_stored_privately_without_desktop(self):
        src = java("LocalJarvisEngine.java")
        ui = java("MainActivity.java")
        self.assertIn('new File(context.getFilesDir(), "local-jarvis")', src)
        self.assertIn('context.getContentResolver().openInputStream(uri)', src)
        self.assertIn('StandardCopyOption.REPLACE_EXISTING', src)
        self.assertIn("MIN_MODEL_BYTES", src)
        self.assertIn("MAX_MODEL_BYTES", src)
        self.assertIn('Intent.ACTION_OPEN_DOCUMENT', ui)
        self.assertIn("REQ_LOCAL_LLM_MODEL", ui)
        self.assertIn("LocalJarvisEngine.importModel(this, selected)", ui)
        self.assertIn("LocalJarvisEngine.answer(getApplicationContext(), prefs,", ui)
        self.assertIn("Gemma 3 léger · MediaPipe .task", ui)

    def test_local_jarvis_is_the_only_ai_engine(self):
        service=java("FridayService.java")
        ui=java("MainActivity.java")
        cloud=java("NativeAI.java")
        self.assertIn("boolean requestedOnDevice = true;",service)
        self.assertIn("this, settings, question, privateDialogue, turn.api)",service)
        self.assertNotIn("NativeAI.answer(settings, prepared, historySnapshot()",service)
        self.assertNotIn("streamAndSpeak(prepared",service)
        self.assertIn('prefs.put("local_jarvis_all", "1")',ui)
        self.assertIn('prefs.put("local_jarvis_enabled", "1")',ui)
        self.assertIn('prefs.setSecret("mistral", "")',ui)
        self.assertIn("blockCloud();",cloud)

    def test_local_model_is_private_and_missing_model_is_explained(self):
        ui=java("MainActivity.java")
        engine=java("LocalJarvisEngine.java")
        self.assertIn('cfg.get("local_jarvis_enabled", "0")',engine)
        self.assertIn('Aucun modèle IA local importé',engine)
        self.assertIn('private void saveLocalJarvisPrefs()',ui)
        self.assertIn('FileOutputStream',engine)
        self.assertNotIn('getExternalStorageDirectory',engine)
        self.assertNotIn('openjarvis_url',engine)

    def test_version(self):
        gradle = (ROOT / "app/build.gradle").read_text(encoding="utf-8")
        self.assertIn("versionCode 102", gradle)
        self.assertIn("versionName '3.14.8'", gradle)

if __name__ == "__main__":
    unittest.main()
