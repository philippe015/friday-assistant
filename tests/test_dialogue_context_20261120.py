"""Dialogue continuity and privacy regressions for on-device JARVIS."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/fr/friday/assistant"
SERVICE=(JAVA/"FridayService.java").read_text(encoding="utf-8")
WEB=(JAVA/"JarvisInternet.java").read_text(encoding="utf-8")
SEARCH=(JAVA/"WebSearch.java").read_text(encoding="utf-8")
LOCAL=(JAVA/"LocalJarvisEngine.java").read_text(encoding="utf-8")
DIALOG=(JAVA/"DialogueContext.java").read_text(encoding="utf-8")
STORAGE=(JAVA/"PrivateConversationHistory.java").read_text(encoding="utf-8")
POLICY=(JAVA/"DialogueMemoryPolicy.java").read_text(encoding="utf-8")

class EncryptedDialogueMemory(unittest.TestCase):
    def test_root_cause_fixed_context_is_used(self):
        self.assertIn("DialogueContext.recent(this, settings, question)", SERVICE)
        self.assertIn("privateDialogue, turn.api", SERVICE)
        self.assertIn("buildLocalPrompt(cfg, cleaned, privateDialogue)", LOCAL)
        self.assertIn("[CONTEXTE DE DIALOGUE PRIVÉ", LOCAL)
        self.assertIn("Demande ACTUELLE", LOCAL)
        self.assertIn("Utilisateur :", DIALOG)
        self.assertIn("Jarvis :", DIALOG)

    def test_saved_history_is_keystore_only(self):
        self.assertIn("PrivateConversationHistory.read(context, prefs)", DIALOG)
        self.assertIn('private static final int MAX_EVENTS = 80', STORAGE)
        self.assertIn('prefs.putPrivateSecret(KEY, clean.toString())', STORAGE)
        self.assertNotIn("http://", DIALOG)
        self.assertNotIn("https://", DIALOG)

    def test_conversation_questions_skip_unrelated_web_searches(self):
        self.assertIn("DialogueMemoryPolicy.keepLocal(question)", WEB)
        self.assertIn("DialogueMemoryPolicy.keepLocal(question)", SEARCH)
        self.assertIn("DialogueMemoryPolicy.asksMemoryStatus(question)", SERVICE)
        self.assertIn("DialogueContext.memoryStatus(this, settings, question)", SERVICE)
        self.assertIn("Jarvis ", SERVICE)

    def test_private_history_never_appended_to_search_requests(self):
        self.assertIn("sources = research(question, cancellation);",WEB)
        self.assertNotIn("research(privateDialogue",WEB)
        self.assertNotIn("search(privateDialogue",WEB)
        self.assertIn("LocalJarvisEngine.answer(\n                context, settings, prompt, privateDialogue, cancellation)",WEB)
        self.assertIn("Ni mémoire personnelle, ni conversation complète, ni position GPS",WEB)

    def test_context_is_bounded_and_current_question_not_duplicated(self):
        self.assertIn("MAX_DIALOGUE_MESSAGES = 14",DIALOG)
        self.assertIn("MAX_DIALOGUE_CHARACTERS = 2600",DIALOG)
        self.assertIn("previousEnd(events, currentQuestion)",DIALOG)
        self.assertIn("chosen.addFirst(line)",DIALOG)
        self.assertIn("NORMAL_DIALOGUE_MESSAGES = 6",DIALOG)
        self.assertIn("NORMAL_DIALOGUE_CHARACTERS = 1250",DIALOG)
        self.assertIn("DialogueMemoryPolicy.isContextualFollowUp(currentQuestion)",DIALOG)
        self.assertIn("hasWebSources ? 800 : 2900",LOCAL)

    def test_no_breakage_for_typed_questions(self):
        self.assertIn('textQuestions.offer("Jarvis " + clean)',SERVICE)

if __name__ == "__main__":
    unittest.main()
