"""FRIDAY 3.14.29 — short TV voice response and safe channel follow-ups.
All JVM examples are synthetic public TV schedule lines, not private logs.
"""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
J = ROOT / "app/src/main/java/fr/friday/assistant"

class VoiceResponseOptimizerTests(unittest.TestCase):
    def test_jvm_verified_tv_and_tts(self):
        if not shutil.which("javac") or not shutil.which("java"):
            self.skipTest("JDK unavailable")
        code = r"""
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import fr.friday.assistant.TvGuidePolicy;
import fr.friday.assistant.VoiceInputQualityPolicy;
public class VoiceOptimizerSmoke {
 static void ok(boolean v, String msg) { if (!v) throw new AssertionError(msg); }
 public static void main(String[] args) {
  long now=LocalDate.of(2026,11,24).atStartOfDay(ZoneId.of("Europe/Paris"))
      .toInstant().toEpochMilli();
  String guide="Programme TV mardi 24 novembre de 20h à 22h "
   +"N°1 TF1 Canap 92 21h10 Canap 92 Évènement TV "
   +"N°2 France 2 Le plus beau pays 21h10 Le plus beau pays Documentaire "
   +"N°3 France 3 Alex Hugo 21h10 Alex Hugo Série TV "
   +"N°4 France 4 Les choses qu&#039;on dit, les choses qu&#039;on fait "
   +"21h00 Les choses qu&#039;on dit, les choses qu&#039;on fait Cinéma "
   +"N°5 France 5 L&#039;affaire 21h05 L&#039;affaire Culture Infos "
   +"N°6 M6 Quel âge 21h10 Quel âge Divertissement ";
  List<TvGuidePolicy.Entry> entries=TvGuidePolicy.extract(guide,false,now);
  ok(entries.size()==6,"Exactly six visible evening schedule entries");
  ok(entries.get(3).title.contains("qu'on dit"),"Numeric &#039; apostrophe decoded");
  ok(entries.get(4).title.startsWith("L'affaire"),"Another numeric title decoded");
  String full=TvGuidePolicy.describe(guide,"programme TV de ce soir",now);
  String speech=TvGuidePolicy.shortSpeech(guide,"programme TV de ce soir",now);
  ok(full.contains("France 4")&&full.contains("M6"),"Full text still has all six");
  ok(!full.contains("&#039;"),"No TTS HTML artifact in full answer");
  ok(speech.contains("TF1")&&speech.contains("France 2"),"First 2 verified items spoken");
  ok(!speech.contains("France 4") || speech.contains("détaille France 4"),
     "France 4 only appears as a follow-up instruction");
  ok(!speech.contains("M6"),"No six-channel spoken bulletin");
  ok(speech.length()<380,"Spoken response should stay short");
  ok(!speech.contains("https://"),"Do not pronounce URLs");
  ok(TvGuidePolicy.followUpChannel("Détaille France 4").equals("france 4"),
     "Recognize requested channel for continuation");
  ok(TvGuidePolicy.followUpChannel("Et sur France 4 ?").equals("france 4"),
     "Voice follow-up with ellipsis");
  ok(TvGuidePolicy.followUpChannel("Quel temps fait-il à France 4 ?").isEmpty(),
     "No accidental channel matching");
  ok(TvGuidePolicy.sameChannel("france 4","France 4"),"Case-independent exact match");
  ok(!TvGuidePolicy.sameChannel("france 4","France 5"),"Never substitute channel");
  ok(TvGuidePolicy.decodeEntities("L&#039;affaire &eacute;trange &#x2019;").contains("L'affaire étrange"),
     "Decimal hexadecimal and named HTML entities");
  String previous="service donne-moi le programme de télévision de ce soir";
  ok(VoiceInputQualityPolicy.cleanDirectTvUtterance(previous)
          .equals("donne-moi le programme de télévision de ce soir"),
     "Only observed long TV prefix is cleaned");
  ok(VoiceInputQualityPolicy.cleanDirectTvUtterance("service donne-moi de l'aide")
          .equals("service donne-moi de l'aide"),
     "An unrelated service request is preserved");
  ok(VoiceInputQualityPolicy.cleanDirectTvUtterance("jarvis donne-moi la météo à Paris")
          .startsWith("jarvis"),"Wakeword identification unchanged");
  String outdated=guide.replace("24 novembre","23 novembre");
  ok(TvGuidePolicy.shortSpeech(outdated,"programme TV",now).contains("vérifiable"),
     "No text-to-speech claim from an older programme");
  System.out.println("VoiceOptimizerSmoke OK");
 }
}
"""
        with tempfile.TemporaryDirectory() as temp:
            p=Path(temp)/"VoiceOptimizerSmoke.java"
            p.write_text(code,encoding="utf-8")
            r=subprocess.run(["javac","-encoding","UTF-8","-d",temp,
                str(J/"TvGuidePolicy.java"),
                str(J/"VoiceInputQualityPolicy.java"),str(p)],
                capture_output=True,text=True)
            self.assertEqual(r.returncode,0,r.stderr)
            r=subprocess.run(["java","-cp",temp,"VoiceOptimizerSmoke"],
                capture_output=True,text=True)
            self.assertEqual(r.returncode,0,r.stdout+"\n"+r.stderr)

    def test_voice_and_text_routes_share_verified_cached_detail(self):
        internet=(J/"JarvisInternet.java").read_text(encoding="utf-8")
        service=(J/"FridayService.java").read_text(encoding="utf-8")
        policy=(J/"TvGuidePolicy.java").read_text(encoding="utf-8")
        self.assertIn('LAST_TV_KEY = "tv_last_verified_31429"',internet)
        self.assertIn("settings.setSecret(LAST_TV_KEY, cache.toString())",internet)
        self.assertIn("settings.secret(LAST_TV_KEY)",internet)
        self.assertIn("now - observed > 21_600_000L",internet)
        self.assertIn("TvGuidePolicy.expectedDate(now)",internet)
        self.assertIn("TvGuidePolicy.sameChannel(requested, channel)",internet)
        self.assertIn("new Result(response, speech, true, sources)",internet)
        self.assertIn("TvGuidePolicy.shortSpeech(guide.text, question, checkedAt)",internet)
        self.assertIn("return tvFollowUp(question, settings)",internet)
        self.assertIn("VoiceInputQualityPolicy.cleanDirectTvUtterance(heard)",service)
        self.assertIn("VOSK_TV_PREFIX_REPAIRED",service)
        self.assertNotIn("HttpURLConnection",policy)
        self.assertNotIn("PrivateConversationHistory",policy)
        self.assertNotIn("GPS",policy)
        self.assertNotIn("privateDialogue",internet.split("private static void rememberTvGuide",1)[1]
            .split("private static Result tvFollowUp",1)[0])
        self.assertNotIn("http://",policy)

    def test_debug_version_and_release_metadata(self):
        gradle=(ROOT/"app/build.gradle").read_text(encoding="utf-8")
        self.assertIn("versionCode 112",gradle)
        self.assertIn("versionName '3.14.18'",gradle)
        self.assertIn("applicationIdSuffix '.v31421matrix'",gradle)
        self.assertIn("output.versionCode.set(123)",gradle)
        self.assertIn('output.versionName.set("3.14.29-voice-response-optimizer-preview")',gradle)

if __name__=="__main__":
    unittest.main()
