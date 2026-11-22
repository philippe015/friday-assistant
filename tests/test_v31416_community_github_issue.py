"""Opt-in GitHub bug reporting should share only bounded technical event codes."""
from pathlib import Path
import re
import unittest

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/fr/friday/assistant"
LOG=(JAVA/"DiagnosticLog.java").read_text(encoding="utf8")
MAIN=(JAVA/"MainActivity.java").read_text(encoding="utf8")
HTML=(ROOT/"app/src/main/assets/index.html").read_text(encoding="utf8")


class CommunityIssuePrivacyRegression(unittest.TestCase):
    def test_issue_draft_is_exclusively_technical(self):
        self.assertIn("public static String githubIssueBody(Context context)", LOG)
        chunk=LOG.split("public static String githubIssueBody(Context context)",1)[1].split(
            "/** Local preview",1)[0]
        self.assertIn("read(new SecurePrefs(app))",chunk)
        self.assertIn('Math.max(0, entries.length() - 28)',chunk)
        self.assertIn("DiagnosticLogPolicy.allowedCode(code)",chunk)
        self.assertIn('entry.optInt("duration_ms", -1)',chunk)
        self.assertIn("(timestamp - base) / 1000",chunk)
        self.assertNotIn('PrivateConversationHistory',chunk)
        self.assertNotIn('message.optString("text"',chunk)
        self.assertNotIn('time_utc',chunk)
        self.assertNotIn('longitude',chunk)
        self.assertNotIn('latitude',chunk)
        self.assertNotIn('HttpURLConnection',chunk)
        self.assertNotIn('contentResolver',chunk)

    def test_user_must_confirm_before_browser_is_opened(self):
        method=MAIN.split("private void requestGitHubIssue()",1)[1].split(
            "private void confirmClearDiagnosticLogs()",1)[0]
        self.assertIn("DiagnosticLog.githubIssueBody(this)",method)
        self.assertIn('new AlertDialog.Builder(this)',method)
        self.assertIn('.setNegativeButton("Annuler", null)',method)
        self.assertIn('.setPositiveButton("Ouvrir GitHub"',method)
        self.assertIn('new Intent(Intent.ACTION_VIEW, destination)',method)
        self.assertIn("Intent.CATEGORY_BROWSABLE",method)
        self.assertIn('https://github.com/philippe015/',method)
        self.assertIn('friday-assistant/issues/new',method)
        self.assertIn('.appendQueryParameter("body", draft)',method)
        self.assertIn("transmis à GitHub dès",method)
        self.assertIn("aucun ticket n'est publié",method)
        self.assertNotIn('PrivateConversationHistory.read',method)
        self.assertNotIn('DiagnosticLog.exportJson',method)
        self.assertNotIn('getAccessToken',method)

    def test_cockpit_action_is_not_automatic_or_conversation_share(self):
        self.assertIn('id="diagnosticGithubBtn"',HTML)
        self.assertIn("call('reportDiagnosticGitHub')",HTML)
        self.assertIn("void reportDiagnosticGitHub()",MAIN)
        self.assertIn("runOnUiThread(MainActivity.this::requestGitHubIssue)",MAIN)
        self.assertIn("sans conversations ni données privées",HTML)
        self.assertIn("L'utilisateur doit approuver",HTML)
        self.assertIn("ne le publiez pas sur GitHub",HTML)
        self.assertIn('this::requestGitHubIssue',MAIN)

    def test_local_conversation_export_remains_separate(self):
        self.assertIn("return exportJson(context, false)",LOG)
        self.assertIn('if (includeConversation) {',LOG)
        self.assertIn("PrivateConversationHistory.read(app, prefs)",LOG)
        self.assertIn("report.put(\"conversation\", conversation)",LOG)
        self.assertNotIn("githubIssueBody(this, true)",MAIN)


if __name__ == "__main__":
    unittest.main()
