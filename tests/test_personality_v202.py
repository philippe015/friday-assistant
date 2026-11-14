"""Profils chiffrés et personnalisation d'un assistant exclusivement Mistral."""
import unittest
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT/'app/src/main'
class PersonalityIntegration(unittest.TestCase):
    def test_profile_storage_is_encrypted(self):
        text=(SRC/'java/fr/friday/assistant/PersonalityProfiles.java').read_text()
        for keyword in ('prefs.setSecret(KEY','prefs.secret(KEY)','MAX_PROMPT = 16000','MAX_PROFILES = 10'):
            self.assertIn(keyword,text)
    def test_mistral_receives_custom_prompt(self):
        text=(SRC/'java/fr/friday/assistant/NativeAI.java').read_text()
        for keyword in ('PersonalityProfiles.activePrompt(cfg)','body.put("instructions", system(cfg) +','"content", system(cfg)','https://api.mistral.ai/v1/chat/completions'):
            self.assertIn(keyword,text)
    def test_bridge_and_ui(self):
        a=(SRC/'java/fr/friday/assistant/MainActivity.java').read_text()
        html=(SRC/'assets/index.html').read_text()
        for key in ('getPersonalityProfiles()', 'savePersonalityProfiles(String json)', 'sharePersonalityProfiles(String json)', 'void ask(String question)'):
            self.assertIn(key,a)
        for key in ('id="personaPrompt"','id="personaSave"','id="personaTemplate"','id="tabPersona"','id="personaProfiles"','window.fridayOpenPersonality'):
            self.assertIn(key,html)
if __name__=='__main__':unittest.main()
