"""Régression JARVIS 3.14.6 : profil permanent et analyse publique approfondie."""
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"


def src(name):
    return (JAVA / name).read_text(encoding="utf-8")


class JarvisDeepWebRegression(unittest.TestCase):
    def test_local_model_refreshes_active_profile_and_identity(self):
        model = src("LocalJarvisEngine.java")
        self.assertIn("PersonalityProfiles.activeName(cfg)", model)
        self.assertIn("PersonalityProfiles.activePrompt(cfg)", model)
        self.assertIn("AssistantIdentity.displayName(activeName, activePrompt)", model)
        self.assertIn("ProfilePromptPolicy.localInstructions(activeName, activePrompt)", model)
        self.assertIn("privateDialogue", model)

    def test_explicit_https_page_read_deeply(self):
        reader = src("WebPageReader.java")
        internet = src("JarvisInternet.java")
        self.assertIn("public static Page readDeep(String url)", reader)
        self.assertIn("MAX_DEEP_BYTES = 1_500_000", reader)
        self.assertIn("MAX_DEEP_TEXT = 36_000", reader)
        self.assertIn("checkedUri(url)", reader)
        self.assertIn("checkPublicDns(target)", reader)
        self.assertIn("setInstanceFollowRedirects(false)", reader)
        self.assertIn("readAndAnalyseDirectUrl", internet)
        self.assertIn("WebPageReader.readDeep(url)", internet)
        self.assertIn("WebAnalysisPolicy.verbatimRequested(question)", internet)

    def test_hierarchical_analysis_keeps_every_group(self):
        internet = src("JarvisInternet.java")
        policy = src("WebAnalysisPolicy.java")
        self.assertIn("WebAnalysisPolicy.split(articleText)", internet)
        self.assertIn("groupSummaries.add(", internet)
        self.assertIn("for (int i = 0; i < groupSummaries.size(); i++)", internet)
        self.assertIn("WebAnalysisPolicy.fullyCovered(articleText, parts)", internet)
        self.assertIn("MAX_SECTIONS = 24", policy)
        self.assertIn("GROUP_SIZE = 5", policy)
        self.assertIn("analyseDeepSearchResults(", internet)

    def test_private_information_not_sent_to_public_search(self):
        internet = src("JarvisInternet.java")
        model = src("LocalJarvisEngine.java")
        self.assertNotIn("api.mistral.ai", internet)
        self.assertNotIn("PersonalMemory.search(", internet)
        self.assertNotIn("FridayLocation.", internet)
        self.assertIn("buildLocalPrompt(cfg, cleaned, privateDialogue)", model)
        self.assertIn("WebPageReader.checkedUri", internet)

    def test_version_and_voice_not_rewritten(self):
        gradle = (ROOT / "app/build.gradle").read_text(encoding="utf-8")
        self.assertIn("versionName '3.14.13'", gradle)
        self.assertIn("applicationIdSuffix '.readerfixpreview'", gradle)
        self.assertTrue((JAVA / "FridayService.java").exists())


if __name__ == "__main__":
    unittest.main()
