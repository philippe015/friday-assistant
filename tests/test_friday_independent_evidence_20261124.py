"""Regression tests for explicit, independent Web source consultation.

Local model self-evaluation must never be mistaken for independent proof.
Private memory, contacts, identity, GPS and conversation content never leave
the device through this source-verification path.
"""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"


class IndependentEvidence20261124(unittest.TestCase):
    def test_public_consent_policy_jvm(self):
        if not shutil.which("javac") or not shutil.which("java"):
            self.skipTest("Java JDK not installed")
        smoke = r"""
import fr.friday.assistant.IndependentEvidencePolicy;
public final class EvidenceSmoke {
 static void yes(boolean value, String reason) {
   if (!value) throw new AssertionError(reason);
 }
 public static void main(String[] args) {
   yes(IndependentEvidencePolicy.eligible(
      "Quel est le président actuel de la France ?",true,true), "Public fact allowed");
   yes(IndependentEvidencePolicy.eligible(
      "Quelles sont les dernières actualités scientifiques ?",true,true), "Public news");
   yes(!IndependentEvidencePolicy.eligible(
      "Quel est le président actuel de la France ?",true,false), "Direct click required");
   yes(!IndependentEvidencePolicy.eligible(
      "Quel est le président actuel de la France ?",false,true), "Web permission required");
   yes(!IndependentEvidencePolicy.eligible(
      "Quel est mon mot de passe bancaire ?",true,true), "No password or private content");
   yes(!IndependentEvidencePolicy.eligible(
      "Quel est mon traitement médical actuel ?",true,true), "No medical data");
   yes(!IndependentEvidencePolicy.eligible(
      "Quels sont mes souvenirs dans nos conversations ?",true,true), "No dialogue");
   yes(!IndependentEvidencePolicy.eligible(
      "Quelle est ma position GPS exacte ?",true,true), "No location");
   yes(!IndependentEvidencePolicy.eligible(
      "Quelle est la météo sans Internet à Paris ?",true,true), "Respect offline consent");
   yes(!IndependentEvidencePolicy.eligible(
      "Quel est le 123456789 numéro ?",true,true), "No long personal numbers");
   yes(!IndependentEvidencePolicy.eligible(
      "Regarde https://exemple.org/page?token=abc",true,true), "No direct URLs with query");
   yes(!IndependentEvidencePolicy.eligible(
      "Bonjour, comment ça va ?",true,true), "No small talk search");
   yes(IndependentEvidencePolicy.institutionalCandidate(
      "https://www.service-public.gouv.fr/particuliers"), "Official candidate recognized");
   yes(!IndependentEvidencePolicy.institutionalCandidate(
      "https://service-public.gouv.fr.example.com"), "No fake subdomain authority");
   yes(IndependentEvidencePolicy.safeReference(
      "https://www.francetvinfo.fr/info"), "Public HTTPS accepted");
   yes(!IndependentEvidencePolicy.safeReference(
      "http://www.francetvinfo.fr/info"), "Reject HTTP");
   yes(!IndependentEvidencePolicy.safeReference(
      "https://localhost/secret"), "Reject localhost");
   yes(!IndependentEvidencePolicy.safeReference(
      "https://192.168.1.1/router"), "Reject private IP");
   yes(!IndependentEvidencePolicy.safeReference(
      "https://user:password@example.com/"), "Reject credentials");
   System.out.println("EvidenceSmoke OK");
 }
}
"""
        with tempfile.TemporaryDirectory() as tmp:
            test = Path(tmp)/"EvidenceSmoke.java"
            test.write_text(smoke, encoding="utf-8")
            result = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp,
                    str(JAVA/"IndependentEvidencePolicy.java"), str(test)],
                    capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr)
            result = subprocess.run(["java", "-cp", tmp, "EvidenceSmoke"],
                    capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr + result.stdout)

    def test_network_queries_public_question_only(self):
        internet = (JAVA/"JarvisInternet.java").read_text()
        activity = (JAVA/"MainActivity.java").read_text()
        self.assertIn("public static String independentEvidence(", internet)
        body = internet.split("public static String independentEvidence(",1)[1].split(
            "private static List<Source> research(",1)[0]
        self.assertIn("IndependentEvidencePolicy.eligible(question, enabled(settings), true)",body)
        self.assertIn("WebSearch.safeForGeneralSearch(question)",body)
        self.assertIn("research(question, cancellation)",body)
        self.assertIn("WebSearch.relevantResult(",body)
        self.assertIn("RssFreshness.withinHours(",body)
        self.assertIn("IndependentEvidencePolicy.safeReference(source.url)",body)
        self.assertIn("verificationOutil",body)
        self.assertIn("false",body)
        self.assertNotIn("privateDialogue",body)
        self.assertNotIn("PrivateConversationHistory",body)
        self.assertNotIn("LocalLearningMemory.promptHint(",body)
        self.assertNotIn("new LocalJarvisEngine",body)
        self.assertNotIn("prefs.privateSecret",body)
        self.assertIn("verifyIndependentEvidence(String question,",activity)
        self.assertIn("independentEvidenceCallback(question, answer, report, error)",activity)
        self.assertIn("evidenceReviewRunning.compareAndSet(false, true)",activity)

    def test_ui_does_not_auto_send_sources_or_accept_uncited_results(self):
        html = (ROOT/"app/src/main/assets/index.html").read_text()
        self.assertIn('id="learningVerifySources"',html)
        self.assertIn('id="learningSourcesList"',html)
        self.assertIn("window.fridayIndependentEvidence=function(question,answer,report,error)",html)
        self.assertIn("question!==current||answer!==currentAnswer",html)
        self.assertIn("link.rel='noopener noreferrer'",html)
        self.assertIn("excerpt.textContent=",html)
        self.assertIn("host.replaceChildren()",html)
        # Only clicking the sources button may invoke the bridge: no
        # automatic external search during local audit or Matrix replies.
        self.assertIn("$('learningVerifySources').onclick=verifySources;",html)
        self.assertEqual(html.count("call('verifyIndependentEvidence',question,answer)"),1)
        self.assertNotIn("innerHTML=String(item",html)


if __name__ == "__main__":
    unittest.main()
