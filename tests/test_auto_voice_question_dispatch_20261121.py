"""Regression: auto mic opens and recognized spoken questions get dispatched.

These static and pure Java checks cannot prove physical microphone routing.
"""
from pathlib import Path
import subprocess
import tempfile
import shutil
import unittest

ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/"app/src/main/java/fr/friday/assistant"
MAIN=(SRC/"MainActivity.java").read_text(encoding="utf-8")
VOICE=(SRC/"FridayService.java").read_text(encoding="utf-8")
AUTO=(SRC/"AutoListenPolicy.java").read_text(encoding="utf-8")

class AutoMicroQuestionDispatch(unittest.TestCase):
    def test_auto_start_not_blocked_by_old_edition_modal(self):
        self.assertIn("private void startListening(boolean automatically)", MAIN)
        self.assertIn("if (!others.isEmpty() && !automatically)", MAIN)
        self.assertIn('if (automatically && !others.isEmpty())', MAIN)
        self.assertIn("startServiceUnchecked();",MAIN)
        self.assertIn("startForegroundService(i)",MAIN)
        self.assertIn("AutoListenPolicy.shouldStart(", MAIN)
        self.assertIn('prefs.get("jarvis_auto_listen_on_open", "1")', MAIN)

    def test_question_recognized_in_visible_cockpit_is_not_ignored_after_20s(self):
        self.assertIn("public static boolean isCockpitVisible()", MAIN)
        self.assertIn("cockpitVisible = true;", MAIN)
        self.assertIn("cockpitVisible = false;", MAIN)
        self.assertIn("MainActivity.isCockpitVisible()",VOICE)
        self.assertIn('AutoListenPolicy.enabled(settings.get("jarvis_auto_listen_on_open", "1"))',VOICE)
        self.assertIn("TtsEchoGate.followupAccepts(",VOICE)
        self.assertIn("now >= echoBlockUntil",VOICE)
        self.assertIn("handlePhrase(phrase.trim())",VOICE)
        self.assertIn("task.start();",VOICE)

    def test_spoken_question_is_visible_even_if_local_ai_fails(self):
        self.assertIn('event("user", question)',VOICE)
        self.assertIn('event("assistant", visibleFailure)',VOICE)
        self.assertIn('event("error", "Réponse indisponible : " + reason)',VOICE)
        self.assertIn('String visibleFailure = "J\'ai reçu votre question, mais Jarvis "',VOICE)
        self.assertIn("private void initTts()",VOICE)
        self.assertIn("PhoneAudioRoute.usePhoneSpeaker(this);",VOICE)

    def test_stop_is_manual_and_asr_only_suspended_for_tts(self):
        self.assertIn('settings.put("barge_in_enabled", "0")',VOICE)
        self.assertIn("boolean bargeEnabled = false;",VOICE)
        self.assertIn("boolean blocked = TtsEchoGate.suppressGeneralAsr(",VOICE)
        self.assertNotIn("boolean blocked = currentTurn != null",VOICE)

    def test_autostart_policy_is_default_on_but_respects_manual_stop(self):
        if not shutil.which("javac") or not shutil.which("java"):
            self.skipTest("JDK missing")
        src = r"""
import fr.friday.assistant.AutoListenPolicy;
public final class AutoMicSmoke {
    static void require(boolean ok,String reason) {
        if(!ok) throw new AssertionError(reason);
    }
    public static void main(String[] args) {
        require(AutoListenPolicy.enabled("1"),"default on");
        require(AutoListenPolicy.shouldStart("1",false,false,false),"must auto-start");
        require(!AutoListenPolicy.shouldStart("1",false,false,true),"already active");
        require(!AutoListenPolicy.shouldStart("1",true,false,false),"not twice");
        require(!AutoListenPolicy.shouldStart("1",false,true,false),"manual stop");
        require(!AutoListenPolicy.shouldStart("0",false,false,false),"explicit opt-out");
    }
}
"""
        with tempfile.TemporaryDirectory() as d:
            path=Path(d)
            (path/"AutoMicSmoke.java").write_text(src,encoding="utf-8")
            subprocess.run(["javac","-encoding","UTF-8","-d",d,
                            str(SRC/"AutoListenPolicy.java"),
                            str(path/"AutoMicSmoke.java")],capture_output=True,text=True,check=True)
            subprocess.run(["java","-cp",d,"AutoMicSmoke"],capture_output=True,text=True,check=True)

if __name__=="__main__":unittest.main()
