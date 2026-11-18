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
        self.assertIn("LocalJarvisEngine.answer(this, prefs,", ui)
        self.assertIn("Gemma 3 léger · MediaPipe .task", ui)

    def test_local_priority_preserves_mistral_without_hidden_cloud_fallback(self):
        service = java("FridayService.java")
        self.assertIn('LocalJarvisPolicy.shouldRoute(question,', service)
        self.assertIn("boolean pcJarvis = local == null && !localJarvis", service)
        self.assertIn("LocalJarvisEngine.answer(this, settings, question, turn.api)", service)
        self.assertIn("OpenJarvisClient.answer(settings, question, turn.api)", service)
        self.assertIn("NativeAI.answer(settings, prepared, historySnapshot()", service)
        self.assertIn("!webQuestion && !pcJarvis && !localJarvis", service)
        self.assertIn("locationForApi != null && local == null\n                    && !pcJarvis && !localJarvis", service)

    def test_local_off_by_default_private_model(self):
        ui = java("MainActivity.java")
        engine = java("LocalJarvisEngine.java")
        self.assertIn('prefs.get("local_jarvis_enabled", "0")', ui)
        self.assertIn('prefs.get("local_jarvis_all", "0")', ui)
        self.assertIn('prefs.put("local_jarvis_all",', ui)
        self.assertIn('prefs.put("local_jarvis_enabled", enabled ? "1" : "0")', ui)
        self.assertIn('cfg.get("local_jarvis_enabled", "0")', engine)
        self.assertIn('Aucun modèle IA local importé', engine)
        self.assertIn('Thread', java("MainActivity.java"))
        self.assertIn('FileOutputStream', engine)
        self.assertNotIn('getExternalStorageDirectory', engine)
        self.assertNotIn("openjarvis_url", engine)

    def test_version(self):
        gradle = (ROOT / "app/build.gradle").read_text(encoding="utf-8")
        self.assertIn("versionCode 78", gradle)
        self.assertIn("versionName '3.12.0'", gradle)

if __name__ == "__main__":
    unittest.main()
