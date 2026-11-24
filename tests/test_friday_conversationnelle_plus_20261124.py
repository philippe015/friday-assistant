"""Regression checks for FRIDAY 3.14.25 Conversationnelle+.

Fixtures are synthetic and contain no user's private report, GPS, or history.
"""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/fr/friday/assistant"

class ConversationnellePlus(unittest.TestCase):
    def test_real_world_voice_intents_in_jvm(self):
        if not shutil.which("javac") or not shutil.which("java"):
            self.skipTest("javac/java unavailable")
        smoke=r"""
import fr.friday.assistant.WeatherQueryPolicy;
import fr.friday.assistant.WeatherFollowUpPolicy;
import fr.friday.assistant.RssArticleIntent;
public final class ConversationSmoke {
 static void yes(boolean value, String reason) {
  if (!value) throw new AssertionError(reason);
 }
 public static void main(String[] args) {
  yes("".equals(WeatherQueryPolicy.city("donne-moi la météo le temps que fait-il")),
      "Do not geocode phrase « le temps que fait-il » as a city");
  yes("Val-de-Marne".equals(WeatherQueryPolicy.region("météo dans le Val-de-Marne")),
      "Val-de-Marne is a department, not a city");
  yes("Île-de-France".equals(WeatherQueryPolicy.region("météo sur l'Île-de-France")),
      "Île-de-France is a region, not a city");
  yes("Île-de-France".equals(WeatherQueryPolicy.region(
      "tout à fait et la métaux sur l'Île-de-France")),
      "ASR's « la métaux sur » can be understood with explicit place");
  yes(WeatherQueryPolicy.region("météo Vitry-sur-Seine") == null,
      "Vitry-sur-Seine is still a city");
  yes("vitry-sur-seine".equals(WeatherQueryPolicy.city("météo à Vitry-sur-Seine")),
      "Explicitly named cities remain supported");
  yes("météo pour vitry-sur-seine".equals(WeatherFollowUpPolicy.enrich(
      "doyle à Vitry-sur-Seine",
      "Pour quelle ville souhaitez-vous la météo ? Dites par exemple « météo pour Paris, France ».")),
      "Strong location following a request for city must not trigger unrelated Web search");
  yes(!RssArticleIntent.isRequest("lis les articles en entier"),
      "Never choose a numbered RSS article without a number");
  yes(RssArticleIntent.isPluralBulletinFollowUp("lis les articles en entier"),
      "Plural follow-up should reuse last bulletin");
  yes(RssArticleIntent.isPluralBulletinFollowUp("trouve-moi des articles en entier"),
      "The user's generic full-article request is a bulletin follow-up");
  yes(!RssArticleIntent.isPluralBulletinFollowUp("lis l'article 2 en entier"),
      "Existing precise article-reading tool must take priority");
  yes(!RssArticleIntent.isPluralBulletinFollowUp("explique le code pénal"),
      "Legal discussion must never be an RSS follow-up");
  System.out.println("ConversationSmoke OK");
 }
}
"""
        with tempfile.TemporaryDirectory() as temp:
            test=Path(temp)/"ConversationSmoke.java"
            test.write_text(smoke, encoding="utf-8")
            proc=subprocess.run([
                "javac","-encoding","UTF-8","-d",temp,
                *(str(JAVA/x) for x in [
                    "WeatherQueryPolicy.java","WeatherFollowUpPolicy.java",
                    "RssArticleIntent.java","VoiceText.java"
                ]),str(test)],capture_output=True,text=True)
            self.assertEqual(0,proc.returncode,proc.stderr)
            proc=subprocess.run(["java","-cp",temp,"ConversationSmoke"],
                                capture_output=True,text=True)
            self.assertEqual(0,proc.returncode,proc.stderr+proc.stdout)

    def test_knowledge_questions_are_local_and_no_generic_web_search(self):
        web=(JAVA/"WebSearch.java").read_text()
        core=(JAVA/"JarvisInternet.java").read_text()
        rss=(JAVA/"RssArticleIntent.java").read_text()
        self.assertIn('(?:coder|code|programmer|developper)',web)
        self.assertIn("je peux vous aider à concevoir, écrire, expliquer",web)
        self.assertIn("if (assistantAnswer(question, true) != null) return false",web)
        self.assertIn("RssArticleIntent.isPluralBulletinFollowUp(question)",core)
        self.assertIn("listRememberedArticles(settings)",core)
        self.assertIn("settings.secret(LAST_RSS_KEY)",core)
        self.assertIn("Je n'ai aucun article RSS récent mémorisé",core)
        self.assertIn("sans rapport",core)
        self.assertIn("isPluralBulletinFollowUp",rss)
        self.assertNotIn("privateDialogue",core.split(
            "private static Result listRememberedArticles(",1)[1].split(
            "private static Result readRememberedRssArticle(",1)[0])

    def test_weather_uses_only_qualified_geocoding(self):
        weather=(JAVA/"JarvisWeather.java").read_text()
        region=(JAVA/"WeatherQueryPolicy.java").read_text()
        self.assertIn("WeatherQueryPolicy.region(question)",weather)
        self.assertIn('String representative = "Val-de-Marne".equals(region)',weather)
        self.assertIn('?"Créteil"',weather.replace(" ", ""))
        self.assertIn("comme point de référence uniquement",weather)
        self.assertIn("INVALID_CITY.matcher(name).matches()",region)

    def test_release_configuration_remains_stable(self):
        gradle=(ROOT/"app/build.gradle").read_text()
        self.assertIn("versionCode 112",gradle)
        self.assertIn("versionName '3.14.18'",gradle)
        self.assertIn("applicationIdSuffix '.v31421matrix'",gradle)
        self.assertIn("output.versionCode.set(119)",gradle)
        self.assertIn('output.versionName.set("3.14.25-conversation-plus-preview")',gradle)

if __name__=="__main__":
    unittest.main()
