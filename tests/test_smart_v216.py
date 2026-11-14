import unittest
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'app/src/main/java/fr/friday/assistant'

class TestSmartVersion(unittest.TestCase):
    def test_memory_only_on_explicit_command(self):
        svc = (JAVA/'FridayService.java').read_text()
        self.assertIn('MemoryIntent.kind(question)', svc)
        self.assertIn('PersonalMemory.command(settings, question)', svc)
        self.assertIn('!privateLocation && !memoryAction', svc)
        self.assertIn('if (!memoryAction && live(turn)) event("user", question)', svc)
    def test_opt_in_memory(self):
        cfg = (JAVA/'PersonalMemory.java').read_text()
        self.assertIn('cfg.get("memory_enabled", "0")', cfg)
        self.assertIn('cfg.setSecret(KEY', cfg)
        self.assertIn('MAX_ITEMS = 12', cfg)
    def test_deep_mode_and_mistral_only(self):
        ai = (JAVA/'NativeAI.java').read_text()
        self.assertIn('reasoning_effort', ai)
        self.assertIn('mistral-small-latest', ai)
        self.assertIn('PersonalMemory.context(cfg)', ai)
        self.assertNotIn('api.agnes', ai.lower())
    def test_user_controls(self):
        ui = (JAVA/'MainActivity.java').read_text()
        self.assertIn('Activer ma mémoire chiffrée', ui)
        self.assertIn('Enregistrer les notes de mémoire', ui)
        self.assertIn('Raisonnement approfondi', ui)
        self.assertIn('setPositiveButton("Effacer"', ui)

if __name__ == '__main__': unittest.main()
