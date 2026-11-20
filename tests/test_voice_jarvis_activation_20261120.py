"""FRIDAY : régression activation vocale Jarvis/Friday (aucun réseau ni son enregistré)."""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"
VOICE = (JAVA/"VoiceText.java").read_text(encoding="utf-8")
SERVICE = (JAVA/"FridayService.java").read_text(encoding="utf-8")
BARGE = (JAVA/"BargeInPolicy.java").read_text(encoding="utf-8")
UI = (JAVA/"MainActivity.java").read_text(encoding="utf-8")
WEB = (ROOT/"app/src/main/assets/index.html").read_text(encoding="utf-8")

class VoiceActivationReliability(unittest.TestCase):
    def test_local_jarvis_responds_to_two_wake_words(self):
        for name in ("friday", "fraday", "jarvis", "jarvise", "jervis"):
            self.assertIn(name, VOICE)
            self.assertIn(name, BARGE)
        self.assertIn("BargeInPolicy.startsWithWake(heard) && RustWake.detect(heard)", SERVICE)

    def test_first_question_after_manual_activation_needs_no_wake(self):
        self.assertIn("wakeOnlyUntil = SystemClock.elapsedRealtime() + 15_000L;", SERVICE)
        self.assertIn("firstQuestionOnMicStart = true;", SERVICE)
        self.assertIn("wakeOnlyUntil = bargePendingUntil;", SERVICE)
        self.assertIn("now < wakeOnlyUntil", SERVICE)
        self.assertIn('settings.get("followup_handsfree", "0")', SERVICE)
        self.assertIn("if (heard.trim().split", SERVICE)

    def test_vosk_partial_preserved_if_final_empty(self):
        self.assertIn("tail.isEmpty() ? interim : tail", SERVICE)

    def test_diagnosis_is_privacy_preserving(self):
        self.assertIn("recognitionDiagnostic()", SERVICE)
        self.assertIn("asrDiagnostic(", SERVICE)
        self.assertIn("FridayService.recognitionDiagnostic()", UI)
        self.assertIn("Aucune clé, aucun GPS, aucun contenu de conversation", UI)

    def test_cockpit_shows_correct_engine(self):
        self.assertIn("<b>JARVIS</b>", WEB)
        self.assertNotIn("<b>MISTRAL</b>", WEB)
        self.assertIn("kind==='asr_diag'", WEB)

    def test_java_speech_prefix_and_strip(self):
        if not shutil.which("javac") or not shutil.which("java"):
            self.skipTest("JDK absent")
        smoke = """
        import fr.friday.assistant.*;
        public class SpeechSmoke {
          public static void main(String[] args) {
            for (String prefix : new String[]{"Friday", "Fraday", "Jarvis", "Jarvise", "Jervis"}) {
              String phrase = prefix + ", quelle heure est-il ?";
              if (!BargeInPolicy.startsWithWake(phrase)) throw new AssertionError(prefix);
              if (!RustWake.detect(phrase)) throw new AssertionError(prefix);
              if (!VoiceText.withoutWakeWord(phrase).equals("quelle heure est-il ?"))
                  throw new AssertionError(VoiceText.withoutWakeWord(phrase));
            }
            if (BargeInPolicy.startsWithWake("Nous parlons de Jarvis"))
                throw new AssertionError("Activation au milieu d'une phrase");
            if (!VoiceText.withoutWakeWord("Qui est Jarvis ?").equals("Qui est Jarvis ?"))
                throw new AssertionError("Question ordinaire tronquée");
          }
        }
        """
        with tempfile.TemporaryDirectory() as tmp:
            p=Path(tmp)
            (p/"SpeechSmoke.java").write_text(smoke, encoding="utf-8")
            subprocess.run(["javac","-encoding","UTF-8","-d",str(p),
                str(JAVA/"VoiceText.java"),str(JAVA/"BargeInPolicy.java"),
                str(JAVA/"RustWake.java"),str(p/"SpeechSmoke.java")],
                check=True,capture_output=True,text=True)
            subprocess.run(["java","-cp",str(p),"SpeechSmoke"],
                check=True,capture_output=True,text=True)

if __name__ == "__main__":
    unittest.main()
