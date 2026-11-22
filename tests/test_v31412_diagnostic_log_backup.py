import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parent.parent
SERVICE = (ROOT / "app/src/main/java/fr/friday/assistant/FridayService.java").read_text()
LOG = (ROOT / "app/src/main/java/fr/friday/assistant/DiagnosticLog.java").read_text()
MAIN = (ROOT / "app/src/main/java/fr/friday/assistant/MainActivity.java").read_text()
HTML = (ROOT / "app/src/main/assets/index.html").read_text()
RSS = (ROOT / "app/src/main/java/fr/friday/assistant/JarvisInternet.java").read_text()


class DiagnosticLogPrivacyTest(unittest.TestCase):
    def test_keystore_storage_and_retention(self):
        self.assertIn("putPrivateSecret(SECRET", LOG)
        self.assertIn("privateSecret(SECRET)", LOG)
        self.assertIn("removePrivateSecret(SECRET)", LOG)
        self.assertIn("RETENTION_MS = 14L", LOG)
        self.assertIn("MAX_EVENTS = 420", LOG)
        self.assertIn("Executors.newSingleThreadExecutor", LOG)
        self.assertNotIn("openFileOutput(", LOG)
        self.assertNotIn("HttpURLConnection", LOG)

    def test_user_conversation_not_sent_to_diagnostic_writer(self):
        self.assertIn("DiagnosticLog.fromEvent(this, kind, value)", SERVICE)
        self.assertIn('case "user": case "assistant": case "tts": case "partial":',
                      (ROOT / "app/src/main/java/fr/friday/assistant/DiagnosticLogPolicy.java").read_text())
        self.assertIn('"RSS_CONTENT_UNAVAILABLE"', RSS)
        self.assertIn('"RSS_LOCAL_ANALYSIS_FAILED"', RSS)
        self.assertIn('"RSS_TEXT_EXTRACTED"', RSS)

    def test_manual_export_document_and_delete(self):
        self.assertIn("Intent.ACTION_CREATE_DOCUMENT", MAIN)
        self.assertIn("REQ_DIAGNOSTIC_EXPORT", MAIN)
        self.assertIn("DiagnosticLog.exportJson(this)", MAIN)
        self.assertIn("DiagnosticLog.clear(this)", MAIN)
        self.assertIn("setTitle(\"Sauvegarde des logs de diagnostic\")", MAIN)
        self.assertIn("exportDiagnosticLogs", MAIN)
        self.assertIn("showDiagnosticLogs", MAIN)
        self.assertIn("clearDiagnosticLogs", MAIN)
        for element in ("diagnosticExportBtn", "diagnosticViewBtn",
                        "diagnosticClearBtn"):
            self.assertIn(f'id="{element}"', HTML)
            self.assertIn(f"$('"+element+"').onclick", HTML)


if __name__ == "__main__":
    unittest.main()
