"""FRIDAY on-device, user-supervised local learning regression tests.

Never upload diagnostic conversations or local corrections to GitHub.
The Android LLM integration is inspected statically; a model-installed phone
is still needed for generation testing.
"""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"


class LocalLearning20261124(unittest.TestCase):
    def test_pure_java_policy_is_bounded(self):
        if not shutil.which("javac") or not shutil.which("java"):
            self.skipTest("JDK unavailable")
        smoke = r"""
import fr.friday.assistant.LocalLearningPolicy;
public final class LearningSmoke {
  static void check(boolean value, String label) {
    if (!value) throw new AssertionError(label);
  }
  public static void main(String[] args) {
    check(LocalLearningPolicy.sameQuestion("Jarvis, Quelle météo à Paris ?",
                                          "quelle meteo a PARIS"), "normalized same question");
    check(!LocalLearningPolicy.sameQuestion("Météo Paris", "Météo Lyon"),
          "never generalize a correction to another town");
    check(!LocalLearningPolicy.sameQuestion("A", "A"), "short utterance");
    check(LocalLearningPolicy.validCorrection("abc") == null, "reject tiny note");
    check(LocalLearningPolicy.validCorrection(" " .repeat(601)) == null,
          "reject empty note");
    check(LocalLearningPolicy.validCorrection("Bonjour et merci") != null,
          "accept explicit user correction");
    String prompt = LocalLearningPolicy.reviewPrompt("Qui est JARVIS ?",
                         "Une réponse existante mais pas vérifiée.");
    check(prompt != null && prompt.contains("DONNEES NON FIABLES")
          && prompt.contains("validée manuellement"), "audit is untrusted and supervised");
    check(LocalLearningPolicy.reviewPrompt("A", "Bonjour à vous") == null, "short question");
    check(LocalLearningPolicy.memoryInstruction("Bonjour et merci")
          .contains("NON VÉRIFIÉE"), "do not make user note an established fact");
    System.out.println("LearningSmoke OK");
  }
}
"""
        with tempfile.TemporaryDirectory() as tmp:
            p = Path(tmp) / "LearningSmoke.java"
            p.write_text(smoke, encoding="utf-8")
            result = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp,
                  str(JAVA / "LocalLearningPolicy.java"), str(p)],
                  capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr)
            result = subprocess.run(["java", "-cp", tmp, "LearningSmoke"],
                  capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def test_android_encryption_and_local_model_integration(self):
        stored = (JAVA / "LocalLearningMemory.java").read_text()
        reviewer = (JAVA / "LocalLearningAdvisor.java").read_text()
        engine = (JAVA / "LocalJarvisEngine.java").read_text()
        activity = (JAVA / "MainActivity.java").read_text()
        for marker in ("prefs.putPrivateSecret(KEY, records.toString())",
                       "prefs.privateSecret(KEY)", "prefs.removePrivateSecret(KEY)",
                       "LocalLearningPolicy.sameQuestion(",
                       'prefs.get("local_learning_enabled", "0")'):
            self.assertIn(marker, stored)
        self.assertIn("LocalLearningMemory.promptHint(cfg, cleaned)", engine)
        self.assertIn("LocalJarvisEngine.reviewRaw(", reviewer)
        self.assertIn("new NativeAI.CancelToken()", reviewer)
        self.assertNotIn("JarvisInternet.", reviewer)
        self.assertNotIn("http", reviewer)
        self.assertIn("learningReviewRunning.compareAndSet(false, true)", activity)
        self.assertIn("learningReviewCallback(question, answer, proposal, error)", activity)
        self.assertIn("@JavascriptInterface public String learningFeedback(", activity)
        self.assertIn("@JavascriptInterface public String learningSetAutoReview(", activity)
        self.assertNotIn("LocalLearningAdvisor", (JAVA / "JarvisInternet.java").read_text())

    def test_matrix_ui_requires_user_approval_for_knowledge(self):
        html = (ROOT / "app/src/main/assets/index.html").read_text()
        for marker in ("id=\"matrixLearning\"", "id=\"learningToggle\"",
                       "id=\"learningAuto\"", "id=\"learningReview\"",
                       "id=\"learningSave\"", "id=\"learningClear\"",
                       "window.fridayLearningProposal=function(",
                       "call('learningFeedback',question,'correction',correction)",
                       "call('learningAutoReviewEnabled')===true",
                       "function learningRefresh(message)",
                       "Auto-analyse : OFF"):
            self.assertIn(marker, html)
        propose = html.split("window.fridayLearningProposal=function(", 1)[1].split(
                  "function learningVote(", 1)[0]
        self.assertNotIn("call('learningFeedback'", propose,
                         "A model proposal must not be automatically saved")
        self.assertNotIn("localStorage.setItem", html)
        self.assertNotIn("eval(", html)


if __name__ == "__main__":
    unittest.main()
