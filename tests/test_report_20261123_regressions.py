"""Non-sensitive reproductions of the 2026-11-23 JARVIS conversation report."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"

def java(name):
    return (JAVA/name).read_text(encoding="utf-8")

class Report20261123(unittest.TestCase):
    def test_identity_is_local_for_voice_and_text(self):
        android = java("JarvisAndroidTools.java")
        identity = java("AssistantIdentity.java")
        voice = java("FridayService.java")
        typed = java("MainActivity.java")
        self.assertIn("AssistantIdentity.reply(utterance", android)
        self.assertLess(android.index("AssistantIdentity.reply("),
                        android.index("JarvisWeather.answer("))
        self.assertIn("JarvisAndroidTools.answer(this, settings, question)", voice)
        self.assertIn("JarvisAndroidTools.answer(", typed)
        self.assertNotIn("Mistral est uniquement le moteur", identity)
        self.assertNotIn("[Information non spécifiée]", identity)

    def test_utterance_does_not_make_envie_a_news_topic(self):
        news = java("RecentNewsPolicy.java")
        internet = java("JarvisInternet.java")
        self.assertIn("cleanVoiceNewsRequest", news)
        self.assertIn("RecentNewsPolicy.cleanVoiceNewsRequest(question)", internet)
        self.assertIn("de cette nuit", news)

    def test_short_rss_followup_remains_private_and_local(self):
        kotlin = java("JarvisKotlinBrain.kt")
        rss = java("RssArticleIntent.java")
        self.assertIn('Regex("^(?:(?:de|des|dit|lire) )?en entier$")', kotlin)
        self.assertIn("ARTICLE_COMPLETE_BEFORE_NUMBER", rss)
        self.assertIn("ARTICLE_COMPLETE_BEFORE_NUMBER.matcher(text).find()", rss)

    def test_remote_gps_error_categories_are_non_private(self):
        nearby = java("NearbyPlaceSearch.java")
        for code in ("NEARBY_REMOTE_TIMEOUT", "NEARBY_REMOTE_DNS_FAILED",
                     "NEARBY_REMOTE_TLS_FAILED", "NEARBY_REMOTE_HTTP_OR_IO"):
            self.assertIn('DiagnosticLog.record(ctx, "' + code + '")', nearby)
        self.assertNotIn('DiagnosticLog.record(ctx, unavailable.getMessage()', nearby)

if __name__ == "__main__":
    unittest.main()
