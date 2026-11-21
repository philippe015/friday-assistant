"""FRIDAY v3.13.4 — only local Jarvis inference, text questions spoken aloud."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/"app/src/main/java/fr/friday/assistant"
MAIN=(SRC/"MainActivity.java").read_text(encoding="utf-8")
VOICE=(SRC/"FridayService.java").read_text(encoding="utf-8")
CLOUD=(SRC/"NativeAI.java").read_text(encoding="utf-8")
HTML=(ROOT/"app/src/main/assets/index.html").read_text(encoding="utf-8")
GRADLE=(ROOT/"app/build.gradle").read_text(encoding="utf-8")

class JarvisExclusiveVoice(unittest.TestCase):
    def test_all_typed_requests_use_jarvis_without_mic(self):
        typed=MAIN.split("private void sendQuestion(String question) {",1)[1].split("private void testVoice()",1)[0]
        self.assertNotIn("FridayService.isActive()", typed)
        self.assertNotIn("NativeAI.answer(", typed)
        self.assertNotIn("JarvisMistralRouting.useLocal(", typed)
        self.assertIn("LocalJarvisEngine.answer(", typed)
        self.assertIn('String engineName = "Jarvis local";', typed)
        self.assertIn('playWebVoice(spokenResult);', typed)
        self.assertIn('prefs.get("voice_enabled", "1")', typed)

    def test_tts_reads_long_replies_in_french(self):
        self.assertIn('webVoice.setLanguage(Locale.FRENCH)', MAIN)
        self.assertIn('VoiceText.ttsChunks(spoken, 280)', MAIN)
        self.assertIn("TextToSpeech.QUEUE_ADD",MAIN)
        self.assertIn("TextToSpeech.QUEUE_FLUSH",MAIN)
        self.assertIn("PhoneAudioRoute.speechAttributes()", MAIN)

    def test_cockpit_text_does_not_require_micro(self):
        js=HTML.split("function ask(text)",1)[1].split("function start()",1)[0]
        self.assertNotIn("isListening",js)
        self.assertIn("call('ask',q)",js)
        self.assertIn("Jarvis local",js)

    def test_voice_requests_use_local_model_only(self):
        handler=VOICE.split("private void answerTurn(",1)[1].split("private String streamAndSpeak(",1)[0]
        self.assertIn("this, settings, question, privateDialogue, turn.api)",handler)
        self.assertNotIn("NativeAI.answer(",handler)
        self.assertNotIn("NativeAI.answerStreaming(",handler)
        self.assertNotIn("streamAndSpeak(prepared",handler)
        self.assertIn("boolean requestedOnDevice = true;",handler)

    def test_mistral_api_is_blocked_at_source(self):
        self.assertIn("private static boolean cloudDisabled() { return true; }",CLOUD)
        self.assertIn("private static void blockCloud() throws IOException",CLOUD)
        self.assertIn("blockCloud();",CLOUD)
        self.assertIn('prefs.setSecret("mistral", "");',MAIN)
        self.assertIn('prefs.put("web_enabled", "0")',MAIN)
        self.assertIn('prefs.put("memory_share_mistral", "0")',MAIN)
        self.assertIn('prefs.put("location_share", "0")',MAIN)
        self.assertIn('prefs.put("local_jarvis_enabled", "1")',MAIN)
        self.assertIn('prefs.put("local_jarvis_all", "1")',MAIN)

    def test_public_cockpit_does_not_offer_mistral(self):
        ui=MAIN.split("private void buildUi() {",1)[1].split("private void save() {",1)[0]
        self.assertNotIn('config.addView(mistralKey)',ui)
        self.assertNotIn('config.addView(mistralModel)',ui)
        self.assertNotIn('config.addView(webEnabled)',ui)
        self.assertNotIn('config.addView(memoryShareMistral)',ui)
        self.assertNotIn("Analyser cette page Web avec Mistral",ui)

    def test_version(self):
        self.assertIn("versionCode 100",GRADLE)
        self.assertIn("versionName '3.13.15'",GRADLE)

if __name__=="__main__":
    unittest.main()
