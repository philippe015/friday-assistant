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
        self.assertIn('"instructions", system(cfg)', ai)
        self.assertIn('"content", system(cfg)', ai)

    def test_location_forwarded_on_direct_requests_with_consent(self):
        service = source("FridayService.java")
        fragment = service.split('boolean sharingAllowed = "1".equals(settings.get("location_share", "0"));', 1)[1]
        self.assertLess(fragment.index("if (sharingAllowed)"), fragment.index("LocationIntent.direct(question)"))
        shared = fragment.split("} else if (LocationIntent.direct(question))", 1)[0]
        self.assertIn("locationForApi = fix.forAi()", shared)
        self.assertIn("NativeAI.answer(settings, prepared", service)
        self.assertIn("historySnapshot(), locationForApi, turn.api", service)
        self.assertIn('settings.put("gps_last_shared"', service)
        self.assertIn('event("gps_api"', service)

    def test_sharing_not_enabled_implicitly(self):
        activity = source("MainActivity.java")
        self.assertIn('"location_share", "0"', activity)
        self.assertIn("locationShare.setChecked(true)", activity)
        self.assertIn("gpsShareTelemetry()", activity)
        self.assertIn("gpsConsent()", activity)
        self.assertIn("FridayService.locationForegroundEnabled()", activity)
        self.assertIn("configurer", activity.lower())
        self.assertIn("prefs.put(\"location_share\", \"0\")", activity)

    def test_identity_and_consent_exposed_in_hud(self):
        html = (ROOT / "app/src/main/assets/index.html").read_text(encoding="utf-8")
        self.assertIn("gpsShareState", html)
        self.assertIn("gpsConsentBtn", html)
        self.assertIn("call('gpsShareTelemetry')", html)
        self.assertIn("call('gpsConsent')", html)
        self.assertIn("personaState", html)
        self.assertIn("Mon identité", html)

    def test_version(self):
        gradle = (ROOT / "app/build.gradle").read_text(encoding="utf-8")
        self.assertIn("versionCode 56", gradle)
        self.assertIn("versionName '3.4.1'", gradle)

if __name__ == "__main__":
    unittest.main()
