"""Opt-in conversation export: technical logs remain private by default.
No private speech or history may enter the encrypted technical event store.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parent.parent
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"
ACTIVITY = (JAVA / "MainActivity.java").read_text()
LOG = (JAVA / "DiagnosticLog.java").read_text()
HISTORY = (JAVA / "PrivateConversationHistory.java").read_text()
SERVICE = (JAVA / "FridayService.java").read_text()
HTML = (ROOT / "app/src/main/assets/index.html").read_text()


class ConversationReportOptinTests(unittest.TestCase):
    def test_explicit_default_off_and_manual_confirmation(self):
        self.assertIn("return exportJson(context, false);", LOG)
        self.assertIn("if (includeConversation) {", LOG)
        self.assertIn("PrivateConversationHistory.read(app, prefs)", LOG)
        self.assertIn("includeConversation.setChecked(false)", ACTIVITY)
        self.assertIn("pendingDiagnosticIncludeConversation = false", ACTIVITY)
        self.assertIn('includeConversation.isChecked()', ACTIVITY)
        self.assertIn("DiagnosticLog.exportJson(this, includeConversation)", ACTIVITY)
        self.assertIn("Intent.ACTION_CREATE_DOCUMENT", ACTIVITY)
        self.assertIn("n'est PAS chiffré", ACTIVITY)
        self.assertIn("Aucun envoi automatique", ACTIVITY)

    def test_encrypted_original_and_timestamped_channel(self):
        self.assertIn("prefs.putPrivateSecret(KEY, clean.toString())", HISTORY)
        self.assertIn("prefs.privateSecret(KEY)", HISTORY)
        self.assertIn('.put("time_ms", System.currentTimeMillis())', HISTORY)
        self.assertIn('.put("channel", channel)', HISTORY)
        self.assertIn('kind, value, "voice"', SERVICE)
        self.assertIn('kind, text, "unknown"', HISTORY)
        self.assertIn('request, "text"', ACTIVITY)
        self.assertIn('answer, "text"', ACTIVITY)
        self.assertIn('message.optLong("time_ms", 0)', LOG)
        self.assertIn('item.put("time_unknown", true)', LOG)
        self.assertIn('report.put("conversation_count", conversation.length())', LOG)
        self.assertIn('report.put("timeline", timeline(entries, conversation))', LOG)

    def test_no_duplicate_sensitive_diagnostic_logging(self):
        self.assertIn('append(context, code, ms)', LOG)
        self.assertNotIn("append(context, kind, message)", LOG)
        self.assertNotIn("putPrivateSecret(SECRET, conversation", LOG)
        self.assertNotIn("sendBroadcast(", LOG)
        self.assertNotIn("HttpURLConnection", LOG)
        self.assertIn('item.put("text", message.optString("text", ""))', LOG)
        self.assertIn('point.put("type", "conversation_reference")', LOG)
        self.assertNotIn('point.put("text"', LOG)

    def test_cockpit_informs_user_of_sensitive_export(self):
        self.assertIn("Inclure la conversation", HTML)
        self.assertIn("le JSON n'est pas chiffré".lower(), HTML.lower())
        self.assertIn("Aucun envoi automatique", HTML)
        self.assertIn('id="diagnosticExportBtn"', HTML)
        self.assertIn('report.put("conversation_included", includeConversation)', LOG)


if __name__ == "__main__":
    unittest.main()
