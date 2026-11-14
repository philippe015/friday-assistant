"""Absence de requête vers une autre API de modèle, et migration de la clé précédente."""
from pathlib import Path
import unittest
R=Path(__file__).resolve().parents[1]
class ModelApiTest(unittest.TestCase):
 def test_only_allowed_model_host(self):
  code='\n'.join(p.read_text(errors='ignore') for p in (R/'app/src/main').rglob('*') if p.is_file() and p.suffix in ('.java','.html','.js'))
  self.assertNotIn('apihub.',code)
  self.assertNotIn('chatAgnes(',code)
  self.assertNotIn('keyAgnes',code)
  self.assertIn('https://api.mistral.ai/v1/conversations',code)
 def test_no_duo_mode(self):
  ui=(R/'app/src/main/assets/index.html').read_text()
  self.assertNotIn('data-mode="duo"',ui)
  self.assertNotIn('Agnès',ui)
 def test_old_secret_deleted(self):
  activity=(R/'app/src/main/java/fr/friday/assistant/MainActivity.java').read_text()
  self.assertIn('prefs.setSecret("agnes", "")',activity)
if __name__=='__main__':unittest.main()
