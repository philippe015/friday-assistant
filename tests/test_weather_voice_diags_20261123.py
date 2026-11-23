"""Regression from private Android logs 17:28 and 18:03, 23 November 2026.

The private JSON reports are deliberately NOT copied to this public repository.
These checks compile and execute only privacy-safe, pure Java production logic.
"""
from pathlib import Path
import subprocess
import tempfile
import shutil
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"


class WeatherVoiceDiagnostics(unittest.TestCase):
    def test_city_reply_plus_evening_context(self):
        if not shutil.which("javac") or not shutil.which("java"):
            self.skipTest("Java JDK unavailable")
        smoke = r'''
import fr.friday.assistant.*;
public class EveningWeatherSmoke {
  static void same(String received, String expected, String what) {
    if (!java.util.Objects.equals(received, expected))
      throw new AssertionError(what+": "+received+" expected "+expected);
  }
  static void yes(boolean condition, String what) {
    if (!condition) throw new AssertionError(what);
  }
  public static void main(String[] args) {
    String prompt = "Pour quelle ville souhaitez-vous la météo ? Dites par exemple météo pour Paris, France.";
    same(WeatherFollowUpPolicy.enrich("vitry sur seine ce soir", prompt),
      "météo ce soir pour vitry sur seine", "Voice reply city and period");
    same(WeatherFollowUpPolicy.enrich("pour Vitry-sur-Seine ce soir", prompt),
      "météo ce soir pour vitry-sur-seine", "Hyphenated city");
    same(WeatherFollowUpPolicy.enrich("Vitry-sur-Seine, France ce soir", prompt),
      "météo ce soir pour vitry-sur-seine, france", "Explicit country");
    same(WeatherFollowUpPolicy.enrich("vitry sur seine", prompt),
      "météo pour vitry sur seine", "Ordinary city");
    same(WeatherFollowUpPolicy.enrich("météo pour Paris, France", prompt),
      null, "Standalone question retains normal route");
    same(WeatherQueryPolicy.city("météo ce soir pour vitry sur seine"),
      "vitry sur seine", "Full weather tool can parse enriched question");
    yes(WeatherFollowUpPolicy.uncertainCityAfterWeatherPrompt(
      "c'est sur scène france", prompt), "ASR city correction stays local");
    yes(!WeatherFollowUpPolicy.uncertainCityAfterWeatherPrompt(
      "Quel est le climat ?", prompt), "New ordinary question is not blocked");
    yes(!WeatherFollowUpPolicy.uncertainCityAfterWeatherPrompt(
      "c'est sur scène france", "Je suis JARVIS"), "Only following weather prompt");

    same(DiagnosticLogPolicy.safeCode("asr_diag",
        "Question transcrite par Vosk mais non envoyée : dites Jarvis au début."),
        "VOSK_NO_WAKE", "Real FridayService log wording must be recognized");
    same(DiagnosticLogPolicy.safeCode("asr_diag",
        "Question transcrite par Vosk mais pas envoyée"),
        "VOSK_NO_WAKE", "Historic wording compatible");
    same(DiagnosticLogPolicy.safeCode("asr_diag",
        "Question finalisée par Vosk : exemple privé"),
        "VOSK_FINAL", "Vosk final still tracked");
    same(DiagnosticLogPolicy.safeCode("user",
        "Mon texte confidentiel et ma position"),
        "", "Never store speech in technical log");
    System.out.println("EveningWeatherSmoke OK");
  }
}
'''
        with tempfile.TemporaryDirectory() as folder:
            tmp = Path(folder)
            (tmp / "EveningWeatherSmoke.java").write_text(smoke, encoding="utf-8")
            classes = [
                "WeatherFollowUpPolicy.java",
                "WeatherQueryPolicy.java",
                "DiagnosticLogPolicy.java",
            ]
            compile_cmd = [
                "javac", "-encoding", "UTF-8", "-d", folder,
                *[str(JAVA / name) for name in classes],
                str(tmp / "EveningWeatherSmoke.java"),
            ]
            result = subprocess.run(compile_cmd, capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr + result.stdout)
            result = subprocess.run(["java", "-cp", folder, "EveningWeatherSmoke"],
                                    capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr + result.stdout)

    def test_android_followup_is_called_before_web(self):
        weather = (JAVA / "JarvisWeather.java").read_text(encoding="utf-8")
        tools = (JAVA / "JarvisAndroidTools.java").read_text(encoding="utf-8")
        voice = (JAVA / "FridayService.java").read_text(encoding="utf-8")
        self.assertIn("WeatherFollowUpPolicy.enrich(question, lastAssistant)", weather)
        self.assertIn("WeatherFollowUpPolicy.uncertainCityAfterWeatherPrompt(", weather)
        self.assertIn("JarvisWeather.followUp(context, settings, utterance)", tools)
        self.assertLess(tools.index("JarvisWeather.followUp("),
                        tools.index("NativeAI.localAnswer(cleaned)"))
        self.assertIn("JarvisAndroidTools.answer(this, settings, question)", voice)

if __name__ == "__main__":
    unittest.main()
