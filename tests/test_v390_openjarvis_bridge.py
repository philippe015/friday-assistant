"""From v3.12.3, the legacy PC bridge must never be active or exposed."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"

def read(name):
    return (JAVA / name).read_text(encoding="utf-8")

class OnlyLocalJarvisAndMistral(unittest.TestCase):
    def test_desktop_bridge_not_accessible_from_ui_or_voice(self):
        ui = read("MainActivity.java")
        voice = read("FridayService.java")
        self.assertNotIn("OpenJarvisClient.", voice)
        self.assertNotIn("OpenJarvisPolicy.", voice)
        self.assertNotIn("pcJarvis", voice)
        self.assertNotIn("testOpenJarvis()", ui)
        self.assertNotIn("saveOpenJarvisPrefs()", ui)
        self.assertNotIn("openJarvisRouteAll", ui)
        self.assertIn('prefs.put("openjarvis_enabled", "0")', ui)
        self.assertIn('prefs.put("openjarvis_route_all", "0")', ui)

    def test_jarvis_is_the_only_active_ai_engine(self):
        voice=read("FridayService.java")
        ui=read("MainActivity.java")
        cloud=read("NativeAI.java")
        self.assertIn("boolean requestedOnDevice = true;",voice)
        self.assertIn("LocalJarvisEngine.answer(this, settings, question, turn.api)",voice)
        self.assertNotIn("NativeAI.answer(settings, prepared",voice)
        self.assertIn('prefs.put("local_jarvis_all", "1")',ui)
        self.assertIn('prefs.put("web_enabled", "0")',ui)
        self.assertIn("blockCloud();",cloud)

    def test_historical_bridge_library_cannot_be_invoked(self):
        client = read("OpenJarvisClient.java")
        self.assertIn('settings.get("openjarvis_enabled", "0")', client)
        self.assertNotIn("OpenJarvisClient.answer", read("FridayService.java"))
        self.assertNotIn("OpenJarvisClient.test", read("MainActivity.java"))

    def test_apk_version(self):
        gradle = (ROOT / "app/build.gradle").read_text(encoding="utf-8")
        self.assertIn("versionCode 88", gradle)
        self.assertIn("versionName '3.13.3'", gradle)

if __name__ == "__main__":
    unittest.main()
