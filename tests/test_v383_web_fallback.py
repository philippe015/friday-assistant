"""Régressions FRIDAY v3.8.3 : recherche Web Mistral et vie privée."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"

def source(filename):
    return (JAVA / filename).read_text(encoding="utf-8")

class WebAccessV383(unittest.TestCase):
    def test_real_conversations_search_not_chat_simulation(self):
        src = source("NativeAI.java")
        self.assertIn('MISTRAL_BASE + "/conversations"', src)
        self.assertIn('new JSONObject().put("type", "web_search")', src)
        self.assertIn('.put("store", false)', src)
        self.assertIn('if ("web_search".equals(entry.optString("name")))', src)
        self.assertIn('"tool_reference".equals(type)', src)

    def test_limited_retry_does_not_pretend_search_succeeded(self):
        native = source("NativeAI.java")
        policy = source("WebToolPolicy.java")
        self.assertIn("conversationWithFallback(cfg, key, model, input,", native)
        self.assertIn("conversationWithFallback(cfg, key, configuredModel(cfg)", native)
        self.assertIn("if (initial.references > 0", native)
        self.assertIn("WebToolPolicy.retryRejectedModel(", native)
        self.assertIn("return conversation(cfg, key, DEFAULT_MODEL, targeted, history)", native)
        self.assertIn("httpCode == 400 || httpCode == 404 || httpCode == 422", policy)
        self.assertIn("Outil Web non déclenché", native)
        self.assertIn("Aucun résultat de recherche n'a été obtenu", native)
        self.assertIn("WebToolPolicy.maySearchPublicAlternatives(reason)", native)
        self.assertIn("Je n\x27ai pas lu le contenu de cette page", native)
        self.assertIn("String safeQuestion = question.replaceAll(", native)
        self.assertIn("Collections.emptyList()", native)

    def test_local_memory_and_url_restrictions_remain(self):
        memory = source("PersonalMemory.java")
        self.assertIn('cfg.get("memory_share_mistral", "0")', memory)
        self.assertIn("cfg.putPrivateSecret(STORE, json)", memory)
        web = source("WebPageReader.java")
        self.assertIn('if (!"https".equalsIgnoreCase(uri.getScheme()))', web)
        self.assertIn("checkPublicDns(target)", web)
        self.assertIn("explainHttpStatus(code)", web)
        self.assertIn("Seules les pages HTTPS publiques", web)

    def test_version(self):
        gradle = (ROOT / "app/build.gradle").read_text(encoding="utf-8")
        self.assertRegex(gradle, r"versionCode (?:66|67|68|69|70|71|72|73|74|75|76|77|78|79|80|81|82|83|84|85|86|87|88|89|90|91|92|93|94)")
        self.assertRegex(gradle, r"versionName '(?:3\.8\.[3456789]|3\.9\.0|3\.10\.0|3\.11\.0|3\.11\.1|3\.11\.2|3\.12\.0|3\.12\.1|3\.12\.2|3\.12\.3|3\.12\.4|3\.12\.5|3\.12\.6|3\.13\.0|3\.13\.1|3\.13\.2|3\.13\.3|3\.13\.4|3\.13\.5|3\.13\.6|3\.13\.7|3\.13\.8|3\.14\.0)'")

if __name__ == "__main__":
    unittest.main()
