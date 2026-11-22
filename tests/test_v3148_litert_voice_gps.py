"""Non-régression JARVIS 3.14.8 : LiteRT JNI, dictée Vosk et statut GPS."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"


def source(path):
    return (JAVA / path).read_text(encoding="utf-8")


class JarvisV3148RecoveryTests(unittest.TestCase):
    def test_litert_inference_recovery_is_bounded(self):
        kotlin = source("LiteRtJarvisEngine.kt")
        self.assertIn("private var activeGpu = false", kotlin)
        self.assertIn("val failedOnGpu = activeGpu", kotlin)
        self.assertIn("if (failedOnGpu)", kotlin)
        self.assertIn("backend = Backend.CPU()", kotlin)
        self.assertIn("val cpuResponse = infer(cpuEngine, prompt)", kotlin)
        self.assertIn('SecurePrefs(context).put("local_jarvis_gpu", "0")', kotlin)
        self.assertIn("private fun closeLocked()", kotlin)
        self.assertIn("private fun shortReason(failure: Throwable)", kotlin)
        self.assertIn("private fun infer(engine: Engine, prompt: String)", kotlin)
        self.assertIn("catch (cancelled: java.util.concurrent.CancellationException)", kotlin)
        self.assertNotIn("api.mistral.ai", kotlin)

    def test_voice_avoids_single_word_false_dispatch(self):
        voice = source("FridayService.java")
        self.assertIn("boolean isolatedNoise", voice)
        self.assertIn("else if (isolatedNoise)", voice)
        self.assertIn("RssArticleIntent.ambiguousVoiceReading(question)", voice)
        self.assertIn("Cette précision est traitée localement", voice)
        self.assertIn("LocalJarvisEngine.answer(", voice)
        self.assertIn("VoiceSelfEchoPolicy.resemblesRecentSpeech(", voice)

    def test_gps_state_is_not_silently_fabricated(self):
        activity = source("MainActivity.java")
        cockpit = (ROOT / "app/src/main/assets/index.html").read_text(encoding="utf-8")
        self.assertIn("Aucun relevé récent reçu d'Android après 16 secondes", activity)
        self.assertIn("Autorisation GPS Android refusée", activity)
        self.assertIn("Acquisition GPS en cours", activity)
        self.assertIn("Aucune mesure GPS effectuée", cockpit)
        self.assertNotIn("place||'Position non acquise'", cockpit)
        self.assertIn("FridayLocation.locate(getApplicationContext())", activity)

    def test_existing_permissions_and_version(self):
        manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        self.assertIn("ACCESS_FINE_LOCATION", manifest)
        self.assertIn("FOREGROUND_SERVICE_LOCATION", manifest)
        gradle = (ROOT / "app/build.gradle").read_text(encoding="utf-8")
        self.assertIn("versionCode 103", gradle)
        self.assertIn("versionName '3.14.9'", gradle)
        self.assertIn("applicationIdSuffix '.readerfixpreview'", gradle)


if __name__ == "__main__":
    unittest.main()
