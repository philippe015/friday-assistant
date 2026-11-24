"""Matrix UI smoke tests: a chat surface using the existing FRIDAY Android pipeline.

These tests intentionally never run commands on the device or store user chat
messages in a debug log; they verify bridge and UI wiring only.
"""
from html.parser import HTMLParser
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
HTML = ROOT / "app/src/main/assets/index.html"
JAVA = ROOT / "app/src/main/java/fr/friday/assistant/MainActivity.java"


class Tags(HTMLParser):
    def __init__(self):
        super().__init__()
        self.ids = []
        self.buttons = []
        self.terminal_label = False

    def handle_starttag(self, tag, attrs):
        a = dict(attrs)
        if "id" in a:
            self.ids.append(a["id"])
        if tag == "button":
            self.buttons.append(a.get("id"))


class MatrixTerminalTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.html = HTML.read_text(encoding="utf-8")
        cls.java = JAVA.read_text(encoding="utf-8")

    def test_tab_fields_and_accessible_controls(self):
        tags = Tags()
        tags.feed(self.html)
        self.assertEqual(len(tags.ids), len(set(tags.ids)), "Duplicate HTML ids")
        for name in ("tabMatrix", "matrixPage", "matrixScreen", "matrixInput",
                     "matrixSend", "matrixMic", "matrixReplay",
                     "matrixSilence", "matrixState", "matrixFullscreen"):
            self.assertIn(name, tags.ids)
        self.assertIn('aria-label="Terminal Matrix', self.html)
        self.assertIn('role="log"', self.html)
        self.assertIn('role="status"', self.html)
        self.assertIn("grid-template-columns:repeat(2,minmax(0,1fr))", self.html)

    def test_actual_matrix_green_style_and_no_command_shell(self):
        self.assertIn(".matrix-shell", self.html)
        self.assertIn("--mx:#68ff92", self.html)
        self.assertIn("#010b05", self.html)
        self.assertIn("aucune commande Linux/Android", self.html)
        self.assertIn("matrixLine", self.html)
        self.assertIn("value.textContent=String(body||'')", self.html)
        self.assertNotIn("eval(", self.html)
        self.assertNotIn("localStorage.setItem", self.html)

    def test_uses_existing_chat_pipeline_with_tts_and_encrypted_history(self):
        for code in ("@JavascriptInterface public void askMatrix(String question)",
                     "sendQuestion(question, true)",
                     "PrivateConversationHistory.append(",
                     'prefs.get("voice_enabled", "1")',
                     "playWebVoice(spokenResult)",
                     "window.fridayMatrixComplete&&window.fridayMatrixComplete("):
            self.assertIn(code, self.java)
        for code in ("call('askMatrix',q)", "drawMatrix();",
                     "window.fridayMatrixComplete=function(",
                     "call('speakNative',last)",
                     "call('stopSpeakingNative')",
                     "call('start')"):
            self.assertIn(code, self.html)
        self.assertIn("state.messages.slice(-30)", self.html)
        self.assertIn("drawMatrix();}", self.html)

    def test_native_busy_empty_and_errors_release_pending(self):
        self.assertIn("if (fromMatrixTerminal) matrixTerminalComplete(cleaned", self.java)
        self.assertIn('matrixTerminalComplete("", "",', self.java)
        self.assertIn("if (fromMatrixTerminal)\n                    matrixTerminalComplete(request", self.java)
        self.assertIn("typedQuestionRunning.compareAndSet(false, true)", self.java)

    def test_matrix_fullscreen_toggle_and_restore(self):
        for marker in (
            'id="matrixFullscreen"', 'aria-pressed="false"',
            'matrix-expanded', 'matrix-fullscreen-active',
            'height:100dvh', 'flex:1 1 auto;height:auto',
            'function setMatrixFullscreen(enabled)',
            "call('setMatrixFullscreen',next)",
            "window.fridayMatrixExitFullscreen=()=>setMatrixFullscreen(false)",
            "if(tab!=='matrix'&&state.matrixFullscreen)setMatrixFullscreen(false)",
            "e.key==='Escape'&&state.matrixFullscreen",
        ):
            self.assertIn(marker, self.html)
        for marker in (
            "private void setMatrixFullscreenMode(boolean enabled)",
            "@JavascriptInterface public void setMatrixFullscreen(boolean enabled)",
            "View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY",
            "if (matrixFullscreenActive)",
            "window.fridayMatrixExitFullscreen&&window.fridayMatrixExitFullscreen()",
        ):
            self.assertIn(marker, self.java)

    def test_true_edge_to_edge_native_fullscreen(self):
        # Do not settle for only a fullscreen HTML element inside a WebView
        # with Android's header, settings, buttons and insets still taking space.
        for marker in (
            "private LinearLayout matrixRoot;",
            "matrixRoot = outer;",
            "if (child != hud)",
            "matrixSavedNativeVisibility.put(child, child.getVisibility())",
            "child.setVisibility(View.GONE)",
            "item.getKey().setVisibility(item.getValue())",
            "if (matrixFullscreenActive) view.setPadding(0, 0, 0, 0)",
            "View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN",
            "View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION",
            "controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars())",
            "controller.show(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars())",
            "matrixRoot.requestApplyInsets()",
            "matrixRoot.requestLayout()",
            "SOFT_INPUT_ADJUST_RESIZE",
        ):
            self.assertIn(marker, self.java)
        for marker in (
            "body.matrix-fullscreen-active .app{max-width:none",
            "#matrixPage.matrix-expanded{position:fixed;inset:0",
            "width:100vw;max-width:none",
            "height:var(--matrix-visual-height,100dvh)",
            "document.documentElement.classList.toggle('matrix-fullscreen-active',next)",
            "window.fridayMatrixViewportChanged=matrixUpdateViewport",
            "window.visualViewport.addEventListener('resize',matrixUpdateViewport)",
            "↙ Mode normal",
        ):
            self.assertIn(marker, self.html)

    def test_javascript_parses(self):
        node = shutil.which("node")
        if not node:
            self.skipTest("Node.js unavailable")
        js = re.search(r"<script>\s*([\s\S]*?)\s*</script>", self.html)
        self.assertIsNotNone(js)
        with tempfile.TemporaryDirectory() as tmp:
            source = Path(tmp) / "friday-matrix.js"
            source.write_text(js.group(1), encoding="utf-8")
            result = subprocess.run([node, "--check", str(source)],
                                    capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr)

if __name__ == "__main__":
    unittest.main()
