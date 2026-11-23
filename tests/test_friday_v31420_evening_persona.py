"""FRIDAY 3.14.20 evolution: hourly forecast and reliable app persona.
The user's private conversation exports are not committed into this repository.
"""
from pathlib import Path
import subprocess
import tempfile
import shutil
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"


class Friday31420Evolution(unittest.TestCase):
    def test_pure_java_evening_and_identity(self):
        if not shutil.which("javac") or not shutil.which("java"):
            self.skipTest("Java JDK not installed")
        program = r'''
import fr.friday.assistant.*;

public class Friday31420Smoke {
  static void check(boolean ok, String message) {
    if (!ok) throw new AssertionError(message);
  }
  static void same(String value, String wanted, String what) {
    if (!java.util.Objects.equals(value, wanted))
      throw new AssertionError(what + " => " + value + "; wanted " + wanted);
  }
  public static void main(String[] args) {
    check(WeatherEveningPolicy.requested("météo Vitry-sur-Seine ce soir"),
          "Evening intent");
    check(WeatherEveningPolicy.requested("météo demain soir pour Paris"),
          "Tomorrow evening intent");
    check(!WeatherEveningPolicy.requested("météo Paris demain matin"),
          "Do not turn tomorrow morning into an evening");
    String[] stamps = {
      "2026-11-23T17:00", "2026-11-23T18:00", "2026-11-23T19:00",
      "2026-11-23T20:00", "2026-11-23T21:00", "2026-11-23T22:00",
      "2026-11-23T23:00", "2026-11-24T18:00", "2026-11-24T20:00",
      "2026-11-24T23:00"
    };
    int[] evening = WeatherEveningPolicy.indices(stamps, "2026-11-23", 18);
    check(evening.length == 6 && evening[0] == 1 && evening[5] == 6,
          "18–23 for this evening");
    int[] remainder = WeatherEveningPolicy.indices(stamps, "2026-11-23", 21);
    check(remainder.length == 3 && remainder[0] == 4,
          "No past-hour forecast on a late evening");
    check(WeatherEveningPolicy.indices(stamps, "2026-11-23", 24).length == 0,
          "Past evening must return no future forecast");
    check(WeatherEveningPolicy.indices(stamps, "2026-11-24", 18).length == 3,
          "Tomorrow evening in local timezone");
    same(AssistantResponseIdentityGuard.correct("raconte une blague",
       "Je suis Gemma 4. Je peux raconter une blague.", "F.R.I.D.A.Y."),
       "Je suis F.R.I.D.A.Y., votre assistant personnel. Je peux raconter une blague.",
       "Local model self-identification is corrected");
    same(AssistantResponseIdentityGuard.correct("Bonjour", "Je suis Mistral.",
       "JARVIS"), "Je suis JARVIS, votre assistant personnel.", "Custom persona");
    same(AssistantResponseIdentityGuard.correct("Quel modèle es-tu ?",
       "Je suis Gemma 4.", "F.R.I.D.A.Y."), "Je suis Gemma 4.",
       "Technical model disclosure remains honest");
    same(AssistantResponseIdentityGuard.correct("Explique Gemma 4",
       "Gemma 4 est un modèle.", "F.R.I.D.A.Y."), "Gemma 4 est un modèle.",
       "Do not rewrite model explanations");
    same(AssistantResponseIdentityGuard.correct("bonjour",
       "Selon l'article, « Je suis Gemma 4 ».", "F.R.I.D.A.Y."),
       "Selon l'article, « Je suis Gemma 4 ».",
       "Preserve quotations and article content");
    System.out.println("FRIDAY v31420 Java smoke PASS");
  }
}
'''
        with tempfile.TemporaryDirectory() as temp:
            wd = Path(temp)
            (wd / "Friday31420Smoke.java").write_text(program, encoding="utf-8")
            cmd = ["javac", "-encoding", "UTF-8", "-d", temp,
                   str(JAVA / "WeatherQueryPolicy.java"),
                   str(JAVA / "WeatherEveningPolicy.java"),
                   str(JAVA / "AssistantResponseIdentityGuard.java"),
                   str(wd / "Friday31420Smoke.java")]
            result = subprocess.run(cmd, capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr + result.stdout)
            result = subprocess.run(["java", "-cp", temp, "Friday31420Smoke"],
                                    capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr + result.stdout)

    def test_android_wiring_and_privacy(self):
        weather = (JAVA / "JarvisWeather.java").read_text(encoding="utf-8")
        inference = (JAVA / "LocalJarvisEngine.java").read_text(encoding="utf-8")
        self.assertIn("&hourly=temperature_2m,precipitation_probability,weather_code", weather)
        self.assertIn("WeatherEveningPolicy.indices(stamps, date, earliest)", weather)
        self.assertIn("java.time.ZonedDateTime.now(zone)", weather)
        self.assertIn("return eveningAnswer(name, country, forecast", weather)
        self.assertIn("AssistantResponseIdentityGuard.correct(", inference)
        self.assertGreaterEqual(inference.count("AssistantResponseIdentityGuard.correct("), 2)
        self.assertNotIn("privateDialogue", (JAVA/"WeatherEveningPolicy.java").read_text())
        self.assertNotIn("https://", (JAVA/"AssistantResponseIdentityGuard.java").read_text())

if __name__ == "__main__":
    unittest.main()
