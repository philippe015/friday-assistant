"""Static integration checks for FRIDAY v3.6.0 speech streaming."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"

class StreamingIntegration(unittest.TestCase):
    def test_streaming_is_real(self):
        ai = (JAVA / "NativeAI.java").read_text()
        self.assertIn('"stream", true', ai)
        self.assertIn("text/event-stream", ai)
        self.assertIn("getInputStream()", ai)
        self.assertIn('"[DONE]"', ai)
        self.assertIn("SpeechSentenceBuffer.EventLines", ai)
        self.assertIn("onSentence.accept", ai)
        self.assertIn("WebSearch.needsWeb(question, previous)", ai)

    def test_streamed_voice_cancelled(self):
        service = (JAVA / "FridayService.java").read_text()
        self.assertIn("new ArrayBlockingQueue<>(12)", service)
        self.assertIn("new Thread(() -> {", service)
        self.assertIn("turn.speaker = audio", service)
        self.assertIn("audio.interrupt()", service)
        self.assertIn("turn.streamingVoiceSucceeded", service)
        self.assertIn("NativeAI.answerStreaming(", service)

    def test_user_setting(self):
        activity = (JAVA / "MainActivity.java").read_text()
        self.assertIn('settings.get("stream_enabled", "1")', (JAVA / "NativeAI.java").read_text())
        self.assertIn('prefs.put("stream_enabled"', activity)
        self.assertIn("streamingEnabled.setChecked", activity)

    def test_version(self):
        gradle = (ROOT / "app/build.gradle").read_text()
        import re
        self.assertGreaterEqual(int(re.search(r"versionCode (\d+)", gradle).group(1)), 58)
        model = re.search(r"versionName '(\d+)\.(\d+)\.(\d+)'", gradle)
        self.assertIsNotNone(model)
        self.assertGreaterEqual(tuple(map(int, model.groups())), (3, 6, 0))

if __name__ == "__main__":
    unittest.main()
