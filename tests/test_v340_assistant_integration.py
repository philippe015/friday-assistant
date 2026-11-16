"""Contrôles d'intégration FRIDAY v3.4.0 sur sources (aucune clé API)."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"

def file(path):
    return (ROOT / path).read_text(encoding="utf-8")

class AssistantIntegrations(unittest.TestCase):
    def test_web_telemetry_not_hardcoded(self):
        html = file("app/src/main/assets/index.html")
        activity = file("app/src/main/java/fr/friday/assistant/MainActivity.java")
        self.assertNotIn("$('webState').textContent='Désactivé'", html)
        self.assertIn("call('webTelemetry')", html)
        self.assertIn("web_verified", activity)
        self.assertIn('prefs.get("web_enabled", "1")', activity)
        self.assertIn("call('gpsTelemetry')", html)
        self.assertIn("call('activeProfileName')", html)

    def test_active_profile_sent_to_mistral(self):
        ai = file("app/src/main/java/fr/friday/assistant/NativeAI.java")
        self.assertIn("PersonalityProfiles.activePrompt(cfg)", ai)
        self.assertIn("PersonalityProfiles.activeName(cfg)", ai)
        self.assertIn('"instructions", system(cfg, question)', ai)
        self.assertIn('"content", system(cfg, question)', ai)
        self.assertIn("PROMPT DU PROFIL ACTIF", ai)
        profiles = file("app/src/main/java/fr/friday/assistant/PersonalityProfiles.java")
        self.assertIn("expected.equals(persisted)", profiles)
        self.assertIn("public static String activeName", profiles)

    def test_interruption_retains_context(self):
        service = file("app/src/main/java/fr/friday/assistant/FridayService.java")
        self.assertIn("clarificationAnchor", service)
        self.assertIn("clarificationContext", service)
        self.assertIn("CONTEXTE DE LA DEMANDE INTERROMPUE", service)
        self.assertIn("BargeInPolicy.contextualFollowUp(question)", service)
        self.assertIn("BargeInPolicy.wantsToAddDetails(heard)", service)

    def test_location_access_requires_consent(self):
        service = file("app/src/main/java/fr/friday/assistant/FridayService.java")
        activity = file("app/src/main/java/fr/friday/assistant/MainActivity.java")
        self.assertIn('settings.get("location_share", "0")', service)
        self.assertIn('settings.get("location_enabled", "0")', service)
        self.assertIn("FridayLocation.providerEnabled(this)", service)
        self.assertIn("configureGpsForAssistant()", activity)
        self.assertIn("Autoriser le partage ponctuel", activity)
        self.assertIn('prefs.put("location_share", "0")', activity)
        self.assertIn('if (kind.equals("location"))', activity)

    def test_version(self):
        gradle = file("app/build.gradle")
        import re
        self.assertGreaterEqual(int(re.search(r"versionCode (\d+)", gradle).group(1)), 55)
        match = re.search(r"versionName '(\d+)\.(\d+)\.(\d+)'", gradle)
        self.assertIsNotNone(match)
        self.assertGreaterEqual(tuple(int(x) for x in match.groups()), (3, 4, 0))

if __name__ == "__main__":
    unittest.main()
