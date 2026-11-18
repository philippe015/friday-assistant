"""FRIDAY 3.8.5: Web diagnostic must be visible; Always mode must reach Conversations."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"

def read(name):
    return (JAVA / name).read_text(encoding="utf-8")

class WebDiagnosticV385(unittest.TestCase):
    def test_test_button_has_instant_and_persistent_feedback(self):
        ui = read("MainActivity.java")
        self.assertIn('webDiagnosticLabel = label(prefs.get("web_last_test_status"', ui)
        self.assertIn('setTitle("FRIDAY · diagnostic Internet")', ui)
        self.assertIn("webDiagnosticLabel.setText(pending)", ui)
        self.assertIn("new Thread(() -> {", ui)
        section = ui[ui.index("private void enableAndTestWeb()"):ui.index("private void testHttpsPage()")]
        self.assertIn("NativeAI.testWeb(prefs)", section)
        self.assertIn("NativeAI.explainWebTestFailure(error)", section)
        self.assertIn('prefs.put("web_last_test_status", detail)', section)
        self.assertIn("runOnUiThread(() -> {", section)
        self.assertIn("progress.setMessage(detail)", section)
        self.assertIn("new AlertDialog.Builder(this)", section)
        self.assertIn('prefs.put("web_verified", verified ? "1" : "0")', section)

    def test_always_mode_covers_streaming_and_normal(self):
        ui = read("MainActivity.java")
        ai = read("NativeAI.java")
        web = read("WebSearch.java")
        self.assertIn('webAlways.setChecked("always".equals(prefs.get("web_mode", "always")))', ui)
        self.assertIn('prefs.put("web_mode", webAlways.isChecked() ? "always" : "auto")', ui)
        self.assertIn('WebSearch.shouldSearch(cfg.get("web_mode", "always"), question, previous)', ai)
        self.assertIn('WebSearch.shouldSearch(settings.get("web_mode", "always"), question, previous)', ai)
        self.assertIn('"always".equals(mode) || needsWeb(question, previousUserQuestion)', web)

    def test_failure_diagnostics_are_safe_and_specific(self):
        ai = read("NativeAI.java")
        self.assertIn("explainWebTestFailure(Exception error)", ai)
        self.assertIn("reason.contains(\"(401)\")", ai)
        self.assertIn("reason.contains(\"(403)\")", ai)
        self.assertIn("reason.contains(\"(429)\")", ai)
        self.assertIn('new JSONObject().put("type", "web_search")', ai)
        self.assertIn('MISTRAL_BASE + "/conversations"', ai)

    def test_version(self):
        gradle = (ROOT / "app/build.gradle").read_text(encoding="utf-8")
        self.assertIn("versionCode 80", gradle)
        self.assertIn("versionName '3.12.2'", gradle)

if __name__ == "__main__":
    unittest.main()
