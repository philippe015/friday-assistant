"""Regression: extended encrypted memory must not leak history to network or fill JNI."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/fr/friday/assistant"
HISTORY=(JAVA/"PrivateConversationHistory.java").read_text()
CONTEXT=(JAVA/"DialogueContext.java").read_text()
VOICE=(JAVA/"FridayService.java").read_text()
TYPED=(JAVA/"MainActivity.java").read_text()
LITERT=(JAVA/"LiteRtPromptBudget.java").read_text()
LOG=(JAVA/"DiagnosticLog.java").read_text()
ROUTING=(JAVA/"JarvisInternet.java").read_text()
CLARIFY=(JAVA/"ConversationClarificationPolicy.java").read_text()
SCRIPT=(ROOT/"scripts/tester_sources.sh").read_text()

class EnhancedConversationMemory(unittest.TestCase):
    def test_capacity_is_bounded_and_kept_encrypted(self):
        self.assertIn("MAX_EVENTS = 240", HISTORY)
        self.assertIn("MAX_JSON_CHARS = 320000", HISTORY)
        self.assertIn("MAX_MESSAGE_CHARS = 16000", HISTORY)
        self.assertIn("while (clean.length() > MAX_EVENTS", HISTORY)
        self.assertIn("clean.remove(0)", HISTORY)
        self.assertIn("prefs.putPrivateSecret(KEY, clean.toString())", HISTORY)
        self.assertIn("prefs.privateSecret(KEY)", HISTORY)
        self.assertNotIn("HttpURLConnection", HISTORY)
        self.assertNotIn("github.com", HISTORY)

    def test_retrieval_is_selected_not_all_240_messages(self):
        self.assertIn("MAX_DIALOGUE_MESSAGES = 16", CONTEXT)
        self.assertIn("MAX_RECALLED_TOPICS = 4", CONTEXT)
        self.assertIn("MAX_DIALOGUE_CHARACTERS = 4200", CONTEXT)
        self.assertIn("ConversationRecallPolicy.score", CONTEXT)
        self.assertIn("MAX_JSON_CHARS", HISTORY)
        self.assertIn("lastBytes(privatePart, contextBudget)", LITERT)
        self.assertIn("privatePart.replace", LITERT)
        self.assertIn("maxBytes <= 1800 ? 135 : 500", LITERT)
        self.assertIn("RETRY_UTF8_BYTES = 1400", LITERT)

    def test_both_voice_and_typed_receive_same_private_memory(self):
        self.assertIn("DialogueContext.recent(this, settings, question)",VOICE)
        self.assertIn("DialogueContext.recent(",TYPED)
        self.assertIn("request,\n                                privateDialogue, new NativeAI.CancelToken()",TYPED)
        self.assertIn("request,\n                                privateDialogue, new NativeAI.CancelToken()",TYPED)
        self.assertIn("ConversationClarificationPolicy.missingContextQuestion(",VOICE)
        self.assertIn("ConversationClarificationPolicy.missingContextQuestion(",TYPED)
        self.assertIn("privateDialogue, cancellation", ROUTING)
        self.assertIn("privateDialogue", ROUTING)
        self.assertNotIn("privateDialogue.toString()", ROUTING)

    def test_ui_and_public_diagnostics_remain_bounded(self):
        self.assertIn("Math.max(0, all.length() - 80)",TYPED)
        self.assertIn("visible.toString()",TYPED)
        self.assertIn('Math.max(0, history.length() - 80)',LOG)
        self.assertIn("PrivateConversationHistory.read(app, prefs)",LOG)
        self.assertIn("if (includeConversation)",LOG)
        self.assertIn("Last 80", LOG)

    def test_clarification_and_regression_are_on_device(self):
        self.assertIn('missingContextQuestion(String question, String privateDialogue)',CLARIFY)
        self.assertIn('privateDialogue.trim().isEmpty()',CLARIFY)
        self.assertNotIn('HttpURLConnection',CLARIFY)
        self.assertNotIn('URL(',CLARIFY)
        self.assertIn("tests/ConversationClarificationPolicyTest.java",SCRIPT)
        self.assertIn("fr.friday.assistant.ConversationClarificationPolicyTest",SCRIPT)

if __name__=="__main__":
    unittest.main()
