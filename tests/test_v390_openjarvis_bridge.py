"""FRIDAY v3.9.0 OpenJarvis integration security / regression checks."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/fr/friday/assistant"

def read(name):
    return (JAVA/name).read_text(encoding="utf-8")

class OpenJarvisIntegrationTest(unittest.TestCase):
    def test_off_by_default_and_explicit_opt_in(self):
        policy=read("OpenJarvisPolicy.java")
        bridge=read("OpenJarvisClient.java")
        ui=read("MainActivity.java")
        self.assertIn('settings.get("openjarvis_enabled", "0")', bridge)
        self.assertIn('prefs.get("openjarvis_enabled", "0")', ui)
        self.assertIn('prefs.get("openjarvis_route_all", "0")', ui)
        self.assertIn('prefs.put("openjarvis_enabled", enabled ? "1" : "0")', ui)
        self.assertIn('if (!enabled || question == null', policy)
        self.assertIn("OpenJarvisPolicy.shouldRoute(", read("FridayService.java"))

    def test_uses_real_openjarvis_compatible_api(self):
        client=read("OpenJarvisClient.java")
        policy=read("OpenJarvisPolicy.java")
        self.assertIn('"/v1/chat/completions"', policy)
        self.assertIn('"/health"', policy)
        self.assertIn('"messages", new JSONArray()', client)
        self.assertIn('"stream", false', client)
        self.assertIn('choices.optJSONObject(0).optJSONObject("message")', client)
        self.assertIn('new URL(endpoint).openConnection()', client)
        self.assertIn("connectTimeout" if False else "setConnectTimeout(10000)", client)

    def test_https_authenticated_and_no_secret_in_logs(self):
        client=read("OpenJarvisClient.java")
        policy=read("OpenJarvisPolicy.java")
        self.assertIn('!"https".equalsIgnoreCase(uri.getScheme())', policy)
        self.assertIn('uri.getRawUserInfo() != null', policy)
        self.assertIn('uri.getRawQuery() != null', policy)
        self.assertIn('"localhost".equals(host)', policy)
        self.assertIn("connection.setInstanceFollowRedirects(false)", client)
        self.assertIn('"Authorization", "Bearer " + token', client)
        self.assertIn('settings.secret("openjarvis")', client)
        self.assertIn('prefs.setSecret("openjarvis", newKey)', read("MainActivity.java"))
        self.assertNotIn('Log.', client)
        self.assertNotIn('System.out', client)

    def test_does_not_upload_gps_memory_or_conversation_implicitly(self):
        service=read("FridayService.java")
        client=read("OpenJarvisClient.java")
        self.assertIn('OpenJarvisClient.answer(settings, question, turn.api)', service)
        self.assertIn('locationForApi != null && local == null', service)
        self.assertIn('&& !pcJarvis && !localJarvis', service)
        self.assertNotIn("PersonalMemory.", client)
        self.assertNotIn("locationContext", client)
        self.assertNotIn("historySnapshot()", client)
        self.assertIn("OpenJarvisPolicy.cleanQuestion(question)", client)
        self.assertIn("NativeAI.answer(settings, prepared", service)
        self.assertIn('new JSONObject().put("type", "web_search")', read("NativeAI.java"))

    def test_diagnostics_without_background_request(self):
        ui=read("MainActivity.java")
        self.assertIn("private void testOpenJarvis()", ui)
        self.assertIn("OpenJarvisClient.test(prefs)", ui)
        self.assertIn("openJarvisTestRunning.compareAndSet(false, true)", ui)
        self.assertIn("prefs.put(\"openjarvis_last_status\", text)", ui)
        self.assertIn("mode Mistral" if False else "Mistral reste le moteur par défaut", ui)

    def test_apk_version(self):
        gradle=(ROOT/"app/build.gradle").read_text(encoding="utf-8")
        self.assertIn("versionCode 76", gradle)
        self.assertIn("versionName '3.11.1'", gradle)

if __name__=="__main__":
    unittest.main()
