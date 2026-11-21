"""Guard JARVIS Web latency, phone-only dialogue context, and microphone stability."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"
WEB = (JAVA / "JarvisInternet.java").read_text(encoding="utf-8")
MAIN = (JAVA / "MainActivity.java").read_text(encoding="utf-8")
VOICE = (JAVA / "FridayService.java").read_text(encoding="utf-8")
MEMORY = (JAVA / "DialogueContext.java").read_text(encoding="utf-8")


class JarvisQuickNewsAndPrivateDialogue(unittest.TestCase):
    def test_rss_recent_news_avoids_slow_optional_article_fetch(self):
        recent = WEB.split("private static List<Source> research(", 1)[1].split(
            "private static List<Source> search(", 1)[0]
        self.assertIn("sources.addAll(search(", recent)
        self.assertIn("RecentNewsPolicy.newsWindowHours(question) > 0", recent)
        self.assertLess(recent.index("RecentNewsPolicy.newsWindowHours(question) > 0"),
                        recent.index("WebPageReader.read(sources.get(i).url)"))
        self.assertIn("if (found.size() < 3)", WEB)
        self.assertIn("RssFreshness.withinHours(", WEB)
        self.assertIn("RecentNewsPolicy.civilTodayStartMillis(", WEB)

    def test_private_dialogue_reused_by_written_and_voice(self):
        self.assertIn("DialogueContext.recent(this, settings, question)", VOICE)
        typed = MAIN.split("private void sendQuestion(String question) {", 1)[1].split(
            "private void testVoice()", 1)[0]
        self.assertIn("DialogueContext.recent(", typed)
        self.assertIn("getApplicationContext(), prefs, request)", typed)
        self.assertIn("getApplicationContext(), prefs, request, privateDialogue,", typed)
        self.assertIn("privateDialogue, new NativeAI.CancelToken()", typed)
        self.assertIn("PrivateConversationHistory.append(", typed)
        self.assertIn("PrivateConversationHistory.read(context, prefs)", MEMORY)

    def test_web_only_gets_current_public_question(self):
        self.assertIn("sources = research(question, cancellation)", WEB)
        self.assertNotIn("research(privateDialogue", WEB)
        self.assertNotIn("fetch(privateDialogue", WEB)
        self.assertIn("Aucune mémoire privée n'a été envoyée", WEB)
        self.assertIn('!LocationIntent.nearby(request)', MAIN)

    def test_diagnostic_splits_network_and_local_response(self):
        self.assertIn("public final long researchMs", WEB)
        self.assertIn("public final long generationMs", WEB)
        self.assertIn("Temps recherche Internet :", VOICE)
        self.assertIn("génération Jarvis locale :", VOICE)
        self.assertIn('event("latency"', VOICE)

    def test_voice_mic_and_stop_unchanged(self):
        self.assertIn("ar.startRecording();", VOICE)
        self.assertIn("new JarvisVoiceDecoder.Vosk(model, grammar)", VOICE)
        self.assertIn("TtsEchoGate.suppressGeneralAsr(", VOICE)
        self.assertIn("TtsEchoGate.useStopRecognizer(", VOICE)


if __name__ == "__main__":
    unittest.main()
