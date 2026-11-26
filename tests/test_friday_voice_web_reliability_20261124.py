"""FRIDAY 3.14.28: Voice/Web Reliability regression suite.

Fixtures are synthetic; private conversation logs are NOT included.
"""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/fr/friday/assistant"

class VoiceWebReliabilityTests(unittest.TestCase):
    def test_voice_and_tv_guide_policies_on_jvm(self):
        if not shutil.which("javac") or not shutil.which("java"):
            self.skipTest("JDK not installed")
        sample=r"""
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import fr.friday.assistant.VoiceInputQualityPolicy;
import fr.friday.assistant.TvGuidePolicy;
public class VoiceWebSmoke {
  static void ok(boolean value,String reason){if(!value)throw new AssertionError(reason);}
  public static void main(String[] args) {
    String noise=("ça commence il paraît et ça arrive quand cela sera dans un contexte "
       +"par la suite avec beaucoup et nous allons partir tout de suite comme "
       +"les personnes dont les situations sont venues pendant toute la soirée ");
    String longNoise=(noise+noise+noise+noise).trim();
    ok(VoiceInputQualityPolicy.requestRepeat(longNoise),
       "Do not search the internet from very long unintelligible Vosk text");
    ok(!VoiceInputQualityPolicy.requestRepeat("Explique les fonctions de JARVIS."),
       "Ordinary voice questions stay allowed");
    ok(!VoiceInputQualityPolicy.requestRepeat("Donne-moi un résumé détaillé "+longNoise),
       "Explicit long dictation stays allowed");
    ok(!VoiceInputQualityPolicy.requestRepeat("Jarvis donne-moi un résumé détaillé "+longNoise),
       "Wakeword plus clear task stays allowed");
    ok(TvGuidePolicy.isRequest("sans donne-moi le programme de télé vision pour ce soir"),
       "Real TV utterance must take targeted route");
    ok(TvGuidePolicy.isRequest("quels films passent ce soir sur la télé ?"),
       "Movies tonight are TV intents");
    ok(TvGuidePolicy.isRequest("j'ai envie accède aux programmes de télévision pour connaître comme film ce soir"),
       "Recover noisy natural-language TV request");
    ok(!TvGuidePolicy.isRequest("Explique le film que j'ai vu hier"),
       "An unrelated film question isn't a current listing");
    ok(!TvGuidePolicy.isRequest("Quelle est la capitale du Portugal ?"),
       "Normal questions still reach local/appropriate Web engines");
    ok(TvGuidePolicy.asksOtherCountry("programme TV en Belgique ce soir"),
       "Don't give French TNT as the schedule of another country");
    long date=LocalDate.of(2026,11,24).atStartOfDay(ZoneId.of("Europe/Paris"))
         .toInstant().toEpochMilli();
    String guide="Programme TV Changer de date et d'heure mardi 24 novembre de 20h à 22h "
       +"Filtrer vos programmes N°1 TF1 Canap 92 Lire le résumé "
       +"21h10 Canap 92 Évènement TV Autre 50min Inédit "
       +"N°4 France 4 Les choses qu'on dit, les choses qu'on fait Lire le résumé "
       +"21h00 Les choses qu'on dit, les choses qu'on fait Coup de Télé-Loisirs Cinéma "
       +"2h Rediffusion N°5 France 5 Mécaniques du journalisme Lire le résumé "
       +"21h05 Mécaniques du journalisme Culture Infos 50min Inédit";
    ok(TvGuidePolicy.confirmsEveningDate(guide,date),"Verify explicit current guide date");
    List<TvGuidePolicy.Entry> movies=TvGuidePolicy.extract(guide,true,date);
    ok(movies.size()==1,"Return only identifiable movie entries");
    ok(movies.get(0).channel.equals("France 4"),"Correct channel");
    ok(movies.get(0).hour.equals("21h00"),"Correct observed hour");
    ok(movies.get(0).title.contains("Les choses qu'on dit"),"Correct visible title");
    ok(TvGuidePolicy.extract(guide,false,date).size()==3,"Full guide includes other genres");
    ok(TvGuidePolicy.extract(guide.replace("mardi 24 novembre de 20h à 22h",
       "lundi 23 novembre de 20h à 22h"),true,date).isEmpty(),
       "Never present stale schedule as tonight");
    ok(TvGuidePolicy.describe("Aucune liste", "films ce soir", date)
      .contains("Je ne vais pas inventer"),
      "No invented movies on missing schedule");
    ok(TvGuidePolicy.GUIDE_URL.equals("https://www.programme-tv.net/"),
       "Only known public guide is used");
    System.out.println("VoiceWebSmoke OK");
  }
}
"""
        with tempfile.TemporaryDirectory() as temp:
            src=Path(temp)/"VoiceWebSmoke.java"
            src.write_text(sample,encoding="utf-8")
            proc=subprocess.run(["javac","-encoding","UTF-8","-d",temp,
                str(JAVA/"VoiceInputQualityPolicy.java"),str(JAVA/"TvGuidePolicy.java"),
                str(src)],capture_output=True,text=True)
            self.assertEqual(0,proc.returncode,proc.stderr)
            proc=subprocess.run(["java","-cp",temp,"VoiceWebSmoke"],
                                capture_output=True,text=True)
            self.assertEqual(0,proc.returncode,proc.stderr+proc.stdout)

    def test_privacy_and_nonhallucination_integration(self):
        service=(JAVA/"FridayService.java").read_text(encoding="utf-8")
        internet=(JAVA/"JarvisInternet.java").read_text(encoding="utf-8")
        search=(JAVA/"WebSearch.java").read_text(encoding="utf-8")
        tv=(JAVA/"TvGuidePolicy.java").read_text(encoding="utf-8")
        reader=(JAVA/"WebPageReader.java").read_text(encoding="utf-8")
        self.assertIn("VoiceInputQualityPolicy.requestRepeat(phrase)",service)
        self.assertIn('VOSK_LONG_UNRELIABLE_REJECTED',service)
        self.assertIn('if (unclear || unreliableLongAsr)',service)
        self.assertIn("if (TvGuidePolicy.isRequest(question))",internet)
        self.assertIn("return tvSchedule(question, settings)",internet)
        self.assertIn("readLargeText(TvGuidePolicy.GUIDE_URL)",internet)
        self.assertIn("String response = TvGuidePolicy.describe(",internet)
        self.assertIn("public static Page readLargeText(",reader)
        self.assertIn("!internetEnabled && TvGuidePolicy.isRequest(question)",search)
        self.assertNotIn("HttpURLConnection",tv)
        self.assertNotIn("PrivateConversationHistory",tv)
        self.assertNotIn("GPS",tv)
        self.assertNotIn("OpenAI",tv)

    def test_debug_build_metadata_release_unchanged(self):
        gradle=(ROOT/"app/build.gradle").read_text()
        self.assertIn("versionCode 112",gradle)
        self.assertIn("versionName '3.14.18'",gradle)
        self.assertIn("applicationIdSuffix '.v31421matrix'",gradle)
        self.assertIn("output.versionCode.set(125)",gradle)
        self.assertIn('output.versionName.set("3.14.31-weather-voice-city-preview")',gradle)

if __name__=="__main__":
    unittest.main()
