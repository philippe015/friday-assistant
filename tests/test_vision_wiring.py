"""La vision est native : photo via FileProvider et analyse via Mistral."""
import unittest
from pathlib import Path
import xml.etree.ElementTree as ET
R=Path(__file__).resolve().parents[1]
SRC=R/'app/src/main'
class VisionTest(unittest.TestCase):
 def test_native_fileprovider_and_mistral(self):
  activity=(SRC/'java/fr/friday/assistant/MainActivity.java').read_text()
  ai=(SRC/'java/fr/friday/assistant/NativeAI.java').read_text()
  for key in ('MediaStore.ACTION_IMAGE_CAPTURE','FileProvider.getUriForFile','visionAnswer','pendingJpeg'):
   self.assertIn(key,activity if key!='visionAnswer' else ai+activity)
  self.assertIn('data:image/jpeg;base64,',ai)
  self.assertIn('https://api.mistral.ai/v1/chat/completions',ai)
  manifest=ET.parse(SRC/'AndroidManifest.xml').getroot()
  ns='{http://schemas.android.com/apk/res/android}'
  provider=manifest.find('.//provider')
  self.assertEqual(provider.attrib[ns+'authorities'],'${applicationId}.fileprovider')
 def test_hud_vision_button(self):
  html=(SRC/'assets/index.html').read_text()
  for key in ('id="visionBtn"','visionPoll','vision(',"call('speakNative',text)"):
   self.assertIn(key,html)
if __name__=='__main__':unittest.main()
