"""FRIDAY v3.11.0: real dual engine MediaPipe .task and LiteRT-LM .litertlm."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"
def read(name):
    return (JAVA/name).read_text(encoding="utf-8")

class LiteRTDualEngineV311(unittest.TestCase):
    def test_real_litert_engine_and_kotlin_dependency(self):
        build = (ROOT/"build.gradle").read_text(encoding="utf-8")
        app = (ROOT/"app/build.gradle").read_text(encoding="utf-8")
        engine = read("LiteRtJarvisEngine.kt")
        self.assertIn("org.jetbrains.kotlin.android", build)
        self.assertIn("org.jetbrains.kotlin.android", app)
        self.assertIn("com.google.ai.edge.litertlm:litertlm-android:0.18.0", app)
        self.assertIn("com.google.mediapipe:tasks-genai:0.10.35", app)
        self.assertIn("EngineConfig(", engine)
        self.assertIn("Engine(config)", engine)
        self.assertIn("candidate.initialize()", engine)
        self.assertIn("engine.createConversation().use", engine)
        self.assertIn("conversation.sendMessage(prompt)", engine)
        self.assertIn("Backend.GPU()", engine)
        self.assertIn("Backend.CPU()", engine)
        self.assertIn("NativeAI.CancelToken?", engine)

    def test_import_safely_detects_each_file_format(self):
        engine = read("LocalJarvisEngine.java")
        self.assertIn('filename.endsWith(".litertlm")', engine)
        self.assertIn('filename.endsWith(".task")', engine)
        self.assertIn('filename.endsWith(".bin")', engine)
        self.assertIn('new File(dir, "litertlm".equals(modelFormat)', engine)
        self.assertIn('"model.litertlm" : "model.task"', engine)
        self.assertIn('preferences.put("local_jarvis_format", importFormat)', engine)
        self.assertIn('preferences.put("local_jarvis_gpu", gpu ? "1" : "0")', engine)
        self.assertIn('LiteRtJarvisEngine.releaseAndWait()', engine)
        self.assertIn("StandardCopyOption.REPLACE_EXISTING", engine)
        self.assertIn("available < declaredSize + 400_000_000L", engine)
        self.assertIn("4_500_000_000L", engine)
        self.assertIn("if (tmp.exists()) tmp.delete()", engine)

    def test_local_only_routing_and_memory(self):
        engine = read("LocalJarvisEngine.java")
        self.assertIn('if ("litertlm".equals(format(context)))', engine)
        self.assertIn("LiteRtJarvisEngine.answer(context, modelFile(context),", engine)
        self.assertIn("buildLocalPrompt(cfg, cleaned, privateDialogue)", engine)
        self.assertIn("PersonalMemory.search(cfg, cleaned, 3)", engine)
        self.assertIn("LocalJarvisArithmetic.tryAnswer(cleaned)", engine)
        self.assertIn("cached.generateResponse(prompt)", engine)
        service = read("FridayService.java")
        self.assertIn("this, settings, question, privateDialogue, turn.api)", service)
        self.assertNotIn("HttpURLConnection", read("LiteRtJarvisEngine.kt"))

    def test_two_exact_model_links_no_repository_ambiguity(self):
        ui = read("MainActivity.java")
        self.assertIn("gemma3-270m-it-q8.task", ui)
        self.assertIn("gemma-4-E2B-it-gpu.litertlm", ui)
        self.assertIn("openLocalJarvisGemma4Models", ui)
        self.assertIn("Importer un modèle IA local (.task / .litertlm)", ui)
        self.assertIn("GGUF", ui)
        manifest = (ROOT/"app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        self.assertIn('android:name="libOpenCL.so"', manifest)
        self.assertIn('android:name="libvndksupport.so"', manifest)
        self.assertIn('android:required="false"', manifest)

    def test_version(self):
        app=(ROOT/"app/build.gradle").read_text(encoding="utf-8")
        self.assertIn("versionCode 94", app)
        self.assertIn("versionName '3.14.0'", app)

if __name__ == "__main__":
    unittest.main()
