"""3.14.31 — synthetic weather-name ASR regression.
No actual user transcript, microphone recording, or personal history in GitHub.
"""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/fr/friday/assistant"

class VitryWeatherVoiceRegression(unittest.TestCase):
    def test_jvm_phonetic_and_context_validation(self):
        if not shutil.which("java") or not shutil.which("javac"):
            self.skipTest("JDK unavailable")
        smoke=r"""
import fr.friday.assistant.WeatherQueryPolicy;
import fr.friday.assistant.WeatherCitySpeechPolicy;
import fr.friday.assistant.WeatherFollowUpPolicy;

public class VitryWeatherSmoke {
 static void eq(String a, String b, String problem) {
  if (!java.util.Objects.equals(a,b))
    throw new AssertionError(problem + ": " + a + " expected " + b);
 }
 static void yes(boolean value,String problem){
  if(!value)throw new AssertionError(problem);
 }
 public static void main(String[]args) {
  eq(WeatherQueryPolicy.city("météo pour Vitry dur Seine"),
     "vitry-sur-seine, france", "ASR dur Seine explicitly mentions Vitry");
  eq(WeatherQueryPolicy.city("météo pour Vitry sur scène"),
     "vitry-sur-seine, france", "Recognize explicit Vitry + sur scène");
  eq(WeatherQueryPolicy.city("météo à Vitri sur senne, France"),
     "vitry-sur-seine, france", "Phonetic variant + country");
  eq(WeatherQueryPolicy.city("météo de Vitry-sur-Seine en France"),
     "vitry-sur-seine, france", "Written city unchanged");
  eq(WeatherQueryPolicy.city("météo à Joinville le Pont en France"),
     "joinville le pont, france", "Retain prior city fix");
  yes(WeatherCitySpeechPolicy.canonicalCity("sur scène") == null,
     "Cannot guess Vitry when city was not pronounced");
  yes(WeatherCitySpeechPolicy.canonicalCity("scène France") == null,
     "No inferred city from scene France");
  yes(WeatherCitySpeechPolicy.canonicalCity("une scène de théâtre") == null,
     "Unrelated theatrical scene is not a town");
  yes(WeatherCitySpeechPolicy.missingCityInAudio("scène france"),
     "Ambiguous French country fragment recognized as ASR uncertainty");
  yes(WeatherCitySpeechPolicy.missingCityInAudio("ii sur scène"),
     "Short false Vosk city recognized as uncertain");
  yes(WeatherCitySpeechPolicy.missingCityInAudio("sur scène"),
     "Orphaned place fragment recognized as uncertain");
  yes(!WeatherCitySpeechPolicy.missingCityInAudio("Vitry sur scène"),
     "Explicit named town can be resolved");
  String prompt="Pour quelle ville souhaitez-vous la météo ? Dites par exemple « météo pour Paris, France ».";
  yes(WeatherCitySpeechPolicy.awaitingWeatherCity(prompt),
     "Original prompt keeps weather followup active");
  yes(WeatherCitySpeechPolicy.awaitingWeatherCity(
    "Aucune commune correspondant à « scene, france » n'a été trouvée par le géocodeur."),
    "Failed geocoder must keep weather dialogue");
  yes(WeatherCitySpeechPolicy.awaitingWeatherCity(
    "Vous indiquez « ii sur scène ». Confirmez-vous que vous souhaitez la météo de cette commune ?"),
    "Confirmation of vague voice input stays in weather");
  yes(WeatherCitySpeechPolicy.awaitingWeatherCity(
    WeatherCitySpeechPolicy.askCityAgain()),
    "Another uncertain city reply is still a weather follow-up");
  yes(!WeatherCitySpeechPolicy.awaitingWeatherCity(
    "Le théâtre de la scène française est célèbre."),
    "Ordinary assistant reply cannot prime a weather location");
  eq(WeatherCitySpeechPolicy.explicitWeatherQuestion(
     "Vitry dur scène", "service de la météo pour demain"),
     "météo demain pour vitry-sur-seine, france",
     "Keep previous requested tomorrow during spoken city correction");
  eq(WeatherCitySpeechPolicy.explicitWeatherQuestion(
     "Vitry-sur-Seine ce soir", "météo pour demain"),
     "météo ce soir pour vitry-sur-seine, france",
     "Explicit changed time wins over old weather question");
  yes(WeatherCitySpeechPolicy.explicitWeatherQuestion(
     "sur scène", "météo demain") == null,
     "No fabricated geocode for orphaned fragment");
  eq(WeatherFollowUpPolicy.enrich("Vitry-sur-Seine ce soir",prompt),
     "météo ce soir pour vitry-sur-seine",
     "Legacy spoken-city path remains intact");
  System.out.println("VitryWeatherSmoke OK");
 }
}
"""
        with tempfile.TemporaryDirectory() as tmp:
            f=Path(tmp)/"VitryWeatherSmoke.java"
            f.write_text(smoke,encoding="utf8")
            p=subprocess.run(["javac","-encoding","UTF-8","-d",tmp,
                              str(JAVA/"WeatherQueryPolicy.java"),
                              str(JAVA/"WeatherCitySpeechPolicy.java"),
                              str(JAVA/"WeatherFollowUpPolicy.java"),str(f)],
                              capture_output=True,text=True)
            self.assertEqual(0,p.returncode,p.stderr)
            q=subprocess.run(["java","-cp",tmp,"VitryWeatherSmoke"],
                             capture_output=True,text=True)
            self.assertEqual(0,q.returncode,q.stderr+q.stdout)

    def test_routing_and_voice_next_turn(self):
        weather=(JAVA/"JarvisWeather.java").read_text(encoding="utf8")
        tools=(JAVA/"JarvisAndroidTools.java").read_text(encoding="utf8")
        service=(JAVA/"FridayService.java").read_text(encoding="utf8")
        self.assertIn("WeatherCitySpeechPolicy.awaitingWeatherCity(lastAssistant)",weather)
        self.assertIn("WeatherCitySpeechPolicy.missingCityInAudio(question)",weather)
        self.assertIn("WeatherCitySpeechPolicy.explicitWeatherQuestion(",weather)
        self.assertIn("previousWeatherRequest",weather)
        self.assertIn('DiagnosticLog.record(context, "WEATHER_CITY_ASR_CLARIFY")',weather)
        self.assertIn("WeatherFollowUpPolicy.enrich(question, lastAssistant)",weather)
        self.assertLess(weather.index("WeatherCitySpeechPolicy.missingCityInAudio(question)"),
                        weather.index("WeatherFollowUpPolicy.enrich(question, lastAssistant)"))
        self.assertLess(tools.index("JarvisWeather.followUp(context, settings, utterance)"),
                        tools.index("JarvisToolPolicy.classify(utterance)"))
        self.assertIn("WeatherCitySpeechPolicy.awaitingWeatherCity(answer)",service)
        self.assertIn("WeatherFollowUpPolicy.clearSpokenCityReply(heard)",service)
        self.assertIn("lastReplyAt + 50_000L",service)
        self.assertNotIn("HttpURLConnection",(JAVA/"WeatherCitySpeechPolicy.java").read_text())
        self.assertNotIn("getLastKnownLocation",(JAVA/"WeatherCitySpeechPolicy.java").read_text())

    def test_debug_metadata(self):
        gradle=(ROOT/"app/build.gradle").read_text()
        self.assertIn("versionCode 112",gradle)
        self.assertIn("versionName '3.14.18'",gradle)
        self.assertIn("applicationIdSuffix '.v31421matrix'",gradle)
        self.assertIn("output.versionCode.set(125)",gradle)
        self.assertIn('output.versionName.set("3.14.31-weather-voice-city-preview")',gradle)

if __name__=="__main__":
    unittest.main()
