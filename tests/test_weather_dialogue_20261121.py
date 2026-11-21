"""JARVIS: invite user for city before querying a meteorological source."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"
VOICE = (JAVA / "FridayService.java").read_text(encoding="utf-8")
MAIN = (JAVA / "MainActivity.java").read_text(encoding="utf-8")
DIALOGUE = (JAVA / "WeatherDialogue.java").read_text(encoding="utf-8")
FORECAST = (JAVA / "WeatherForecast.java").read_text(encoding="utf-8")
REQUEST = (JAVA / "WeatherRequestPolicy.java").read_text(encoding="utf-8")
MANIFEST = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
SCRIPT = (ROOT / "scripts/tester_sources.sh").read_text(encoding="utf-8")


class WeatherDialogueAndroid(unittest.TestCase):
    def test_asks_city_and_uses_same_session_for_followup(self):
        self.assertIn("Pour quelle ville souhaitez-vous la météo", DIALOGUE)
        self.assertIn("jarvis_weather_city_until", DIALOGUE)
        self.assertIn("VALID_FOR_MS = 120_000L", DIALOGUE)
        self.assertIn("WeatherRequestPolicy.cityReply(heard)", DIALOGUE)
        self.assertIn("WeatherDialogue.cityFollowup(settings, heard)", VOICE)
        self.assertIn("WeatherRequestPolicy.isWeatherRequest(heard)", VOICE)
        self.assertIn("weatherAnswer.speech", VOICE)
        self.assertIn("WeatherDialogue.answer(", MAIN)
        self.assertIn("playWebVoice(spokenResult)", MAIN)

    def test_authorized_live_source_with_precise_labeling(self):
        self.assertIn("https://geocoding-api.open-meteo.com/v1/search", FORECAST)
        self.assertIn("https://api.open-meteo.com/v1/forecast", FORECAST)
        self.assertIn("forecast_days=3&timezone=auto", FORECAST)
        self.assertIn("precipitation_probability_max", FORECAST)
        self.assertIn("Température estimée actuellement", FORECAST)
        self.assertIn("pas d'un relevé garanti", FORECAST)
        self.assertIn("Source météorologique : Open-Meteo", FORECAST)
        self.assertIn('connection.setInstanceFollowRedirects(false)', FORECAST)
        self.assertIn("connection.setConnectTimeout(5_000)", FORECAST)
        self.assertIn("connection.setReadTimeout(7_000)", FORECAST)
        self.assertIn("cancellation.check()", FORECAST)
        self.assertIn("android.permission.INTERNET", MANIFEST)

    def test_privacy_and_no_invented_weather(self):
        self.assertNotIn("FridayLocation.", DIALOGUE + FORECAST)
        self.assertNotIn("PersonalMemory.", DIALOGUE + FORECAST)
        self.assertNotIn("PrivateConversationHistory.", DIALOGUE + FORECAST)
        self.assertNotIn("LocalJarvisEngine.", DIALOGUE + FORECAST)
        self.assertIn("JarvisInternet.enabled(prefs)", DIALOGUE)
        self.assertIn("Je ne vais pas inventer des températures", DIALOGUE)
        self.assertIn("Aucune coordonnée GPS", FORECAST)
        self.assertIn("WeatherRequestPolicyTest.java", SCRIPT)
        self.assertIn("WeatherPlacePolicyTest.java", SCRIPT)
        self.assertIn("if (requestedCountry.isEmpty())", FORECAST)
        self.assertIn("WeatherPlacePolicy.sameCity(", FORECAST)
        self.assertIn("WeatherPlacePolicy.sameCountry(", FORECAST)
        self.assertIn("throw new NeedCountryException(", FORECAST)
        self.assertIn("catch (WeatherForecast.NeedCountryException ambiguous)", DIALOGUE)
        self.assertIn("PENDING_COUNTRY", DIALOGUE)
        self.assertIn("PENDING_CITY", DIALOGUE)

    def test_existing_audio_stack_retained(self):
        self.assertIn("recognizer.acceptWaveForm(buffer, count)", VOICE)
        self.assertIn("TtsEchoGate.suppressGeneralAsr(", VOICE)
        self.assertIn("BargeInPolicy.isStopListening(heard)", VOICE)
        self.assertIn("voice_state", VOICE)
        self.assertIn("public static boolean isWeatherRequest(String question)", REQUEST)


if __name__ == "__main__":
    unittest.main()
