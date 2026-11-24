"""Reproduction des demandes météo du rapport utilisateur v3.14.18."""
import subprocess
from pathlib import Path
import tempfile
import unittest
import shutil

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/fr/friday/assistant"

class WeatherLogV31418(unittest.TestCase):
    def test_weather_formulations_are_parsed_without_gps(self):
        if not shutil.which("javac") or not shutil.which("java"):
            self.skipTest("JDK non disponible")
        sample=r"""
import fr.friday.assistant.WeatherQueryPolicy;
public final class WeatherLogSmoke {
  private static void expect(String request, String wanted) {
    String result = WeatherQueryPolicy.city(request);
    if (!java.util.Objects.equals(wanted, result))
      throw new AssertionError(request+" => "+result+" (attendu "+wanted+")");
  }
  public static void main(String[] args) {
    expect("Meteo Joinville-le-Pont 94", "joinville-le-pont");
    expect("Meteo  pour joinville-le-Pont 94", "joinville-le-pont");
    expect("Météo pour demain à Joinville-le-Pont 94", "joinville-le-pont");
    expect("Météo pour Joinville le Pont 94340", "joinville le pont");
    expect("Météo pour Paris, France", "paris, france");
    expect("Quelle sera la météo pour demain", "");
    expect("a ma en quelle sera la météo pour demain", "");
    expect("Météo pour demain", "");
    expect("Météo pour aujourd'hui", "");
    expect("météo pour moi", "");
    expect("Quel temps fait-il à Lyon demain ?", "lyon");
    expect("Explique la physique quantique", null);
    if (!WeatherQueryPolicy.department94("Meteo Joinville-le-Pont 94"))
      throw new AssertionError("Département 94 ignoré");
    if (!WeatherQueryPolicy.department94("Météo Joinville-le-Pont 94340"))
      throw new AssertionError("Code postal 94340 ignoré");
    if (WeatherQueryPolicy.department94("Météo pour Paris, France"))
      throw new AssertionError("Département 94 fabriqué");
    System.out.println("WeatherLogSmoke OK");
  }
}
"""
        with tempfile.TemporaryDirectory() as tmp:
            p=Path(tmp)
            (p/"WeatherLogSmoke.java").write_text(sample,encoding="utf8")
            subprocess.run(["javac","-encoding","UTF-8","-d",tmp,
                 str(JAVA/"WeatherQueryPolicy.java"),str(p/"WeatherLogSmoke.java")],
                 check=True, capture_output=True,text=True)
            result = subprocess.run(["java","-cp",tmp,"WeatherLogSmoke"],
                 check=False,capture_output=True,text=True)
            self.assertEqual(result.returncode, 0, result.stderr + result.stdout)

    def test_geocoder_never_blindly_selects_a_different_town(self):
        java=(JAVA/"JarvisWeather.java").read_text()
        self.assertIn("WeatherQueryPolicy.department94(question)",java)
        self.assertIn('"france".equals(countryHint)', java)
        self.assertIn('"&countryCode=" + countryCode', java)
        self.assertIn('countryCode.equalsIgnoreCase(candidate.optString("country_code", ""))', java)
        self.assertIn('normalizedCity(candidate.optString("name", ""))',java)
        self.assertIn('normalizedCity(candidate.optString("admin2", ""))',java)
        self.assertIn('equals("valdemarne")',java)
        self.assertIn("Je ne vais pas choisir une autre ville.",java)
        self.assertNotIn('JSONObject place = results.getJSONObject(0);',java)

if __name__=="__main__": unittest.main()
