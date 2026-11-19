"""FRIDAY v3.13.3: robust phone mic, adaptive pauses, dated public-Web research."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/fr/friday/assistant"
VOICE=(JAVA/"FridayService.java").read_text(encoding="utf-8")
WEB=(JAVA/"JarvisInternet.java").read_text(encoding="utf-8")
UI=(JAVA/"MainActivity.java").read_text(encoding="utf-8")
FRESH=(JAVA/"RssFreshness.java").read_text(encoding="utf-8")
GRADLE=(ROOT/"app/build.gradle").read_text(encoding="utf-8")

class MicAndFreshNews(unittest.TestCase):
    def test_mic_noise_reduction_is_independent_from_stop_feature(self):
        start=VOICE.index("echoProtection = false;")
        end=VOICE.index("final java.util.concurrent.atomic.AtomicBoolean silencedByPolicy",start)
        code=VOICE[start:end]
        self.assertIn("NoiseSuppressor.isAvailable()",code)
        self.assertIn("AutomaticGainControl.isAvailable()",code)
        self.assertIn("if (bargeEnabled && AcousticEchoCanceler.isAvailable())",code)
        self.assertNotIn("if (bargeEnabled) {",code)
        self.assertIn("PhoneAudioRoute.preferBuiltInMicrophone(this, ar)",VOICE)

    def test_microphone_level_without_audio_storage(self):
        self.assertIn("MicSignalMeter.level(buffer, count)",VOICE)
        self.assertIn("public static String microphoneSignal()",VOICE)
        self.assertIn("FridayService.microphoneSignal()",UI)
        self.assertNotIn("BluetoothMicRouter",VOICE)

    def test_adaptive_vosk_pause_and_stop(self):
        timing=(JAVA/"VoiceUtteranceTiming.java").read_text(encoding="utf-8")
        self.assertIn("VoiceUtteranceTiming.timeoutMs(",VOICE)
        self.assertIn("interruptingResponse",timing)
        self.assertIn("base + 1000",timing)
        self.assertIn("Math.min(base, 1300)",timing)
        self.assertIn("recognizer.acceptWaveForm(buffer, count)",VOICE)

    def test_recent_news_requires_dated_rss_not_html_guesses(self):
        self.assertIn('RecentNewsPolicy.requestedHours(q)',WEB)
        self.assertIn("RssFreshness.withinHours(published, now, recentHours)",WEB)
        self.assertIn("RssFreshness.publishedMillis(xmlField(item, \"pubDate\"))",WEB)
        self.assertIn("https://news.google.com/rss?hl=fr&gl=FR&ceid=FR:fr",WEB)
        self.assertIn("parseRss(fetch(feed), hours, now)",WEB)
        self.assertIn("aucun article avec horodatage",WEB)
        self.assertIn("DateTimeFormatter.RFC_1123_DATE_TIME",FRESH)
        self.assertIn("horodatage RSS",WEB)

    def test_web_keeps_existing_security_and_local_only(self):
        self.assertIn("WebPageReader.checkedUri(url)",WEB)
        self.assertIn("setInstanceFollowRedirects(false)",WEB)
        self.assertIn("destination.getHost()",WEB)
        self.assertIn("MAX_SEARCH_BYTES = 200_000",WEB)
        self.assertIn("LocalJarvisEngine.answer(context, settings, prompt, cancellation)",WEB)
        self.assertNotIn("NativeAI.answer(",WEB)
        self.assertNotIn("PersonalMemory.",WEB)
        self.assertNotIn("FridayLocation.",WEB)

    def test_recent_news_diagnostic_available(self):
        self.assertIn("JarvisInternet.probeRecentNews()",UI)
        self.assertIn("Tester les actualités des 7 dernières heures",UI)
        self.assertIn("Horodatage RSS",WEB)
        self.assertIn("Aucune mémoire privée n'a été envoyée",WEB)

    def test_version(self):
        self.assertIn("versionCode 88",GRADLE)
        self.assertIn("versionName '3.13.3'",GRADLE)

if __name__=="__main__": unittest.main()
