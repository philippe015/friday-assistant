"""Régression v3.14.3 : prompts de profil effectivement transmis à Gemma local."""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"

class ProfilePromptAppliedV3143(unittest.TestCase):
    def test_ui_save_read_and_test_are_wired(self):
        html = (ROOT/"app/src/main/assets/index.html").read_text(encoding="utf-8")
        native = (JAVA/"MainActivity.java").read_text(encoding="utf-8")
        profiles = (JAVA/"PersonalityProfiles.java").read_text(encoding="utf-8")
        self.assertIn('id="personaSave"', html)
        self.assertIn('id="personaTest"', html)
        self.assertIn("function saveProfiles()", html)
        self.assertIn("function testProfile()", html)
        self.assertIn("savePersonalityProfiles", html)
        self.assertIn("getPersonalityProfiles", html)
        self.assertIn("PersonalityProfiles.save(prefs, json)", native)
        self.assertIn("PersonalityProfiles.read(prefs)", native)
        self.assertIn("prefs.setSecret(KEY, expected)", profiles)
        self.assertIn("PersonalityProfiles.activePrompt(cfg)", (JAVA/"LocalJarvisEngine.java").read_text(encoding="utf-8"))
        self.assertIn("ProfilePromptPolicy.localInstructions(", (JAVA/"LocalJarvisEngine.java").read_text(encoding="utf-8"))

    def test_apk_preview_keeps_existing_installation(self):
        gradle=(ROOT/"app/build.gradle").read_text(encoding="utf-8")
        self.assertIn("versionName '3.14.18'",gradle)
        self.assertIn("applicationIdSuffix '.v31418voicefix'",gradle)
        self.assertIn("com.google.ai.edge.litertlm:litertlm-android",gradle)

    def test_java_prompt_limits_and_user_personality(self):
        if not shutil.which("javac") or not shutil.which("java"):
            self.skipTest("Java JDK unavailable")
        smoke = r'''
import fr.friday.assistant.ProfilePromptPolicy;
public class ProfilePromptSmoke {
    static void yes(boolean condition,String msg) {
        if (!condition) throw new AssertionError(msg);
    }
    public static void main(String[] args) {
        String custom="Réponds toujours en français. Appelle-toi Atlas.";
        String applied=ProfilePromptPolicy.localInstructions("Atlas",custom);
        yes(applied.contains("Nom du profil : Atlas"),"selected name");
        yes(applied.contains(custom),"actual personal prompt forwarded");
        yes(applied.contains("FIN DU PROFIL ACTIF"),"prompt boundaries");
        yes(ProfilePromptPolicy.effectiveText(null).isEmpty(),"null prompt");
        yes(ProfilePromptPolicy.effectiveText("\u0000A\nB").equals("A\nB"),"sanitized controls");
        String longPrompt="X".repeat(3300);
        yes(ProfilePromptPolicy.effectiveText(longPrompt).length()==3200,"context bound");
        yes(ProfilePromptPolicy.truncated(longPrompt),"truncation reported");
        yes(!ProfilePromptPolicy.truncated(custom),"valid input not truncated");
        yes(ProfilePromptPolicy.effectiveText("🌍".repeat(1601)).length()==3200,
            "Unicode surrogate pair not split");
    }
}
'''
        with tempfile.TemporaryDirectory() as directory:
            path=Path(directory)
            (path/"ProfilePromptSmoke.java").write_text(smoke,encoding="utf-8")
            subprocess.run(["javac","-encoding","UTF-8","-d",directory,
                str(JAVA/"ProfilePromptPolicy.java"),str(path/"ProfilePromptSmoke.java")],
                capture_output=True,text=True,check=True)
            subprocess.run(["java","-cp",directory,"ProfilePromptSmoke"],
                capture_output=True,text=True,check=True)

if __name__ == "__main__":
    unittest.main()
