"""Non-regression tests for FRIDAY's on-device wake word and public Web search."""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"
SERVICE = (JAVA / "FridayService.java").read_text(encoding="utf-8")
WEB = (JAVA / "JarvisInternet.java").read_text(encoding="utf-8")
RUST = (JAVA / "RustWake.java").read_text(encoding="utf-8")
VOICE = (JAVA / "VoiceText.java").read_text(encoding="utf-8")
BARGE = (JAVA / "BargeInPolicy.java").read_text(encoding="utf-8")


class VoiceAndWebRegression(unittest.TestCase):
    def test_wake_fallback_even_when_native_library_loaded(self):
        self.assertIn("hasWakeWord(normalized) || VoiceText.hasWakeWord(text)", RUST)
        for spelling in ("friday", "fraday", "freddy", "fredi", "vendredi"):
            self.assertIn(spelling, VOICE)
            self.assertIn(spelling, BARGE)

    def test_java_wake_recognition_behavior(self):
        if not shutil.which("javac") or not shutil.which("java"):
            self.skipTest("JDK unavailable; source invariants still checked")
        smoke = """
            import fr.friday.assistant.*;
            public class WakeSmoke {
                public static void main(String[] args) {
                    for (String name : new String[]{"Friday", "Fraday", "Freddy", "Fredi", "Vendredi"}) {
                        String sentence = name + ", quelle heure est-il ?";
                        if (!BargeInPolicy.startsWithWake(sentence)) throw new AssertionError(name);
                        if (!VoiceText.hasWakeWord(sentence)) throw new AssertionError(name);
                        if (!RustWake.detect(sentence)) throw new AssertionError(name);
                        if (!VoiceText.withoutWakeWord(sentence).equals("quelle heure est-il ?"))
                            throw new AssertionError("wake strip: " + name);
                    }
                    if (BargeInPolicy.startsWithWake("Nous sommes vendredi"))
                        throw new AssertionError("woke in the middle of sentence");
                }
            }
        """
        with tempfile.TemporaryDirectory() as directory:
            base = Path(directory)
            (base / "WakeSmoke.java").write_text(smoke, encoding="utf-8")
            subprocess.run([
                "javac", "-encoding", "UTF-8", "-d", str(base),
                str(JAVA / "VoiceText.java"), str(JAVA / "BargeInPolicy.java"),
                str(JAVA / "RustWake.java"), str(base / "WakeSmoke.java")
            ], check=True, capture_output=True, text=True)
            subprocess.run(["java", "-cp", str(base), "WakeSmoke"],
                           check=True, capture_output=True, text=True)

    def test_microphone_released_before_retry_sleep(self):
        self.assertIn("long retryMs = 0L;", SERVICE)
        self.assertIn("if (running && retryMs > 0)", SERVICE)
        self.assertIn("Thread.sleep(retryMs)", SERVICE)
        # Last AudioRecord release must precede the retry delay.
        self.assertLess(SERVICE.rfind("ar.release();"), SERVICE.index("Thread.sleep(retryMs)"))
        self.assertIn("voiceState(\"Microphone indisponible", SERVICE)

    def test_two_provider_fallback_deduplicates(self):
        self.assertIn("private static void mergeUnique(", WEB)
        self.assertGreaterEqual(WEB.count("if (found.size() < MAX_RESULTS)"), 2)
        self.assertIn("previous.url.equals(candidate.url)", WEB)
        self.assertIn("parseRss(", WEB)
        self.assertIn("parseDuckDuckGo(", WEB)
        self.assertIn("RssFreshness.withinHours(published, now, recentHours)", WEB)

    def test_search_never_sends_personal_memory_or_uses_mistral(self):
        self.assertIn('settings.get("jarvis_web_enabled", "1")', WEB)
        self.assertIn("LocalJarvisEngine.answer(context, settings, prompt, cancellation)", WEB)
        self.assertNotIn("api.mistral.ai", WEB)
        self.assertNotIn("PersonalMemory.", WEB)
        self.assertNotIn("FridayLocation.", WEB)
        self.assertIn("WebPageReader.checkedUri(url)", WEB)


if __name__ == "__main__":
    unittest.main()
