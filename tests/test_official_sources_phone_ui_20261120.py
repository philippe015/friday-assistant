"""Regression tests: official public sources and Android phone-first cockpit."""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"
WEB = (JAVA / "JarvisInternet.java").read_text(encoding="utf-8")
POLICY = (JAVA / "OfficialSourcePolicy.java").read_text(encoding="utf-8")
ACTIVITY = (JAVA / "MainActivity.java").read_text(encoding="utf-8")
HTML = (ROOT / "app/src/main/assets/index.html").read_text(encoding="utf-8")


class OfficialSourcesPhoneCompatibility(unittest.TestCase):
    def test_official_policy_compiles_and_is_spoof_safe(self):
        if not shutil.which("java") or not shutil.which("javac"):
            self.skipTest("JDK absent")
        smoke = r"""
            import fr.friday.assistant.OfficialSourcePolicy;
            public class OfficialPolicySmoke {
                static void yes(boolean x, String why) {
                    if (!x) throw new AssertionError(why);
                }
                public static void main(String[] args) {
                    yes(OfficialSourcePolicy.isOfficialUrl(
                        "https://www.service-public.gouv.fr/particuliers"), "gouv.fr");
                    yes(OfficialSourcePolicy.isOfficialUrl(
                        "https://www.legifrance.gouv.fr/codes"), "legifrance");
                    yes(OfficialSourcePolicy.isOfficialUrl(
                        "https://developer.android.com/about/versions/15"), "Android");
                    yes(OfficialSourcePolicy.isOfficialUrl(
                        "https://www.ameli.fr/assure"), "Ameli");
                    yes(!OfficialSourcePolicy.isOfficialUrl(
                        "https://service-public.gouv.fr.evil.example/"), "suffix spoof");
                    yes(!OfficialSourcePolicy.isOfficialUrl(
                        "https://evil-service-public.gouv.fr.example/"), "domain spoof");
                    yes(!OfficialSourcePolicy.isOfficialUrl(
                        "http://www.legifrance.gouv.fr/"), "HTTPS required");
                    yes(!OfficialSourcePolicy.isOfficialUrl(
                        "https://www.legifrance.gouv.fr@evil.example/"), "userinfo spoof");
                    yes(!OfficialSourcePolicy.isOfficialUrl(
                        "https://www.wikipedia.org/"), "community source not official");
                    yes(OfficialSourcePolicy.preferredSites("Code du travail et loi").length == 2,
                        "legal official sources");
                    yes(OfficialSourcePolicy.preferredSites("traitement et maladie").length == 2,
                        "medical official sources");
                    yes(OfficialSourcePolicy.preferredSites("application Android").length == 1,
                        "Android documentation");
                    yes(OfficialSourcePolicy.preferredSites(
                        "actualites du monde aujourd'hui").length == 0, "no official-news bias");
                    yes(OfficialSourcePolicy.sourceLabel(
                        "https://www.cnil.fr/fr").contains("institutionnel"), "source label");
                }
            }
        """
        with tempfile.TemporaryDirectory() as tmp:
            p = Path(tmp)
            (p / "OfficialPolicySmoke.java").write_text(smoke, encoding="utf-8")
            subprocess.run(["javac", "-encoding", "UTF-8", "-d", str(p),
                            str(JAVA / "OfficialSourcePolicy.java"),
                            str(p / "OfficialPolicySmoke.java")],
                           check=True, capture_output=True, text=True)
            subprocess.run(["java", "-cp", str(p), "OfficialPolicySmoke"],
                           check=True, capture_output=True, text=True)

    def test_official_search_first_and_no_false_verification(self):
        self.assertIn("for (String site : OfficialSourcePolicy.preferredSites(q))", WEB)
        self.assertLess(WEB.index("for (String site : OfficialSourcePolicy.preferredSites(q))"),
                        WEB.index("mergeRelevant(found, parseRss(fetch(\"https://www.bing.com/search?q=\""))
        self.assertIn("OfficialSourcePolicy.isOfficialUrl(candidate.url)", WEB)
        self.assertIn("OfficialSourcePolicy.sourceLabel(source.url)", WEB)
        self.assertIn("sources = research(question, cancellation);", WEB)
        self.assertNotIn("research(privateDialogue", WEB)
        self.assertIn("contenu et actualisation à vérifier", POLICY)

    def test_phone_safe_insets_and_no_settings_overlay(self):
        self.assertIn("Build.VERSION.SDK_INT >= 35", ACTIVITY)
        self.assertIn("WindowInsets.Type.systemBars()", ACTIVITY)
        self.assertIn("WindowInsets.Type.displayCutout()", ACTIVITY)
        self.assertIn("outer.requestApplyInsets()", ACTIVITY)
        self.assertIn("screenWidthDp < 520", ACTIVITY)
        self.assertIn("configScroll.setVisibility(View.GONE);", ACTIVITY)
        self.assertIn("getDisplayMetrics().heightPixels / 2", ACTIVITY)
        self.assertIn("@media(max-width:660px) and (max-height:760px)", HTML)
        self.assertIn("min-height:48px", HTML)
        self.assertIn("prefers-reduced-motion:reduce", HTML)

if __name__ == "__main__":
    unittest.main()
