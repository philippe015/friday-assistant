"""Reproduction of 22 Nov 2026 on-phone v3.14.14 conversation report.
Four turns: news was searched on 'envie' due to Vosk dictation; three article
reading attempts were blocked by a too-strict verb regex and generic prompt.
"""
import pathlib
import unittest

ROOT=pathlib.Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/fr/friday/assistant"
RSS=(JAVA/"RssArticleIntent.java").read_text()
SERVICE=(JAVA/"FridayService.java").read_text()
INTERNET=(JAVA/"JarvisInternet.java").read_text()

class RssRealConversationRecovery(unittest.TestCase):
    def test_explicit_article_number_beats_wrong_speech_verb(self):
        self.assertIn("COMPLETE_READING", RSS)
        self.assertIn("boolean completeNumbered = numbered && COMPLETE_READING.matcher(text).find()", RSS)
        self.assertIn("RssArticleIntent.wantsFullText(question)", INTERNET)
        self.assertIn("RssArticleIntent.isRequest(question)", INTERNET)
        self.assertIn("RssArticleIntent.ambiguousVoiceReading(question)", SERVICE)

    def test_no_guess_when_asr_loses_article_number(self):
        self.assertIn('public static boolean missingNumber(String question)', RSS)
        self.assertIn('RSS_VOSK_NUMBER_MISSING', SERVICE)
        self.assertIn('je ne vais pas', SERVICE)
        self.assertIn('deviner votre choix', SERVICE)
        self.assertIn('!isBareReference(question)', RSS)
        self.assertIn('article numéro deux', SERVICE)

    def test_generic_news_not_misclassified_as_envie_de(self):
        self.assertIn('j\\\\s+ai\\\\s+envie\\\\s+de\\\\s+moi', INTERNET)
        self.assertIn('words = words.replaceFirst(', INTERNET)
        # No stripping of an actual 'envie' topic in arbitrary question.
        self.assertIn('(?=(?:les\\\\s+)?dernieres\\\\s+actualites\\\\b)', INTERNET)
        self.assertIn('recentNewsTopic(q)', INTERNET)

if __name__=="__main__":
    unittest.main()
