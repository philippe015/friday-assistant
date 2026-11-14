import unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'app/src/main/java/fr/friday/assistant'
class MistralOnlyTest(unittest.TestCase):
 def test_only_mistral_provider(self):
  ai=(JAVA/'NativeAI.java').read_text()
  activity=(JAVA/'MainActivity.java').read_text()
  html=(ROOT/'app/src/main/assets/index.html').read_text()
  for token in ('api.mistral.ai/v1/conversations','api.mistral.ai/v1/chat/completions','"web_search"','"store", false','visionAnswer'):
   self.assertIn(token,ai)
  self.assertNotIn('apihub.',ai)
  self.assertNotIn('chatAgnes',ai)
  self.assertNotIn('secret("agnes")',ai)
  self.assertNotIn('Agnès',html)
  self.assertNotIn('Duo',html)
  self.assertNotIn('agnesKey',activity)
 def test_web_policy_and_gps_privacy(self):
  ai=(JAVA/'NativeAI.java').read_text()
  service=(JAVA/'FridayService.java').read_text()
  resolver=(JAVA/'AndroidPlaceResolver.java').read_text()
  self.assertIn('WebPolicy.shouldSearch',ai)
  self.assertIn('location_share',service)
  self.assertIn('geocoder.getFromLocation',resolver)
  self.assertIn('CountDownLatch',resolver)
  self.assertIn('fix.accuracy <= 35',resolver)
  self.assertNotIn('GooglePlacesNearby',service)
 def test_native_personality(self):
  ai=(JAVA/'NativeAI.java').read_text()
  self.assertIn('PersonalityProfiles.activePrompt(cfg)',ai)
if __name__=='__main__':unittest.main()
