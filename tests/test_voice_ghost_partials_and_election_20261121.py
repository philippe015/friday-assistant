"""FRIDAY 21/11: microphone ghost partials and ambiguous election candidate requests."""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"
SERVICE = (JAVA / "FridayService.java").read_text(encoding="utf-8")
TOOLS = (JAVA / "JarvisAndroidTools.java").read_text(encoding="utf-8")
POLICY = (JAVA / "ElectionQuestionPolicy.java").read_text(encoding="utf-8")

class VoiceQuestionCaptureRegressions(unittest.TestCase):
    def test_microphone_keeps_listening_while_web_search_is_pending(self):
        self.assertNotIn("boolean blocked = currentTurn != null", SERVICE)
        self.assertIn("boolean blocked = TtsEchoGate.suppressGeneralAsr(", SERVICE)
        self.assertIn("if (blocked) {", SERVICE)
        self.assertIn("recognizer.close(); recognizer = createRecognizer(model);", SERVICE)
        self.assertIn("écoute Vosk active pendant la recherche", SERVICE)
        self.assertIn("Question entendue pendant le traitement précédent", SERVICE)

    def test_final_vosk_phrase_is_logged_instead_of_guessing_from_partials(self):
        self.assertIn('asrDiagnostic("Question finalisée par Vosk : « "', SERVICE)
        self.assertIn('String finalJson = recognizer.getFinalResult();', SERVICE)
        self.assertIn("tail.isEmpty() ? interim : tail", SERVICE)
        self.assertIn("boolean unclear =", SERVICE)

    def test_manual_stop_still_enabled_and_voice_stop_disabled(self):
        self.assertIn("boolean bargeEnabled = false;", SERVICE)
        self.assertIn("if (currentTurn != null) {", SERVICE)
        self.assertIn('if (running) interruptResponse("Réponse interrompue.', SERVICE)

    def test_presidential_candidate_clarification_is_fast_and_local(self):
        self.assertIn("ElectionQuestionPolicy.clarification(utterance)", TOOLS)
        self.assertIn("if (clarification != null) return clarification;", TOOLS)
        self.assertNotIn("HttpURLConnection", POLICY)
        self.assertNotIn("URL(", POLICY)
        if not shutil.which("javac") or not shutil.which("java"):
            self.skipTest("JDK unavailable")
        smoke=r"""
import fr.friday.assistant.ElectionQuestionPolicy;

public class ElectionQuestionSmoke {
    static void expected(String question, boolean mustClarify) {
        String out = ElectionQuestionPolicy.clarification(question);
        if (mustClarify != (out != null))
            throw new AssertionError(question + " -> " + out);
    }
    public static void main(String[] args) {
        expected("donne-moi le nom des candidats de l'élection présidentielle", true);
        expected("deux le nom des candidats de l'élection présidentielle", true);
        expected("quels sont les candidats à la présidentielle française de 2027", false);
        expected("les candidats de l'élection présidentielle en France", true);
        expected("les candidats de l'élection présidentielle de 2027", true);
        expected("quel est le calendrier des élections européennes ?", false);
        expected("explique le système politique français", false);
        String clarification = ElectionQuestionPolicy.clarification(
                "donne-moi le nom des candidats de l'élection présidentielle");
        if (!clarification.contains("pays") || !clarification.contains("année"))
            throw new AssertionError("missing country/year prompt");
    }
}
"""
        with tempfile.TemporaryDirectory() as td:
            t=Path(td)
            (t/"ElectionQuestionSmoke.java").write_text(smoke,encoding="utf-8")
            subprocess.run(["javac","-encoding","UTF-8","-d",str(t),
                str(JAVA/"ElectionQuestionPolicy.java"),str(t/"ElectionQuestionSmoke.java")],
                check=True,capture_output=True,text=True)
            subprocess.run(["java","-cp",str(t),"ElectionQuestionSmoke"],
                           check=True,capture_output=True,text=True)

if __name__=="__main__":
    unittest.main()
