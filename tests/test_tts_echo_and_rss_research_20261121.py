"""Correctifs v3.14.2 : blocage de l'écho TTS et vraies sources RSS datées."""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/fr/friday/assistant"
def src(name):
    return (JAVA/name).read_text(encoding="utf-8")

class SpeechEchoAndRssRegressions(unittest.TestCase):
    def test_dispatch_blocks_speaker_echo_only_for_voice(self):
        service=src("FridayService.java")
        gate=src("VoiceSelfEchoPolicy.java")
        self.assertIn("boolean selfEcho = VoiceSelfEchoPolicy.resemblesRecentSpeech(",service)
        self.assertIn("if (selfEcho)",service)
        self.assertIn("Écho du haut-parleur JARVIS ignoré",service)
        self.assertIn("lastSpokenForEcho = speechAnswer",service)
        self.assertIn("lastSpokenAt = finishedAt",service)
        self.assertIn("boolean blocked = TtsEchoGate.suppressGeneralAsr(",service)
        self.assertNotIn("currentTurn != null || speakingNow",gate)
        # Written questions bypass the microphone self-echo block.
        self.assertIn("handlePhrase(typed)",service)
        self.assertNotIn("VoiceSelfEchoPolicy.resemblesRecentSpeech(\n                                    heard",service)

    def test_rss_recency_is_not_removed_before_search(self):
        internet=src("JarvisInternet.java")
        self.assertIn("RecentNewsPolicy.newsWindowHours(question) > 0",internet)
        self.assertIn("? question : WebSearch.searchQuery(question, \"\")",internet)
        self.assertIn("sources.addAll(search(effectiveQuery, cancellation))",internet)
        self.assertIn("if (!anyDated)",internet)
        self.assertIn("Aucun titre RSS daté vérifiable",internet)

    def test_java_speech_similarity_and_no_false_followups(self):
        if not shutil.which("javac") or not shutil.which("java"):
            self.skipTest("JDK unavailable")
        smoke=r"""
import fr.friday.assistant.VoiceSelfEchoPolicy;
public final class SpeechEchoSmoke {
    static void check(boolean value, String why) {
        if (!value) throw new AssertionError(why);
    }
    public static void main(String[] args) {
        long t=100000L;
        String tts="J'ai trouvé zéro titres dont l'horodatage RSS se situe "
            +"dans les vingt quatre dernières heures. Cela ne prouve "
            +"ni l'heure de l'événement ni l'exactitude des informations "
            +"publiées. Les liens figurent dans les sources écrites. "
            +"Je ne dispose pas nécessairement du texte intégral des articles. "
            +"Les résumés éventuels proviennent uniquement des descriptions RSS.";
        String echo="j ai trouvé zéro titres dont le rodage reiser se situe "
            +"dans les vingt quatre dernières heures cela ne prouve "
            +"ni lors de l événement ni l exactitude des informations publiées "
            +"les liens figure dans les sources écrites je ne dispose pas "
            +"nécessairement du texte intégral des articles";
        check(VoiceSelfEchoPolicy.resemblesRecentSpeech(echo,tts,t+4000,t),
            "exact user capture must be suppressed as Jarvis own speech");
        check(!VoiceSelfEchoPolicy.resemblesRecentSpeech(
            "et demain ?",tts,t+4000,t),"short natural follow-up kept");
        check(!VoiceSelfEchoPolicy.resemblesRecentSpeech(
            "Jarvis peux tu chercher les actualités de ce matin et citer la source",
            tts,t+6000,t),"unrelated real question kept");
        check(!VoiceSelfEchoPolicy.resemblesRecentSpeech(
            echo,tts,t+46000,t),"guard expires after 45 seconds");
        check(!VoiceSelfEchoPolicy.resemblesRecentSpeech(
            echo,tts,t-100,t),"clock regression must not reject user");
        check(!VoiceSelfEchoPolicy.resemblesRecentSpeech(
            echo,tts,t+6000,0),"no reference: no echo filtering");
        check(!VoiceSelfEchoPolicy.resemblesRecentSpeech(
            "je veux les prévisions météo pour le japon au cours de la semaine",
            tts,t+6000,t),"non-matching long phrase kept");
    }
}
"""
        with tempfile.TemporaryDirectory() as temp:
            folder=Path(temp)
            (folder/"SpeechEchoSmoke.java").write_text(smoke,encoding="utf8")
            subprocess.run(["javac","-encoding","UTF-8","-d",temp,
                str(JAVA/"VoiceSelfEchoPolicy.java"),str(folder/"SpeechEchoSmoke.java")],
                check=True,text=True,capture_output=True)
            subprocess.run(["java","-cp",temp,"SpeechEchoSmoke"],
                check=True,text=True,capture_output=True)

if __name__=="__main__": unittest.main()
