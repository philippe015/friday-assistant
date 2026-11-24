"""Auto-Critique+ regression: independent arithmetic, uncertain claims and UI."""
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/fr/friday/assistant"


class AutoCritiquePlusTest(unittest.TestCase):
    def test_independent_arithmetic_and_conservative_llm_parse(self):
        if not shutil.which("javac") or not shutil.which("java"):
            self.skipTest("Java JDK unavailable")
        smoke=r"""
import fr.friday.assistant.LocalCritiquePolicy;
import fr.friday.assistant.LocalLearningPolicy;

public final class AutoCritiqueSmoke {
    static void check(boolean yes, String message) {
        if (!yes) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        var wrong=LocalCritiquePolicy.verifyArithmetic("12262×43737=",
                    "12262 x 43737 égale 539282374.");
        check(wrong!=null && wrong.deterministic && wrong.verdict.equals("ERREUR_CALCUL"),
              "Wrong arithmetic must be caught by offline parser");
        check(wrong.correction.contains("536303094"),
              "Exact expected answer must be supplied, not guessed by LLM");
        var correct=LocalCritiquePolicy.verifyArithmetic("12262×43737=",
                    "Le résultat exact est 536 303 094.");
        check(correct!=null && correct.verdict.equals("CALCUL_VERIFIE")
              && correct.correction.isEmpty(), "Verified correct arithmetic");
        check(LocalCritiquePolicy.verifyArithmetic("Quel est le maire de Lyon ?",
                    "Inconnu") == null, "Not an arithmetic question");
        var selfPraise=LocalCritiquePolicy.parseModel(
                    "VERDICT: CORRECT\nMOTIF: Je crois que c'est plausible\nCORRECTION: Aucune",
                    false);
        check(!selfPraise.deterministic
              && selfPraise.verdict.equals("AUCUNE_ANOMALIE_IDENTIFIEE"),
              "The self-critique must not certify its own output");
        var suspect=LocalCritiquePolicy.parseModel(
                    "VERDICT: ERREUR\nMOTIF: Contradiction interne constatée"
                    + "\nCORRECTION: Préciser les hypothèses avant de répondre.",false);
        check(suspect.verdict.equals("ANOMALIE_POSSIBLE")
              && suspect.correction.contains("hypothèses"),
              "LLM flags possible error with editable suggestion");
        var news=LocalCritiquePolicy.parseModel(
                    "VERDICT: CORRECT\nMOTIF: D'après moi c'est actuel"
                    + "\nCORRECTION: Les chiffres ont été confirmés.",true);
        check(news.verdict.equals("SOURCES_A_VERIFIER")
              && news.correction.isEmpty(),
              "Current facts cannot be verified by a second local generation");
        var malformed=LocalCritiquePolicy.parseModel(
                    "Tout est parfait !",false);
        check(malformed.verdict.equals("A_VERIFIER")
              && malformed.correction.isEmpty(),
              "Do not accept malformed, overconfident model output");
        String prompt=LocalLearningPolicy.reviewPrompt("Quelle est la durée de sommeil ?",
                "Réponse initiale contenant une donnée à vérifier.");
        check(prompt.contains("VERDICT: CORRECT ou ERREUR ou INCERTAIN")
              && prompt.contains("MOTIF:")
              && prompt.contains("CORRECTION:"),
              "Model audit is required to explain its judgment");
        check(LocalLearningPolicy.reviewPrompt("Question correcte",
                "a".repeat(1501))==null, "No silent truncation of long answer");
        check(LocalLearningPolicy.reviewPrompt("Question correcte",
                "réponse complète")!=null, "Normal review");
        System.out.println("AutoCritiqueSmoke OK");
    }
}
"""
        with tempfile.TemporaryDirectory() as td:
            path=Path(td)/"AutoCritiqueSmoke.java"
            path.write_text(smoke,encoding="utf-8")
            command=["javac","-encoding","UTF-8","-d",td,
                     str(JAVA/"LocalJarvisArithmetic.java"),
                     str(JAVA/"LocalLearningPolicy.java"),
                     str(JAVA/"LocalCritiquePolicy.java"),str(path)]
            p=subprocess.run(command,capture_output=True,text=True)
            self.assertEqual(p.returncode,0,p.stderr)
            p=subprocess.run(["java","-cp",td,"AutoCritiqueSmoke"],
                             capture_output=True,text=True)
            self.assertEqual(p.returncode,0,p.stderr+p.stdout)

    def test_llm_review_is_separate_from_normal_chat_and_web(self):
        engine=(JAVA/"LocalJarvisEngine.java").read_text(encoding="utf-8")
        reviewer=(JAVA/"LocalLearningAdvisor.java").read_text(encoding="utf-8")
        ui=(ROOT/"app/src/main/assets/index.html").read_text(encoding="utf-8")
        activity=(JAVA/"MainActivity.java").read_text(encoding="utf-8")
        self.assertIn("public static String reviewRaw(Context context",engine)
        self.assertIn("return generateRaw(context, cfg, auditPrompt, cancellation)",engine)
        self.assertIn("buildLocalPrompt(cfg, cleaned, privateDialogue)",engine)
        self.assertIn("LocalCritiquePolicy.verifyArithmetic(question, answer)",reviewer)
        self.assertIn("LocalCritiquePolicy.parseModel(raw, current)",reviewer)
        self.assertIn("WebSearch.requiresCurrentEvidence(question)",reviewer)
        self.assertIn("LocalJarvisEngine.reviewRaw(",reviewer)
        self.assertNotIn("JarvisInternet.answer(",reviewer)
        self.assertIn("JSONObject",reviewer)
        self.assertIn("verificationOutil",reviewer)
        self.assertIn("window.fridayLearningProposal&&window.fridayLearningProposal(",activity)
        self.assertIn("org.json.JSONObject.quote(answer)",activity)
        self.assertIn("window.fridayLearningProposal=function(question,answer,report,error)",ui)
        self.assertIn("latestAnswer!==answer",ui)
        self.assertIn("state.learningProposalQuestion",ui)
        self.assertIn("state.learningProposalAnswer",ui)
        self.assertIn("audit.verificationOutil===true",ui)
        self.assertNotIn("innerHTML=audit",ui)
        callback=ui.split("window.fridayLearningProposal=function(",1)[1].split(
                   "function learningVote(",1)[0]
        self.assertNotIn("learningFeedback",callback,
                          "A review result must never be auto-saved")

    def test_no_changes_to_play_release(self):
        gradle=(ROOT/"app/build.gradle").read_text(encoding="utf-8")
        self.assertIn("output.versionCode.set(120)",gradle)
        self.assertIn('output.versionName.set("3.14.26-local-benchmark-preview")',gradle)
        self.assertIn("applicationIdSuffix '.v31421matrix'",gradle)
        self.assertIn("versionCode 112",gradle)
        self.assertIn("versionName '3.14.18'",gradle)


if __name__=="__main__":
    unittest.main()
