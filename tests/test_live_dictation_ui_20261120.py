"""Protect visible local ASR diagnostics and ephemeral partial speech UI."""
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"
UI = (ROOT / "app/src/main/assets/index.html").read_text(encoding="utf-8")
SERVICE = (JAVA / "FridayService.java").read_text(encoding="utf-8")
ACTIVITY = (JAVA / "MainActivity.java").read_text(encoding="utf-8")


class LiveDictationUi(unittest.TestCase):
    def test_partial_shown_as_ephemeral_user_bubble(self):
        self.assertIn("else if(kind==='error')", UI)
        self.assertIn("if(kind==='partial')", UI)
        self.assertIn("state.partial=s.slice(0,700)", UI)
        self.assertIn("dictée en cours (non enregistrée)", UI)
        self.assertIn("const items=state.messages.slice(-n);if(state.partial)", UI)
        self.assertIn("state.partial='';state.partialSequence++;", UI)
        self.assertIn("setTimeout(()=>{if(state.partialSequence===sequence)", UI)

    def test_partial_not_persisted_or_logged(self):
        self.assertIn("state.messages=events.filter", UI)
        self.assertNotIn("state.messages.push({kind:'user',text:state.partial", UI)
        self.assertNotIn("log(kind+' : '+s);", UI.split("if(kind==='partial'){")[1].split("}else if(kind==='error')")[0])
        self.assertIn('if (kind.equals("user") || kind.equals("assistant")) replayVoiceHistory();', ACTIVITY)
        self.assertIn('if (kind.equals("user") || kind.equals("assistant"))', SERVICE)

    def test_report_distinguishes_engine_and_age(self):
        self.assertIn("Moteur de reconnaissance actif : ", ACTIVITY)
        self.assertIn("Dernière activité de reconnaissance : ", ACTIVITY)
        self.assertIn("FridayService.recognitionEngine()", ACTIVITY)
        self.assertIn("lastAsrTextAt = now;", SERVICE)
        self.assertIn("reconnaissance", SERVICE.lower())
        self.assertIn("aucun texte reconnu depuis le démarrage", SERVICE)

    def test_rolling_pcm_peak_explained(self):
        self.assertIn("signalPeak = Math.max(signalPeak, level)", SERVICE)
        self.assertIn('"/100 · pic 6 s : "', SERVICE)
        self.assertIn("MicSignalMeter.level(buffer, count)", SERVICE)


if __name__ == "__main__":
    unittest.main()
