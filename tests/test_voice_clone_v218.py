from pathlib import Path
import unittest
BASE=Path(__file__).resolve().parents[1]
JAVA=BASE/'app/src/main/java/fr/friday/assistant'
class VoiceCloneTest(unittest.TestCase):
    def test_mistral_only_and_reference(self):
        code=(JAVA/'MistralVoice.java').read_text()
        assert 'api.mistral.ai/v1/audio/speech' in (JAVA/'VoiceClonePolicy.java').read_text()
        self.assertIn('"ref_audio"', code)
        self.assertIn('"audio_data"', code)
        self.assertIn('"response_format", "wav"', code)
        self.assertNotIn('elevenlabs', code.lower())
    def test_wav_capture(self):
        code=(JAVA/'VoiceSampleRecorder.java').read_text()
        self.assertIn('"RIFF"', code)
        self.assertIn('"WAVEfmt "', code)
        self.assertIn('MediaRecorder.AudioSource.MIC', code)
        self.assertIn('getFilesDir()',code)
    def test_mobile_privacy_and_fallback(self):
        ui=(JAVA/'MainActivity.java').read_text()
        svc=(JAVA/'FridayService.java').read_text()
        self.assertIn('voiceConsent.isChecked()',ui)
        self.assertIn('VoiceSampleRecorder.erase(this)',ui)
        self.assertIn('MistralVoice.speak', svc)
        self.assertIn('repli Android', svc)
if __name__=='__main__': unittest.main()
