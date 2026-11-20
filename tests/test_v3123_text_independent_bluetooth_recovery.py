"""FRIDAY 3.13.1 — typed Jarvis works even without mic; speaker is phone-only."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/fr/friday/assistant"
MAIN=(JAVA/"MainActivity.java").read_text(encoding="utf-8")
SERVICE=(JAVA/"FridayService.java").read_text(encoding="utf-8")
ROUTE=(JAVA/"PhoneAudioRoute.java").read_text(encoding="utf-8")
HTML=(ROOT/"app/src/main/assets/index.html").read_text(encoding="utf-8")
class WrittenInputIndependent(unittest.TestCase):
    def test_typed_question_never_needs_microphone(self):
        typed=MAIN.split("private void sendQuestion(String question) {",1)[1].split("private void testVoice()",1)[0]
        for forbidden in ["FridayService.isActive()", "FridayService.ACTION_ASK_TEXT",
                          "Manifest.permission.RECORD_AUDIO", "startListening()", "NativeAI.answer("]:
            self.assertNotIn(forbidden,typed)
        self.assertIn("LocalJarvisEngine.answer(",typed)
        self.assertIn("new Thread(",typed)
    def test_errors_answer_and_tts_are_visible(self):
        typed=MAIN.split("private void sendQuestion(String question) {",1)[1].split("private void testVoice()",1)[0]
        self.assertIn('String engineName = "Jarvis local";',typed)
        self.assertIn('fault = "Erreur " + engineName',typed)
        self.assertIn("manualAnswerLabel.setText(result)",typed)
        self.assertIn("PrivateConversationHistory.append(",typed)
        self.assertIn("playWebVoice(spokenResult);",typed)
        self.assertIn("window.fridayTypedReply=function(question,answer)",HTML)
    def test_bluetooth_not_in_public_settings(self):
        self.assertNotIn("bluetoothMicEnabled",MAIN)
        self.assertNotIn("bluetoothMicRouter",SERVICE)
        self.assertIn("PhoneAudioRoute.preferBuiltInMicrophone(this, ar)",SERVICE)
        self.assertIn("PhoneAudioRoute.usePhoneSpeaker(this)",SERVICE)
        self.assertIn("AudioDeviceInfo.TYPE_BUILTIN_SPEAKER",ROUTE)
    def test_version(self):
        gradle=(ROOT/"app/build.gradle").read_text(encoding="utf-8")
        self.assertIn("versionCode 90",gradle)
        self.assertIn("versionName '3.13.5'",gradle)
if __name__=="__main__":unittest.main()
