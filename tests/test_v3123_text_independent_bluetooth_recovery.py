"""FRIDAY v3.12.3: typed requests must work without audio, Bluetooth SCO must reconnect."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/fr/friday/assistant"
MAIN=(JAVA/"MainActivity.java").read_text(encoding="utf-8")
SERVICE=(JAVA/"FridayService.java").read_text(encoding="utf-8")
ROUTER=(JAVA/"BluetoothMicRouter.java").read_text(encoding="utf-8")
HTML=(ROOT/"app/src/main/assets/index.html").read_text(encoding="utf-8")

class WrittenInputIndependent(unittest.TestCase):
    def test_typed_question_never_needs_mic_or_vosk(self):
        typed=MAIN.split("private void sendQuestion(String question) {",1)[1].split("private void testVoice()",1)[0]
        self.assertNotIn("FridayService.isActive()",typed)
        self.assertNotIn("FridayService.ACTION_ASK_TEXT",typed)
        self.assertNotIn("Manifest.permission.RECORD_AUDIO",typed)
        self.assertNotIn("startListening()",typed)
        self.assertIn("JarvisMistralRouting.useLocal(",typed)
        self.assertIn("LocalJarvisEngine.answer(",typed)
        self.assertIn("NativeAI.answer(",typed)
        self.assertIn("new Thread(",typed)

    def test_errors_identify_selected_engine_and_answer_visible(self):
        typed=MAIN.split("private void sendQuestion(String question) {",1)[1].split("private void testVoice()",1)[0]
        self.assertIn('offline ? "Jarvis local" : "Mistral"',typed)
        self.assertIn('fault = "Erreur " + engineName',typed)
        self.assertIn('manualAnswerLabel.setText(result)',typed)
        self.assertIn('PrivateConversationHistory.append(',typed)
        self.assertIn('prefs.put("last_error", error)',typed)
        self.assertIn("window.fridayTypedReply",typed)
        self.assertIn("window.fridayTypedReply=function(question,answer)",HTML)
        self.assertIn("private final AtomicBoolean typedQuestionRunning",MAIN)

    def test_no_unintended_cloud_fallback_from_local(self):
        typed=MAIN.split("private void sendQuestion(String question) {",1)[1].split("private void testVoice()",1)[0]
        self.assertIn("if (offline) {",typed)
        self.assertIn("LocalJarvisEngine.answer(",typed)
        self.assertIn("} else {",typed)
        self.assertNotIn("catch (Exception localError)",typed)

class BluetoothRetry(unittest.TestCase):
    def test_sco_reestablished_before_new_capture(self):
        body=ROUTER.split("public boolean awaitBluetoothRoute(",1)[1].split("public void preferInput(",1)[0]
        self.assertIn("selectRoute();",body)
        self.assertIn("legacyScoRequested",body)
        self.assertIn("legacyScoState",body)
        prefer=ROUTER.split("public void preferInput(",1)[1].split("public String routedStatus(",1)[0]
        self.assertNotIn("selectRoute();",prefer)
        self.assertLess(SERVICE.index("bluetoothMicRouter.awaitBluetoothRoute(6000L)"),
                        SERVICE.index("ar = new AudioRecord(src"))

    def test_wrong_route_requires_persistent_failure(self):
        self.assertIn("wrongRouteConfirmations >= 3",SERVICE)
        self.assertIn("wrongRouteLastCheck >= 1000L",SERVICE)
        self.assertIn("bluetoothMicRouter.wrongInputRouted(ar)",SERVICE)

    def test_version(self):
        gradle=(ROOT/"app/build.gradle").read_text(encoding="utf-8")
        self.assertIn("versionCode 81",gradle)
        self.assertIn("versionName '3.12.3'",gradle)

if __name__=="__main__":
    unittest.main()
