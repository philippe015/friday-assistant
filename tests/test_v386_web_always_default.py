"""FRIDAY v3.13.4: the former always-on Mistral search is explicitly disabled."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/"app/src/main/java/fr/friday/assistant"
UI=(SRC/"MainActivity.java").read_text(encoding="utf-8")
VOICE=(SRC/"FridayService.java").read_text(encoding="utf-8")
CLOUD=(SRC/"NativeAI.java").read_text(encoding="utf-8")
class JarvisOnlyMigration(unittest.TestCase):
    def test_old_web_migration_overridden_by_local_mode(self):
        self.assertIn('prefs.get("jarvis_exclusive_v3130", "0")',UI)
        self.assertIn('prefs.put("jarvis_exclusive_v3130", "1")',UI)
        self.assertIn('prefs.put("web_enabled", "0")',UI)
        self.assertIn('prefs.put("web_mode", "off")',UI)
        self.assertIn('prefs.put("memory_share_mistral", "0")',UI)
        self.assertIn('prefs.put("location_share", "0")',UI)
    def test_voice_requests_do_not_use_legacy_web(self):
        voice_response=VOICE.split("private void answerTurn(",1)[1].split("private String streamAndSpeak(",1)[0]
        self.assertIn("this, settings, question, privateDialogue, turn.api)",voice_response)
        self.assertNotIn("WebSearch.shouldSearch(",voice_response)
        self.assertNotIn("NativeAI.answer(",voice_response)
        self.assertIn("blockCloud();",CLOUD)
    def test_encrypted_notes_and_voice_interruptions_remain(self):
        memory=(SRC/"PersonalMemory.java").read_text(encoding="utf-8")
        self.assertIn('cfg.get("memory_share_mistral", "0")',memory)
        self.assertIn("TtsEchoGate.suppressGeneralAsr(",VOICE)
        self.assertIn("stopRecognizer.acceptWaveForm(buffer, count)",VOICE)
    def test_version(self):
        gradle=(ROOT/"app/build.gradle").read_text(encoding="utf-8")
        self.assertIn("versionCode 108",gradle)
        self.assertIn("versionName '3.14.14'",gradle)
if __name__=="__main__": unittest.main()
