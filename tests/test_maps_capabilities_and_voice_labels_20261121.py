"""FRIDAY regression: spoken STOP removed and Maps capability questions are local.

This static audit cannot establish performance on a physical Android device.
"""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/fr/friday/assistant"
def source(name): return (JAVA/name).read_text(encoding="utf-8")

class VoiceMapsDiagnosticsTest(unittest.TestCase):
    def test_speech_ui_no_longer_claims_spoken_stop(self):
        voice=source("FridayService.java")
        self.assertNotIn("STOP actif pendant la voix", voice)
        self.assertNotIn("STOP disponible", voice)
        self.assertNotIn("Dites « STOP » pour interrompre", voice)
        self.assertIn("STOP vocal désactivé · bouton disponible", voice)
        self.assertIn("Utilisez le bouton Interrompre pour arrêter", voice)

    def test_voice_status_accurately_describes_web_permission(self):
        voice=source("FridayService.java")
        self.assertNotIn('"Jarvis sur le téléphone, sans réseau"', voice)
        self.assertIn("JarvisInternet.shouldResearch(settings, question)", voice)
        self.assertIn('event("latency", "Réponse préparée en "', voice)
        self.assertIn('event("latency", "Première parole après "', voice)

    def test_maps_capability_uses_local_phone_tool_not_unrelated_web_sources(self):
        policy=source("JarvisToolPolicy.java")
        tools=source("JarvisAndroidTools.java")
        self.assertIn("CAPABILITIES, MAPS,", policy)
        self.assertIn("case MAPS: return mapsAccess(context, settings)", tools)
        self.assertIn("Je n'ai pas d'accès direct à l'application Google Maps", tools)
        self.assertIn("FridayLocation.hasPermission(context)", tools)
        self.assertIn("FridayLocation.providerEnabled(context)", tools)
        self.assertNotIn("HttpURLConnection", tools)
        self.assertNotIn("Google Maps API", tools)

if __name__=="__main__":
    unittest.main()
