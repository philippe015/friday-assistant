"""JARVIS rename invariants: brand only, never migrate app data by package rename."""
from pathlib import Path
import re
import unittest

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/fr/friday/assistant"
APP=ROOT/"app/src/main"
def read(name): return (JAVA/name).read_text(encoding="utf-8")

class JarvisOnlyBranding(unittest.TestCase):
    def test_android_identity_compatible_with_previous_release(self):
        manifest=(APP/"AndroidManifest.xml").read_text(encoding="utf-8")
        gradle=(ROOT/"app/build.gradle").read_text(encoding="utf-8")
        self.assertIn('android:label="JARVIS · IA locale"',manifest)
        self.assertIn("applicationId 'fr.friday.assistant'",gradle)
        self.assertIn("namespace 'fr.friday.assistant'",gradle)
        self.assertIn("android:icon=\"@drawable/friday_icon\"",manifest)

    def test_one_wake_word_accepted_by_both_decoders(self):
        voice=read("VoiceText.java")
        barge=read("BargeInPolicy.java")
        native=(ROOT/"native/src/lib.rs").read_text(encoding="utf-8")
        stop=read("VoiceStopSpotter.java")
        vocab=read("VoiceVocabulary.java")
        wake=read("RustWake.java")
        for spelling in ("jarvis","jarvise","jarvice","jervis"):
            for content in (voice,barge,native):
                self.assertIn(spelling, content)
        self.assertNotIn("friday|fraday",voice)
        self.assertNotIn("friday stop",stop.lower())
        self.assertNotIn('"friday"',vocab.lower())
        self.assertIn('if (!VoiceText.hasWakeWord(text)) return false;',wake)
        self.assertIn("Java_fr_friday_assistant_RustWake_hasWakeWord",native)

    def test_user_cockpit_not_friday(self):
        html=(APP/"assets/index.html").read_text(encoding="utf-8")
        self.assertIn('<div class="name">JARVIS</div>',html)
        self.assertNotIn("F.R.I.D.A.Y.",html)
        self.assertNotIn("« Friday… »",html)
        self.assertIn("window.fridayNativeReady",html) # JS ABI unchanged
        self.assertIn("window.FridayAndroid",html) # existing bridge unchanged

    def test_persona_and_llm_are_local_and_preserve_key(self):
        persona=read("PersonalityProfiles.java")
        brain=read("LocalJarvisEngine.java")
        self.assertIn('KEY = "personality_profiles_202"', persona)
        self.assertIn('root.put("active", "friday")', persona) # stable profile id
        self.assertIn('"JARVIS"',persona)
        self.assertIn("migratedDefault(validated(stored))",persona)
        self.assertIn('Tu es JARVIS',brain)
        self.assertIn('\\nJARVIS :',brain)
        self.assertNotIn('Tu es FRIDAY',brain)

    def test_followups_and_web_not_removed(self):
        service=read("FridayService.java")
        self.assertIn("TtsEchoGate.followupUntil(finishedAt, false)",service)
        self.assertIn('JarvisInternet.shouldResearch(settings, question)',service)
        self.assertIn('JarvisInternet.liveConnectionDiagnostic(settings, turn.api)',service)
        self.assertIn('VoiceStopSpotter.GRAMMAR',service)
        self.assertIn('JARVIS',service)

if __name__ == "__main__":
    unittest.main()
