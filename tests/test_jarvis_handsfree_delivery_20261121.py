"""On-device Jarvis: a visible ASR transcript must become a real voice question."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / "app/src/main/java/fr/friday/assistant"
VOICE = (BASE / "FridayService.java").read_text()
ACTIVITY = (BASE / "MainActivity.java").read_text()
POLICY = (BASE / "HandsFreeQuestionPolicy.java").read_text()
HTML = (ROOT / "app/src/main/assets/index.html").read_text()

class HandsfreeDelivery(unittest.TestCase):
    def test_real_voice_request_not_only_preview(self):
        self.assertIn("HandsFreeQuestionPolicy.acceptsDirectQuestion(heard,", VOICE)
        self.assertIn('settings.get("followup_handsfree", "0")', VOICE)
        self.assertIn('event("status", "Question vocale acceptée', VOICE)
        self.assertIn('Thread task = new Thread(() -> answerTurn(request, turn)', VOICE)
        self.assertIn('event("user", question)', VOICE)
        self.assertIn("HandsFreeQuestionPolicy.acceptsDirectQuestion(", POLICY.replace("public static boolean acceptsDirectQuestion(", "HandsFreeQuestionPolicy.acceptsDirectQuestion("))

    def test_startup_assistant_ready_once(self):
        self.assertIn('prefs.put("followup_handsfree", "1")', ACTIVITY)
        self.assertIn('prefs.put("jarvis_auto_listen_on_open", "1")', ACTIVITY)
        self.assertIn('prefs.put("voice_assistant_ready_v31314", "1")', ACTIVITY)
        self.assertIn("AutoListenPolicy.retryOnResume(", ACTIVITY)
        self.assertIn("manuallyStoppedListening = true", ACTIVITY)

    def test_keep_text_and_voice_distinct(self):
        self.assertIn('private void sendQuestion(String question)', ACTIVITY)
        self.assertIn('Transcription non envoyée à JARVIS', HTML)
        self.assertIn("plausibleDirectQuestion(heard", VOICE)
        self.assertIn('!speakingNow && now >= echoBlockUntil', VOICE)

if __name__ == "__main__":
    unittest.main()
