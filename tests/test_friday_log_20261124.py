"""Regression tests for issues visible in the 2026-11-24 local diagnostic export.

The user's diagnostic JSON and private conversation are NOT committed to GitHub.
Run pure-JVM arithmetic tests and inspect Android routing for the same behavior
in voice, typed chat and the Matrix terminal.
"""
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"


class FridayLog20261124(unittest.TestCase):
    def test_bare_math_is_exact_and_offline(self):
        if not shutil.which("javac") or not shutil.which("java"):
            self.skipTest("JDK not installed")
        smoke = r"""
import fr.friday.assistant.LocalJarvisArithmetic;
public final class FridayMathSmoke {
  private static void same(String input, String expected) {
    String actual = LocalJarvisArithmetic.tryAnswer(input);
    if (!java.util.Objects.equals(actual, expected))
      throw new AssertionError(input + " => " + actual + " expected " + expected);
  }
  public static void main(String[] args) {
    same("12262×43737=", "Calcul effectué localement sur le téléphone : 536303094.");
    same("12 262 × 43 737 =",
      "Calcul effectué localement sur le téléphone : 536303094.");
    same("calcule 17 fois 19",
      "Calcul effectué localement sur le téléphone : 323.");
    same("Jarvis, calcule 7+8",
      "Calcul effectué localement sur le téléphone : 15.");
    same("12+3", "Calcul effectué localement sur le téléphone : 15.");
    same("2026-11-24", null);
    same("12262", null);
    same("Suis ravi de te revoir", null);
    System.out.println("FridayMathSmoke OK");
  }
}
"""
        with tempfile.TemporaryDirectory() as tmp:
            code = Path(tmp) / "FridayMathSmoke.java"
            code.write_text(smoke, encoding="utf-8")
            compile_run = subprocess.run([
                "javac", "-encoding", "UTF-8", "-d", tmp,
                str(JAVA / "LocalJarvisArithmetic.java"), str(code)
            ], capture_output=True, text=True)
            self.assertEqual(compile_run.returncode, 0, compile_run.stderr)
            executed = subprocess.run(["java", "-cp", tmp, "FridayMathSmoke"],
                capture_output=True, text=True)
            self.assertEqual(executed.returncode, 0, executed.stderr + executed.stdout)

    def test_both_android_routes_use_arithmetic_before_internet(self):
        tools = (JAVA / "JarvisAndroidTools.java").read_text(encoding="utf-8")
        service = (JAVA / "FridayService.java").read_text(encoding="utf-8")
        activity = (JAVA / "MainActivity.java").read_text(encoding="utf-8")
        self.assertIn("LocalJarvisArithmetic.tryAnswer(cleaned)", tools)
        self.assertLess(tools.index("LocalJarvisArithmetic.tryAnswer(cleaned)"),
                        tools.index("return NativeAI.localAnswer(cleaned)"))
        self.assertLess(service.index("JarvisAndroidTools.answer("),
                        service.index("WebSearch.assistantAnswer(question"))
        self.assertLess(activity.index("JarvisAndroidTools.answer("),
                        activity.index("WebSearch.assistantAnswer(request"))
        self.assertIn("sendQuestion(question, true)", activity)
        self.assertIn("pureCalculation(question)", (JAVA / "WebSearch.java").read_text())

    def test_no_uploaded_conversation_or_web_secrets_in_tests(self):
        files = [str(p) for p in (ROOT / "tests").glob("*")]
        self.assertNotIn("JARVIS-rapport-conversation-20261124-100619.json",
                         "\n".join(files))


if __name__ == "__main__":
    unittest.main()
