"""Static regression gates for the optional Sherpa French Android beta."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"
MAIN = (JAVA / "MainActivity.java").read_text(encoding="utf-8")
SERVICE = (JAVA / "FridayService.java").read_text(encoding="utf-8")
SHERPA = (JAVA / "SherpaVoiceDecoder.java").read_text(encoding="utf-8")
MODELS = (JAVA / "SherpaVoiceModels.java").read_text(encoding="utf-8")
WRAPPER = (JAVA / "JarvisVoiceDecoder.java").read_text(encoding="utf-8")
GRADLE = (ROOT / "app/build.gradle").read_text(encoding="utf-8")
SETTINGS = (ROOT / "settings.gradle").read_text(encoding="utf-8")
MANIFEST = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")


class FrenchSpeechBeta(unittest.TestCase):
    def test_one_audio_record(self):
        self.assertNotIn("new AudioRecord(", SHERPA)
        self.assertIn("recognizer.acceptWaveForm(buffer, count)", SERVICE)
        self.assertIn("JarvisVoiceDecoder recognizer = null", SERVICE)
        self.assertIn("new JarvisVoiceDecoder.Vosk(model, grammar)", SERVICE)
        self.assertIn("recognitionEngine = decoder.name();", SERVICE)

    def test_hard_stop_dedicated_vosk(self):
        self.assertIn("Recognizer stopRecognizer = null", SERVICE)
        self.assertIn("stopRecognizer.setGrammar(VoiceStopSpotter.GRAMMAR)", SERVICE)
        self.assertIn("TtsEchoGate.useStopRecognizer", SERVICE)

    def test_safe_optional_engine(self):
        self.assertIn('settings.get("sherpa_voice_enabled", "0")', SERVICE)
        self.assertIn('settings.put("sherpa_voice_enabled", "0")', SERVICE)
        self.assertIn("SherpaVoiceModels.ready(this)", SERVICE)
        self.assertIn("SherpaVoiceDecoder.shutdown()", SERVICE)
        self.assertIn("if (recognizer instanceof SherpaVoiceDecoder)", SERVICE)
        self.assertIn("if (sherpaDecoderFailed) SherpaVoiceDecoder.shutdown()", SERVICE)
        self.assertNotIn("Mistral", SHERPA)

    def test_native_android_api_and_french_model(self):
        self.assertIn("new OnlineRecognizerConfig()", SHERPA)
        self.assertIn("OnlineRecognizer(ctx.getAssets(), config)", SHERPA)
        self.assertIn("new OnlineModelConfig()", SHERPA)
        self.assertIn("getResult(stream)", SHERPA)
        self.assertIn("stream.acceptWaveform(pcm, 16000)", SHERPA)
        self.assertIn("getNextTarEntry()", MODELS)
        self.assertIn("sherpa-onnx-streaming-zipformer-fr-2023-04-14.tar.bz2", MODELS)
        self.assertIn("MAX_TOTAL", MODELS)
        self.assertIn("entryName.lastIndexOf('/')", MODELS)
        self.assertIn("out.getFD().sync()", MODELS)

    def test_real_android_import_and_no_boot_micro(self):
        self.assertIn("Intent.ACTION_OPEN_DOCUMENT", MAIN)
        self.assertIn("REQ_SHERPA_FR_MODEL", MAIN)
        self.assertIn("SherpaVoiceModels.importOfficialArchive", MAIN)
        self.assertIn("FridayService.isActive()", MAIN)
        self.assertNotIn("BOOT_COMPLETED", MANIFEST)
        self.assertIn("com.github.k2-fsa.sherpa-onnx:sherpa-onnx:v2.0.2", GRADLE)
        self.assertIn("commons-compress:1.28.0", GRADLE)
        self.assertIn("https://jitpack.io", SETTINGS)
        self.assertIn("com.alphacephei:vosk-android", GRADLE)


if __name__ == "__main__":
    unittest.main()
