"""FRIDAY Memory+ v3.8.0 : garde-fous de confidentialité et de migration."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/fr/friday/assistant"

def java(name):
    return (JAVA / name).read_text(encoding="utf-8")

class MemoryPlusPrivacy(unittest.TestCase):
    def test_keystore_strict_without_plaintext_fallback(self):
        prefs = java("SecurePrefs.java")
        self.assertIn("putPrivateSecret", prefs)
        self.assertIn("privateSecret", prefs)
        self.assertIn("AES/GCM/NoPadding", prefs)
        self.assertIn("AndroidKeyStore", prefs)
        self.assertIn("throw new IllegalStateException(LAST_ERROR, e)", prefs)
        self.assertNotIn('putString("secret_" + name, plain)', prefs)
        self.assertIn("Mémoire non chiffrée refusée", prefs)

    def test_memory_is_offline_and_opt_in_to_mistral(self):
        memory = java("PersonalMemory.java")
        self.assertIn('cfg.get("memory_share_mistral", "0")', memory)
        self.assertIn("if (!sharingAllowed(cfg)) return", memory)
        self.assertIn("cfg.putPrivateSecret(STORE, json)", memory)
        self.assertIn("cfg.removePrivateSecret(LEGACY)", memory)
        self.assertIn("MAX_ITEMS = 200", memory)
        self.assertIn("MAX_ITEM_LENGTH = 600", memory)
        self.assertIn("MAX_CONTEXT_ITEMS = 5", memory)
        self.assertIn("MAX_CONTEXT_CHARACTERS = 1800", memory)
        self.assertNotIn("github.com", memory.lower())
        self.assertNotIn("httpconnection", memory.lower())
        self.assertIn("if (type == MemoryIntent.CLEAR)", memory)
        self.assertIn("Aucune suppression n'a été effectuée", memory)

    def test_memory_per_query_with_consent(self):
        ai = java("NativeAI.java")
        self.assertIn("PersonalMemory.context(cfg, question)", ai)
        self.assertIn('system(cfg, question)', ai)
        self.assertNotIn("PersonalMemory.context(cfg);", ai)
        activity = java("MainActivity.java")
        self.assertIn('prefs.get("memory_share_mistral", "0")', activity)
        self.assertIn('prefs.put("memory_share_mistral"', activity)
        self.assertIn("memorySearchInput", activity)
        self.assertIn("PersonalMemory.update", activity)
        self.assertIn("PersonalMemory.delete", activity)
        self.assertIn("PersonalMemory.markReviewed", activity)
        self.assertIn('setPositiveButton("Supprimer"', activity)

    def test_history_migrates_to_encryption_without_plaintext_writes(self):
        history = java("PrivateConversationHistory.java")
        service = java("FridayService.java")
        activity = java("MainActivity.java")
        self.assertIn("prefs.putPrivateSecret(KEY, json)", history)
        self.assertIn("old.edit().clear().commit()", history)
        self.assertIn("prefs.putPrivateSecret(KEY, clean.toString())", history)
        self.assertIn("PrivateConversationHistory.append(", service)
        self.assertIn("PrivateConversationHistory.read(", activity)
        self.assertIn("PrivateConversationHistory.clear(", activity)
        self.assertNotIn('putString("events", list.toString())', service)

    def test_android_backup_disabled_and_version(self):
        manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text()
        self.assertIn('android:allowBackup="false"', manifest)
        self.assertIn('android:usesCleartextTraffic="false"', manifest)
        gradle = (ROOT / "app/build.gradle").read_text()
        import re
        code = re.search(r"versionCode (\\d+)", gradle)
        self.assertIsNotNone(code)
        self.assertGreaterEqual(int(code.group(1)), 63)
        version = re.search(r"versionName '(\\d+)\\.(\\d+)\\.(\\d+)'", gradle)
        self.assertIsNotNone(version)
        self.assertGreaterEqual(tuple(map(int, version.groups())), (3, 8, 0))

if __name__ == "__main__":
    unittest.main()
