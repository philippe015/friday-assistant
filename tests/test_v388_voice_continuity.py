"""FRIDAY v3.8.8: multiple voice turns after a lengthy Mistral Web answer."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"

class VoiceContinuityV388(unittest.TestCase):
    def test_local_jarvis_does_not_launch_remote_streaming(self):
        src=(JAVA/"FridayService.java").read_text(encoding="utf-8")
        section=src[src.index("String prepared = PythonBrain.prepareQuestion(question)"):
                    src.index("// Aucune coordonnée GPS n'est envoyée")]
        self.assertIn("Recherche publique HTTPS facultative",section)
        self.assertIn("this, settings, question, privateDialogue, turn.api)",section)
        self.assertIn("JarvisInternet.shouldResearch(settings, question)",section)
        self.assertNotIn("NativeAI.answer(",section)
        self.assertNotIn("streamAndSpeak(prepared",section)

    def test_stream_audio_queue_does_not_wait_for_dead_worker(self):
        src = (JAVA / "FridayService.java").read_text(encoding="utf-8")
        section = src[src.index("private String streamAndSpeak("):
                      src.index("/** Diagnostics courants lus localement", src.index("private String streamAndSpeak("))]
        self.assertIn("final Thread speechWorker = audio", section)
        self.assertIn("!speechWorker.isAlive()", section)
        self.assertIn("speechWorker.isAlive() && voiceOk.get()", section)
        self.assertIn("audio.isAlive() && voiceOk.get()", section)
        self.assertIn("voiceDeadline", section)
        self.assertIn("audio.interrupt()", section)
        self.assertIn("spokenParts.incrementAndGet()", section)
        self.assertIn("turn.streamedVoice = spokenParts.get() > 0", section)
        self.assertIn("turn.streamingVoiceSucceeded = voiceOk.get()", section)
        self.assertNotIn("while (live(turn) && !queue.offer(end", section)

    def test_tts_failure_recovers_without_stopping_microphone(self):
        src = (JAVA / "FridayService.java").read_text(encoding="utf-8")
        self.assertIn("private final AtomicBoolean ttsRecoveryInProgress", src)
        self.assertIn("private void recoverTtsAsync()", src)
        self.assertIn("recoverTtsAsync();", src)
        self.assertIn("old.stop(); old.shutdown();", src)
        self.assertIn("if (running) initTts();", src)
        self.assertIn("Le moteur TTS va être réinitialisé ; microphone toujours actif.", src)
        self.assertIn("currentTurn = null;", src)
        self.assertIn("echoBlockUntil = TtsEchoGate.afterVoice(finishedAt)", src)
        self.assertIn("stopRecognizer.acceptWaveForm(buffer, count)", src)

    def test_version_and_privacy(self):
        gradle = (ROOT / "app/build.gradle").read_text(encoding="utf-8")
        self.assertIn("versionCode 97", gradle)
        self.assertIn("versionName '3.14.3'", gradle)
        personal = (JAVA / "PersonalMemory.java").read_text(encoding="utf-8")
        self.assertIn('cfg.get("memory_share_mistral", "0")', personal)
        ai = (JAVA / "NativeAI.java").read_text(encoding="utf-8")
        self.assertIn('MISTRAL_BASE + "/conversations"', ai)

if __name__ == "__main__":
    unittest.main()
