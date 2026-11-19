"""FRIDAY 3.8.6: Web access always enabled for user questions; never fake execution."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"

def read(name):
    return (JAVA / name).read_text(encoding="utf-8")

class WebAlwaysDefaultV386(unittest.TestCase):
    def test_one_time_upgrade_enables_always_without_replacing_key(self):
        ui = read("MainActivity.java")
        self.assertIn('prefs.get("web_always_default_v386", "0")', ui)
        self.assertIn('prefs.put("web_enabled", "1");', ui)
        self.assertIn('prefs.put("web_mode", "always");', ui)
        self.assertIn('prefs.put("web_always_default_v386", "1");', ui)
        self.assertIn('webAlways.setChecked("always".equals(prefs.get("web_mode", "always")))', ui)
        self.assertIn('prefs.put("web_mode", webAlways.isChecked() ? "always" : "auto")', ui)
        self.assertIn('prefs.hasSecret("mistral")', ui)
        self.assertIn("webDiagnosticLabel = label(prefs.get(\"web_last_test_status\"", ui)

    def test_both_mistral_paths_route_to_web_every_question(self):
        ai = read("NativeAI.java")
        self.assertIn('WebSearch.shouldSearch(settings.get("web_mode", "always"), question, previous)', ai)
        self.assertIn('WebSearch.shouldSearch(cfg.get("web_mode", "always"), question, previous)', ai)
        self.assertIn('MISTRAL_BASE + "/conversations"', ai)
        self.assertIn('new JSONObject().put("type", "web_search")', ai)
        self.assertIn('.put("store", false)', ai)
        self.assertNotIn('.put("tool_choice", "any")', ai)

    def test_second_targeted_web_attempt_is_bounded_and_source_honest(self):
        ai = read("NativeAI.java")
        section = ai[ai.index("private static WebReply conversationWithFallback("):
                     ai.index("/** Vérification réelle de l'option Web", ai.index("private static WebReply conversationWithFallback("))]
        self.assertIn("WebReply initial = conversation(cfg, key, model, question, history)", section)
        self.assertIn("if (initial.references > 0", section)
        self.assertIn("return initial;", section)
        self.assertIn("WebToolPolicy.retryRejectedModel(", section)
        self.assertIn("String targeted =", section)
        self.assertIn("return conversation(cfg, key, DEFAULT_MODEL, targeted, history);", section)
        self.assertNotIn("while (", section)
        self.assertIn("if (!found.executed && found.references == 0)", ai)
        self.assertIn("Mistral n'a pas effectué de recherche Web", ai)

    def test_distinct_live_query_and_connection_test_telemetry(self):
        ai = read("NativeAI.java")
        ui = read("MainActivity.java")
        self.assertIn('cfg.put("web_last_test_status", "Recherche vérifiée via "', ai)
        self.assertIn('prefs.put("web_last_test_status", detail);', ui)
        self.assertIn('String recent = prefs.get("web_last_status", "");', ui)
        self.assertIn('" · Dernière réponse : " + recent', ui)

    def test_version_privacy_and_stop_remain(self):
        gradle = (ROOT / "app/build.gradle").read_text(encoding="utf-8")
        self.assertIn("versionCode 84", gradle)
        self.assertIn("versionName '3.12.6'", gradle)
        self.assertIn('cfg.get("memory_share_mistral", "0")', read("PersonalMemory.java"))
        self.assertIn("TtsEchoGate.suppressGeneralAsr(", read("FridayService.java"))
        self.assertIn("stopRecognizer.acceptWaveForm(buffer, count)", read("FridayService.java"))

if __name__ == "__main__":
    unittest.main()
