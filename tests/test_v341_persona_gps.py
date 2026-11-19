"""Contrôles de câblage FRIDAY v3.4.1 — identité et localisation partagée."""
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"

def source(name):
    return (JAVA / name).read_text(encoding="utf-8")

class PersonaGpsRegression(unittest.TestCase):
    def test_identity_comes_from_active_profile(self):
        ai = source("NativeAI.java")
        self.assertIn("AssistantIdentity.reply(question", ai)
        self.assertIn("PersonalityProfiles.activeName(settings)", ai)
        self.assertIn("PersonalityProfiles.activePrompt(settings)", ai)
        self.assertIn("AssistantIdentity.displayName(profile, prompt)", ai)
        self.assertIn("Mistral est le fournisseur technique", ai)
        self.assertIn('"instructions", system(cfg, question)', ai)
        self.assertIn('"content", system(cfg, question)', ai)

    def test_location_never_forwarded_to_remote_ai(self):
        service=source("FridayService.java")
        block=service.split("private void answerTurn(",1)[1].split("private String streamAndSpeak(",1)[0]
        self.assertIn("boolean sharingAllowed = false;",block)
        self.assertNotIn("NativeAI.answer(",block)
        self.assertNotIn('settings.put("gps_last_shared"',block)
        self.assertNotIn('event("gps_api"',block)

    def test_sharing_not_enabled_implicitly(self):
        activity=source("MainActivity.java")
        self.assertIn('prefs.put("location_share", "0")',activity)
        self.assertIn("locationShare.setChecked(false)",activity)
        self.assertNotIn("config.addView(locationShare)",activity)
        self.assertIn("FridayService.locationForegroundEnabled()",activity)

    def test_identity_and_local_gps_exposed_in_hud(self):
        html=(ROOT/"app/src/main/assets/index.html").read_text(encoding="utf-8")
        self.assertIn("gpsShareState",html)
        self.assertIn("gpsConsentBtn",html)
        self.assertIn("Aucun partage",html)
        self.assertIn("personaState",html)
        self.assertIn("Mon identité",html)

    def test_version(self):
        gradle = (ROOT / "app/build.gradle").read_text(encoding="utf-8")
        import re
        self.assertGreaterEqual(int(re.search(r"versionCode (\d+)", gradle).group(1)), 56)
        version = re.search(r"versionName '(\d+)\.(\d+)\.(\d+)'", gradle)
        self.assertIsNotNone(version)
        self.assertGreaterEqual(tuple(map(int, version.groups())), (3, 4, 1))

if __name__ == "__main__":
    unittest.main()
