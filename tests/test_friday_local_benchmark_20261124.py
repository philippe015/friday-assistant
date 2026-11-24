"""FRIDAY 3.14.26 — isolated local-model benchmark regression tests.
The tests are synthetic; they do not require user files or a model download.
"""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/fr/friday/assistant"

class LocalBenchmarkTests(unittest.TestCase):
    def test_challenge_policy_with_jvm(self):
        if not shutil.which("javac") or not shutil.which("java"):
            self.skipTest("JDK not available")
        source=r"""
import fr.friday.assistant.LocalBenchmarkPolicy;
public class LocalBenchmarkSmoke {
    static void yes(boolean v, String why) { if(!v)throw new AssertionError(why); }
    public static void main(String[] args) {
        yes(LocalBenchmarkPolicy.count()==10,"Ten fixed questions required");
        yes("INDICE_POSITIF".equals(LocalBenchmarkPolicy.evaluate(0,"La réponse est 391.")),"arithmetic");
        yes("NON_CONFIRME".equals(LocalBenchmarkPolicy.evaluate(0,"Je ne sais pas.")),"no invented pass");
        yes("INDICE_POSITIF".equals(LocalBenchmarkPolicy.evaluate(3,"C'est Lisbonne.")),"capital");
        yes("INDICE_POSITIF".equals(LocalBenchmarkPolicy.evaluate(4,"Bonjour !")),"French");
        yes("INDICE_POSITIF".equals(LocalBenchmarkPolicy.evaluate(5,"Lent.")),"antonym");
        yes("INDICE_POSITIF".equals(LocalBenchmarkPolicy.evaluate(6,"Les enfants jouent dans le jardin.")),"grammar");
        yes("INDICE_POSITIF".equals(LocalBenchmarkPolicy.evaluate(7,"def somme(a,b):\n    return a+b")),"Python");
        yes("LECTURE_MANUELLE".equals(LocalBenchmarkPolicy.evaluate(8,"Une longue explication.")),"no fake score");
        yes("SANS_REPONSE".equals(LocalBenchmarkPolicy.evaluate(9," ")),"empty reply");
        yes(LocalBenchmarkPolicy.prompt(0).contains("Réponds en français"),"local prompt");
        for(int i=0;i<LocalBenchmarkPolicy.count();i++){
            String prompt=LocalBenchmarkPolicy.prompt(i).toLowerCase();
            yes(!prompt.contains("https://") && !prompt.contains("memoire personnelle"),"no private data or URLs");
            yes(prompt.length()>60 && prompt.length()<650,"bounded native inference");
        }
        System.out.println("LocalBenchmarkSmoke OK");
    }
}
"""
        with tempfile.TemporaryDirectory() as tmp:
            p=Path(tmp)/"LocalBenchmarkSmoke.java"
            p.write_text(source,encoding="utf-8")
            cp=subprocess.run(["javac","-encoding","UTF-8","-d",tmp,
                               str(JAVA/"LocalBenchmarkPolicy.java"),str(p)],
                              capture_output=True,text=True)
            self.assertEqual(0,cp.returncode,cp.stderr)
            cp=subprocess.run(["java","-cp",tmp,"LocalBenchmarkSmoke"],
                              capture_output=True,text=True)
            self.assertEqual(0,cp.returncode,cp.stderr+cp.stdout)

    def test_strictly_local_and_no_personal_data_or_network(self):
        policy=(JAVA/"LocalBenchmarkPolicy.java").read_text(encoding="utf-8")
        runner=(JAVA/"LocalModelBenchmark.java").read_text(encoding="utf-8")
        engine=(JAVA/"LocalJarvisEngine.java").read_text(encoding="utf-8")
        ui=(ROOT/"app/src/main/assets/index.html").read_text(encoding="utf-8")
        activity=(JAVA/"MainActivity.java").read_text(encoding="utf-8")
        self.assertIn("benchmarkRaw(",engine)
        self.assertIn("return generateRaw(context, cfg, fixedPrompt, cancellation)",engine)
        self.assertIn("LocalJarvisEngine.benchmarkRaw(",runner)
        self.assertIn("prefs.setSecret(VAULT_KEY, json)",runner)
        self.assertIn("NativeAI.CancelToken",runner)
        self.assertIn("public String stop()",runner)
        self.assertIn("SystemClock.elapsedRealtime()",runner)
        self.assertIn("new Thread(() -> run(app, prefs, token)",runner)
        self.assertNotIn("JarvisInternet.answer",runner)
        self.assertNotIn("PrivateConversationHistory.read",runner)
        self.assertNotIn("DialogueContext.recent",runner)
        self.assertNotIn("HttpURLConnection",runner)
        self.assertNotIn("https://",policy)
        self.assertIn('id="modelBenchmarkPanel"',ui)
        self.assertIn("function benchmarkRender()",ui)
        self.assertIn("textContent",ui)
        self.assertIn("benchmarkStart()",activity)
        self.assertIn("benchmarkCancel()",activity)
        self.assertIn("benchmarkCopy()",activity)

    def test_only_debug_identifier_changed(self):
        gradle=(ROOT/"app/build.gradle").read_text(encoding="utf-8")
        self.assertIn("versionCode 112",gradle)
        self.assertIn("versionName '3.14.18'",gradle)
        self.assertIn("applicationIdSuffix '.v31421matrix'",gradle)
        self.assertIn("output.versionCode.set(123)",gradle)
        self.assertIn('output.versionName.set("3.14.29-voice-response-optimizer-preview")',gradle)

if __name__=="__main__":
    unittest.main()
