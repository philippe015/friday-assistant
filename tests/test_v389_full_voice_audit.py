"""FRIDAY 3.8.9 — global microphone, Web, TTS and privacy regression audit.

These checks inspect Android integration code. Real microphone/acoustic behavior
still needs a device test; the pure-Java state machine is tested separately.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"
def code(name):
    return (JAVA / name).read_text(encoding="utf-8")

class FullVoiceAuditV389(unittest.TestCase):
    def test_network_wait_keeps_vosk_listening_tts_blocks_echo_only(self):
        service = code("FridayService.java")
        gate = code("TtsEchoGate.java")
        self.assertIn("boolean blocked = TtsEchoGate.suppressGeneralAsr(\n                                speakingNow || externalSpeechActive, asrNow,\n                                Math.max(echoBlockUntil, externalSpeechUntil))", service)
        self.assertNotIn("boolean blocked = currentTurn != null", service)
        self.assertIn("if (blocked) {", service)
        self.assertIn("recognizer.close(); recognizer = createRecognizer(model);", service)
        self.assertIn("Question entendue pendant le traitement précédent", service)
        self.assertIn("boolean bargeEnabled = false;", service)
        self.assertIn("return voicePlaybackActive || nowMs < untilMs;", gate)
        self.assertIn("!voicePlaybackActive", gate)

    def test_android_mute_recognizer_reset_once_per_transition(self):
        service = code("FridayService.java")
        start = service.index("if (silencedByPolicy.get())")
        end = service.index("// Affiche uniquement une mesure", start)
        area = service[start:end]
        self.assertIn("if (mutedSince == 0)", area)
        self.assertIn("recognizer.close(); recognizer = createRecognizer(model);", area)
        self.assertEqual(area.count("if (mutedSince == 0)"), 1)
        self.assertNotIn("mutedMicroReopens < 2", area)
        self.assertIn("if (mutedAt >= nextMuteNoticeAt)", area)
        self.assertIn("nextMuteNoticeAt = mutedAt + 30000L", area)
        self.assertIn("mutedMicroReopens == 0", service)
        self.assertIn("MediaRecorder.AudioSource.VOICE_RECOGNITION", service)
        self.assertIn('voiceState("Micro restauré : dites Jarvis")', service)

    def test_network_watchdog_allows_new_question_and_avoids_cutting_tts(self):
        service = code("FridayService.java")
        self.assertIn("MAX_PENDING_WEB_MS = 150000L", service)
        self.assertIn("requestWatchdog.postDelayed(turn.watchdog, MAX_PENDING_WEB_MS)", service)
        self.assertIn("TtsEchoGate.staleNetworkTurn(", service)
        self.assertIn("requestWatchdog.removeCallbacks(turn.watchdog)", service)
        self.assertIn("requestWatchdog.removeCallbacksAndMessages(null)", service)
        self.assertIn("interruptResponse(", service)
        self.assertIn("if (!speak(notice, turn)) recoverTtsAsync()", service)
        self.assertIn("Le moteur sélectionné n'a pas répondu à cette question", service)

    def test_service_continuous_and_error_reporting(self):
        service = code("FridayService.java")
        ui = code("MainActivity.java")
        self.assertIn('settings.get("listen_once", "0")', service)
        self.assertIn('prefs.get("listen_once", "0")', ui)
        self.assertIn("FridayService.voiceReadiness()", ui)
        self.assertIn("FridayService.lastTtsStatus()", ui)
        self.assertIn("showVoiceDiagnostics()", ui)
        self.assertIn("Diagnostic complet · micro / Vosk / Jarvis / voix", ui)
        self.assertIn('newPlainText(\n                                "friday-diagnostic-sans-secrets"', ui)
        self.assertIn("Aucune clé, aucun GPS, aucun contenu de conversation", ui)

    def test_web_privacy_and_previous_fixes(self):
        ai = code("NativeAI.java")
        service = code("FridayService.java")
        self.assertIn('java.net.URI.create(page.url).getHost()', ai)
        self.assertIn('MISTRAL_BASE + "/conversations"', ai)
        self.assertIn('new JSONObject().put("type", "web_search")', ai)
        self.assertIn('RecentNewsPolicy.requiresRecentNews(prepared)', service)
        self.assertIn("recoverTtsAsync();", service)
        self.assertIn("TtsEchoGate.afterVoice(finishedAt)", service)
        mem = code("PersonalMemory.java")
        self.assertIn('cfg.get("memory_share_mistral", "0")', mem)

    def test_version_manifest_permissions_and_no_new_external_provider(self):
        gradle = (ROOT / "app/build.gradle").read_text(encoding="utf-8")
        self.assertIn("versionCode 112", gradle)
        self.assertIn("versionName '3.14.18'", gradle)
        manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        for name in ("android.permission.INTERNET", "android.permission.RECORD_AUDIO",
                     "android.permission.FOREGROUND_SERVICE_MICROPHONE"):
            self.assertIn(name, manifest)
        self.assertNotIn("android:allowBackup=\"true\"", manifest)

if __name__ == "__main__":
    unittest.main()
