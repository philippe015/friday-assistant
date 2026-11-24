"""Memory Evolution+ regression: bounded offline paraphrase search and encrypted UI.

No real user dialog, reports, keys or private data are included in this test.
"""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"

class MemoryEvolutionTest(unittest.TestCase):
    def test_safe_paraphrase_matching(self):
        if not shutil.which("javac") or not shutil.which("java"):
            self.skipTest("JDK unavailable")
        source = r"""
import fr.friday.assistant.LocalSemanticMemoryPolicy;
import fr.friday.assistant.LocalLearningPolicy;

public final class MemoryEvolutionSmoke {
    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        check(LocalSemanticMemoryPolicy.similar(
                 "Quel temps fait-il à Paris demain ?",
                 "Donne-moi la météo de Paris demain"),
              "An equivalent weather question must retrieve the same correction");
        check(LocalSemanticMemoryPolicy.similar(
                 "Quelle est la météo à Paris demain ?",
                 "Bulletin météo de Paris pour demain"),
              "Weather forecast variants should match");
        check(!LocalSemanticMemoryPolicy.similar(
                 "Quel temps fait-il à Paris demain ?",
                 "Quelle météo à Lyon demain ?"),
              "Paris must not use Lyon correction");
        check(!LocalSemanticMemoryPolicy.similar(
                 "Quelle est la météo de Paris aujourd'hui ?",
                 "Quelle est la météo de Paris demain ?"),
              "Today versus tomorrow never merges");
        check(!LocalSemanticMemoryPolicy.similar(
                 "Quels sont les résultats pour 2026 ?",
                 "Quels sont les résultats pour 2027 ?"),
              "Year mismatch never merges");
        check(!LocalSemanticMemoryPolicy.similar(
                 "Quelle est la stratégie avec mesure 100 ?",
                 "Quelle est la stratégie avec mesure 200 ?"),
              "Numeric identifiers never merge");
        check(!LocalSemanticMemoryPolicy.similar(
                 "Comment utiliser la commande sans sauvegarde ?",
                 "Comment utiliser la commande avec sauvegarde ?"),
              "Negation must not merge with affirmative");
        check(!LocalSemanticMemoryPolicy.similar(
                 "Aide", "Aide"),
              "Insufficient topical tokens must not generalize");
        check(LocalSemanticMemoryPolicy.sameCorrection(
                 "Le résultat est 100.", "le résultat est 100"),
              "Equivalent approved corrections should not conflict");
        check(!LocalSemanticMemoryPolicy.sameCorrection(
                 "Le résultat est 100.", "Le résultat est 200."),
              "Contradictory answers must be quarantined");
        check(LocalLearningPolicy.sameQuestion(
                 "Jarvis, Quelle météo à Paris ?", "quelle meteo a Paris"),
              "Legacy exact-match compatibility maintained");
        System.out.println("MemoryEvolutionSmoke OK");
    }
}
"""
        with tempfile.TemporaryDirectory() as tmp:
            p = Path(tmp) / "MemoryEvolutionSmoke.java"
            p.write_text(source, encoding="utf-8")
            proc = subprocess.run(
                ["javac", "-encoding", "UTF-8", "-d", tmp,
                 str(JAVA/"LocalLearningPolicy.java"),
                 str(JAVA/"LocalSemanticMemoryPolicy.java"), str(p)],
                 capture_output=True, text=True)
            self.assertEqual(proc.returncode, 0, proc.stderr)
            proc = subprocess.run(["java", "-cp", tmp, "MemoryEvolutionSmoke"],
                                  capture_output=True, text=True)
            self.assertEqual(proc.returncode, 0, proc.stderr+proc.stdout)

    def test_encrypted_schema_migration_and_conflict_quarantine(self):
        memory = (JAVA/"LocalLearningMemory.java").read_text(encoding="utf-8")
        engine = (JAVA/"LocalJarvisEngine.java").read_text(encoding="utf-8")
        activity = (JAVA/"MainActivity.java").read_text(encoding="utf-8")
        for marker in (
            'private static final String KEY = "jarvis_learning_v1"',
            "private static final int MAX_ENTRIES = 100",
            "prefs.privateSecret(KEY)",
            "prefs.putPrivateSecret(KEY, records.toString())",
            "prefs.removePrivateSecret(KEY)",
            "LocalSemanticMemoryPolicy.similarity(",
            "LocalLearningPolicy.sameQuestion(",
            'item.put("correction", "")',
            'item.put("pending_correction", note)',
            "if (!item.optString(\"pending_correction\", \"\").isEmpty()) return \"\";",
            "public static synchronized String listJson(",
            "public static synchronized String resolve(",
            "public static synchronized String delete(",
        ):
            self.assertIn(marker, memory)
        self.assertIn("LocalLearningMemory.promptHint(cfg, cleaned)", engine)
        self.assertIn("learningEntries()", activity)
        self.assertIn("learningResolveConflict(String id, boolean acceptNew)", activity)
        self.assertIn("learningDeleteEntry(String id)", activity)
        self.assertNotIn("JarvisInternet.", memory)
        self.assertNotIn("HttpURLConnection", memory)
        self.assertNotIn("localStorage", memory)

    def test_matrix_ui_requires_explicit_conflict_resolution(self):
        html = (ROOT/"app/src/main/assets/index.html").read_text(encoding="utf-8")
        for marker in (
            'id="learningMemoryBoard"', 'id="learningMemoryRefresh"',
            'id="learningMemoryEntries"', "function learningMemoryRefresh(){",
            "call('learningEntries')",
            "call('learningResolveConflict',String(entry.id),false)",
            "call('learningResolveConflict',String(entry.id),true)",
            "call('learningDeleteEntry',String(entry.id))",
            "if(erase.dataset.armed!=='yes')",
            "Corrections suspendues",
            "$('learningMemoryRefresh').onclick=learningMemoryRefresh;",
        ):
            self.assertIn(marker, html)
        # The user still explicitly approves the model's suggested correction.
        self.assertIn("call('learningFeedback',question,'correction',correction)", html)
        self.assertNotIn("innerHTML=String(entry", html)
        self.assertNotIn("localStorage.setItem", html)
        self.assertNotIn("eval(", html)

    def test_release_unchanged_debug_version_bumped(self):
        gradle = (ROOT/"app/build.gradle").read_text(encoding="utf-8")
        self.assertIn("versionCode 112", gradle)
        self.assertIn("versionName '3.14.18'", gradle)
        self.assertIn("applicationIdSuffix '.v31421matrix'", gradle)
        self.assertIn("output.versionCode.set(122)", gradle)
        self.assertIn('output.versionName.set("3.14.28-voice-web-reliability-preview")', gradle)

if __name__ == "__main__":
    unittest.main()
