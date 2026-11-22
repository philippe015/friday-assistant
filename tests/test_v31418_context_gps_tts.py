"""Régressions câblage des correctifs JARVIS v3.14.18 (sans réseau)."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"
def source(name):
    return (JAVA / name).read_text(encoding="utf-8")

class V31418Regression(unittest.TestCase):
    def test_typed_gps_uses_android_location_instead_of_generic_ai(self):
        activity = source("MainActivity.java")
        helper = source("JarvisLocationReply.java")
        self.assertIn("JarvisLocationReply.answer(getApplicationContext(), prefs, request)", activity)
        self.assertIn("FridayLocation.locate(ctx)", helper)
        self.assertIn("FridayLocation.hasPermission(ctx)", helper)
        self.assertIn("FridayLocation.providerEnabled(ctx)", helper)
        self.assertIn("!LocationIntent.nearby(request)", activity)

    def test_remote_geolocation_has_separate_opt_in(self):
        activity = source("MainActivity.java")
        helper = source("JarvisLocationReply.java")
        nearby = source("NearbyPlaceSearch.java")
        voice = source("FridayService.java")
        self.assertIn('prefs.get("nearby_online_enabled", "0")', helper)
        self.assertIn('settings.get("nearby_online_enabled", "0")', voice)
        self.assertIn('prefs.put("nearby_online_enabled"', activity)
        self.assertIn("nearbyOnlineEnabled.setChecked(", activity)
        self.assertIn('String.format(Locale.US, "%.3f", fix.lat)', nearby)
        self.assertIn('String.format(Locale.US, "%.3f", fix.lon)', nearby)
        self.assertIn("setConnectTimeout(6000)", nearby)
        self.assertIn("setReadTimeout(12000)", nearby)
        self.assertNotIn("PrivateConversationHistory", nearby)
        self.assertNotIn("api.mistral.ai", nearby)
        self.assertIn('prefs.put("location_share", "0")', activity)

    def test_ui_tts_blocks_vosk_until_last_chunk_finishes(self):
        activity = source("MainActivity.java")
        voice = source("FridayService.java")
        self.assertIn("FridayService.externalSpeechStarted(spoken)", activity)
        self.assertIn("webVoiceFinalUtterance = utteranceId", activity)
        self.assertIn("id.equals(webVoiceFinalUtterance)", activity)
        self.assertIn("FridayService.externalSpeechFinished()", activity)
        self.assertIn("speakingNow || externalSpeechActive", voice)
        self.assertIn("Math.max(echoBlockUntil, externalSpeechUntil)", voice)
        self.assertIn("service.lastSpokenForEcho = speech", voice)

    def test_article_index_keeps_last_rss_without_new_web_search(self):
        intent = source("RssArticleIntent.java")
        internet = source("JarvisInternet.java")
        self.assertIn("NEWS_NUMBER", intent)
        self.assertIn("news.find()", intent)
        self.assertIn("return readRememberedRssArticle", internet)
        self.assertIn("rss_last_articles_v3144", internet)
        self.assertIn("settings.setSecret(LAST_RSS_KEY", internet)

    def test_version_and_logs(self):
        gradle = (ROOT/"app/build.gradle").read_text()
        self.assertIn("versionCode 112", gradle)
        self.assertIn("versionName '3.14.18'", gradle)
        helper = source("JarvisLocationReply.java")
        self.assertIn('DiagnosticLog.record(ctx, "GPS_FIX_REPORTED")', helper)
        self.assertIn('DiagnosticLog.record(ctx, "GPS_PERMISSION_MISSING")', helper)
        self.assertIn("NEARBY_REMOTE_FAILED", source("NearbyPlaceSearch.java"))

if __name__ == "__main__":
    unittest.main()
