"""FRIDAY 3.14.30: public synthetic regression cases based on observed categories.
No personal conversations, audio or diagnostics are uploaded.
"""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT=Path(__file__).resolve().parents[1]
J=ROOT/"app/src/main/java/fr/friday/assistant"

class VoiceIntelligenceStabilityTests(unittest.TestCase):
    def test_jvm_natural_tv_and_weather_cleanup(self):
        if not shutil.which("javac") or not shutil.which("java"):
            self.skipTest("JDK absent")
        java=r"""
import fr.friday.assistant.TvGuidePolicy;
import fr.friday.assistant.WeatherQueryPolicy;
public class VoiceIntelligenceSmoke {
 static void yes(boolean value, String why) {
  if (!value) throw new AssertionError(why);
 }
 public static void main(String[] args) {
  yes(TvGuidePolicy.isRequest(
    "Un bruit pour ce soir, qu'est-ce qui est à la télévision ?"),
    "TV intent works after noisy ASR preamble");
  yes(TvGuidePolicy.isRequest("Qu'est-ce qu'il y a à la télévision ce soir ?"),
    "Normal conversational evening TV question");
  yes(TvGuidePolicy.isRequest("Qu'est-ce qui passe à la télé ce soir ?"),
    "Spoken television question");
  yes(TvGuidePolicy.isRequest("C'est quoi à la TV ce soir ?"),
    "Short TV question");
  yes(TvGuidePolicy.isRequest("Je veux le programme de télévision de ce soir"),
    "Original explicit programme request still works");
  yes(!TvGuidePolicy.isRequest("La télévision a-t-elle un impact sur la santé ?"),
    "Not all TV discussion requests need the live guide");
  yes(!TvGuidePolicy.isRequest("Explique pourquoi le cinéma a fermé ce soir"),
    "Avoid false current programming match");
  yes(!TvGuidePolicy.isRequest("Que se passe-t-il à Paris ce soir ?"),
    "No TV topic");
  yes(!TvGuidePolicy.isRequest("Une télévision à vendre"),
    "No question about schedules");
  yes("joinville le pont, france".equals(WeatherQueryPolicy.city(
    "Service donne-moi la météo sur Joinville le Pont en France")),
    "ASR town with en France should not include en in geocoder");
  yes("paris, france".equals(WeatherQueryPolicy.city("météo à Paris en France")),
    "Country preposition removed");
  yes("paris, france".equals(WeatherQueryPolicy.city("météo à Paris, France")),
    "Explicit comma preserved");
  yes("paris".equals(WeatherQueryPolicy.city("météo à Paris")),
    "No country still accepted");
  yes(WeatherQueryPolicy.city("Explique le climat de Mars")==null,
    "Unrelated question must not geocode");
  System.out.println("VoiceIntelligenceSmoke OK");
 }
}
"""
        with tempfile.TemporaryDirectory() as d:
            f=Path(d)/"VoiceIntelligenceSmoke.java"
            f.write_text(java,encoding="utf-8")
            c=subprocess.run(["javac","-encoding","UTF-8","-d",d,
                str(J/"TvGuidePolicy.java"),str(J/"WeatherQueryPolicy.java"),str(f)],
                capture_output=True,text=True)
            self.assertEqual(0,c.returncode,c.stderr)
            x=subprocess.run(["java","-cp",d,"VoiceIntelligenceSmoke"],
                capture_output=True,text=True)
            self.assertEqual(0,x.returncode,x.stdout+x.stderr)

    def test_bounded_cancelable_litert_and_micro_recovery(self):
        litert=(J/"LiteRtJarvisEngine.kt").read_text()
        voice=(J/"FridayService.java").read_text()
        self.assertIn("val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(18)",litert)
        self.assertIn("lock.tryLock(750, TimeUnit.MILLISECONDS)",litert)
        self.assertIn("cancellation?.check()",litert)
        self.assertIn("finally {\n            lock.unlock()",litert)
        self.assertIn('DiagnosticLog.record(this, "MIC_POLICY_RESTORED")',voice)
        self.assertIn('event("mic_conflict"',voice)
        self.assertIn('silencedByPolicy.get()',voice)
        self.assertIn('voiceState("Micro restauré : dites Jarvis")',voice)

    def test_full_news_text_short_spoken_results(self):
        internet=(J/"JarvisInternet.java").read_text()
        self.assertIn("private static String briefRecentHeadlines(",internet)
        self.assertIn("if (said > 2) break;",internet)
        self.assertIn("RssFreshness.withinHours(source.publishedAt, now, hours)",internet)
        self.assertIn('briefRecentHeadlines(sources, newsHours, System.currentTimeMillis())',internet)
        self.assertIn('return new Result(spoken + "\\n\\n" + references,',internet)
        self.assertIn('true, references.toString());',internet)
        self.assertNotIn("PrivateConversationHistory.read(",internet.split(
            "private static String briefRecentHeadlines(",1)[1].split(
            "public static boolean shouldResearch(",1)[0])

    def test_release_metadata(self):
        gradle=(ROOT/"app/build.gradle").read_text()
        self.assertIn("versionCode 112",gradle)
        self.assertIn("versionName '3.14.18'",gradle)
        self.assertIn("applicationIdSuffix '.v31421matrix'",gradle)
        self.assertIn("output.versionCode.set(125)",gradle)
        self.assertIn('output.versionName.set("3.14.31-weather-voice-city-preview")',gradle)

if __name__=="__main__":
    unittest.main()
