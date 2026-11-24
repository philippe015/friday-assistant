"""Kotlin JARVIS: genuine Android wiring + executable JVM smoke when compiler exists."""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/"app/src/main/java/fr/friday/assistant/JarvisKotlinBrain.kt"
JAVA=ROOT/"app/src/main/java/fr/friday/assistant"
CODE=SOURCE.read_text(encoding="utf-8")

class KotlinIntelligenceRegression(unittest.TestCase):
    def test_kotlin_guard_is_offline_and_bounded(self):
        self.assertIn("object JarvisKotlinBrain", CODE)
        self.assertIn("@JvmStatic", CODE)
        self.assertIn("needsVoiceClarification(", CODE)
        self.assertIn("localClarification(", CODE)
        self.assertNotIn("HttpURLConnection", CODE)
        self.assertNotIn("api.mistral.ai", CODE)
        self.assertNotIn("PrivateConversationHistory.read", CODE)
        self.assertNotIn("Runtime.getRuntime()", CODE)

    def test_voice_and_text_both_use_kotlin_before_web(self):
        voice=(JAVA/"FridayService.java").read_text(encoding="utf-8")
        typed=(JAVA/"MainActivity.java").read_text(encoding="utf-8")
        self.assertIn("JarvisKotlinBrain.needsVoiceClarification(", voice)
        self.assertIn("JarvisKotlinBrain.localClarification(question, privateDialogue)", voice)
        self.assertIn("JarvisKotlinBrain.localClarification(request, privateDialogue)", typed)
        self.assertIn('DiagnosticLog.record(this, "KOTLIN_TURN_CLARIFIED")', voice)
        self.assertIn('"KOTLIN_TURN_CLARIFIED"', typed)
        self.assertLess(voice.index("JarvisKotlinBrain.localClarification("),
                        voice.index("WebSearch.assistantAnswer(question"))
        self.assertLess(typed.index("JarvisKotlinBrain.localClarification("),
                        typed.index("WebSearch.assistantAnswer(request"))

    def test_uncertain_dictation_is_a_listening_event(self):
        voice = (JAVA / "FridayService.java").read_text(encoding="utf-8")
        unclear = voice.split("if (unclear || unreliableLongAsr) {", 1)[1].split(
            '} else {\n                                    asrDiagnostic("Question finalisée', 1)[0]
        self.assertIn('asrDiagnostic("Dictée incertaine', unclear)
        self.assertIn("acceptsDirectQuestion(now)", unclear)
        self.assertNotIn('event("error"', unclear)
        self.assertNotIn('event("user"', unclear)
        self.assertNotIn("handlePhrase(", unclear)
        self.assertIn('return "VOSK_CLARIFICATION"',
                      (JAVA / "DiagnosticLogPolicy.java").read_text(encoding="utf-8"))

    def test_jvm_logic(self):
        if not (shutil.which("kotlinc") and shutil.which("java")):
            self.skipTest("Kotlin CLI absent; Android CI Gradle still compiles source")
        smoke=r"""
import fr.friday.assistant.JarvisKotlinBrain
fun expect(value: Boolean, message: String) { if (!value) error(message) }
fun main() {
    val noContext: String? = null
    expect(JarvisKotlinBrain.localClarification("Non", noContext) != null,
        "Non does not trigger a Web lookup")
    expect(JarvisKotlinBrain.localClarification("deux fois non", noContext) != null,
        "No repeated dismissal Web search")
    expect(JarvisKotlinBrain.localClarification("tout de suite c'est tout de suite", noContext) != null,
        "Repeated phrase requires an action")
    expect(JarvisKotlinBrain.localClarification("de en entier", noContext)
        ?.contains("Quel article") == true,
        "Clipped article instruction stays local without Wikipedia")
    expect(JarvisKotlinBrain.localClarification("notre article un humain", noContext) != null,
        "Ambiguous article clarifies")
    expect(JarvisKotlinBrain.localClarification("Merci", noContext) != null,
        "Simple thanks is local")
    expect(JarvisKotlinBrain.localClarification("Lis l'article trois en entier", noContext) == null,
        "Valid RSS read is not suppressed")
    expect(JarvisKotlinBrain.localClarification("Météo pour Joinville-le-Pont 94", noContext) == null,
        "Valid weather query reaches weather tool")
    expect(JarvisKotlinBrain.localClarification("Pourquoi l'univers est-il en expansion ?", noContext) == null,
        "A scientific question still reaches the engine")
    val prior="[ÉCHANGES]\nJarvis : Pour quelle ville souhaitez-vous la météo ?\n" +
        "Utilisateur : autre chose\nJarvis : Je vous écoute.\n"
    expect(JarvisKotlinBrain.localClarification("Joinville le Pont", prior)
        ?.contains("Confirmez-vous") == true, "bare city gets context-aware clarification")
    expect(JarvisKotlinBrain.localClarification("Joinville le Pont", noContext) == null,
        "no city intent fabricated without context")
    expect(JarvisKotlinBrain.needsVoiceClarification("deux fois non", 0.43, false),
        "low confidence ASR triggers confirmation")
    expect(!JarvisKotlinBrain.needsVoiceClarification("météo pour Lyon", 0.92, false),
        "high confidence weather preserved")
    expect(!JarvisKotlinBrain.needsVoiceClarification("article deux", 0.0, false),
        "unknown confidence not treated as failure")
    expect(!JarvisKotlinBrain.needsVoiceClarification("météo pour Lyon", 0.54, true),
        "explicit wake uses lower threshold")
    val news = "les dernières informations scientifiques"
    expect(!JarvisKotlinBrain.needsVoiceClarification(news, 0.49, false),
        "complete news request passes at moderate confidence")
    expect(JarvisKotlinBrain.needsVoiceClarification(news, 0.30, false),
        "very low confidence still needs a repeat")
    expect(JarvisKotlinBrain.needsVoiceClarification("il parle des dernières informations scientifiques", 0.49, false),
        "ambient phrase mentioning the news does not qualify")
    expect(JarvisKotlinBrain.needsVoiceClarification("des fragments incertains", 0.49, false),
        "other uncertain requests still need a repeat")
    println("KotlinBrainSmoke OK")
}
"""
        with tempfile.TemporaryDirectory() as tmp:
            p=Path(tmp)
            t=p/"KotlinBrainSmoke.kt"
            t.write_text(smoke,encoding="utf-8")
            jar=p/"test.jar"
            compiler=subprocess.run(["kotlinc", str(SOURCE), str(t), "-include-runtime", "-d", str(jar)],
                           check=False,text=True,capture_output=True,timeout=90)
            self.assertEqual(compiler.returncode, 0, compiler.stderr + compiler.stdout)
            done=subprocess.run(["java","-jar",str(jar)],
                                check=False,text=True,capture_output=True,timeout=30)
            self.assertEqual(done.returncode, 0, done.stderr + done.stdout)
            self.assertIn("KotlinBrainSmoke OK",done.stdout)

if __name__=="__main__": unittest.main()
