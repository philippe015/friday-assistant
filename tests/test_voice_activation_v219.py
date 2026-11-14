from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
SRC=(ROOT/'app/src/main/java/fr/friday/assistant/MainActivity.java').read_text()
VOICE=(ROOT/'app/src/main/java/fr/friday/assistant/MistralVoice.java').read_text()
HUD=(ROOT/'app/src/main/assets/index.html').read_text()
class ActivationRegression(unittest.TestCase):
    def test_activation_persisted_directly_after_recording(self):
        point=SRC.index('VoiceSampleRecorder.capture(getApplicationContext()')
        end=SRC.index('}, "friday-voice-capture").start();',point)
        code=SRC[point:end]
        self.assertIn('runOnUiThread(this::activateRecordedVoice)',code)
        activation=SRC[SRC.index('private void activateRecordedVoice()'):SRC.index('private void deleteVoiceSample()')]
        self.assertIn('VoiceCloneActivation.enable(prefs::put',activation)
        self.assertIn('clonedVoiceEnabled.setChecked(true)',activation)
        self.assertIn('voiceEnabled.setChecked(true)',activation)
        self.assertIn('refreshVoiceSampleStatus();',activation)
    def test_guarded_by_consent_and_sample(self):
        self.assertIn('voiceConsent.isChecked()',SRC)
        self.assertIn('VoiceSampleRecorder.exists(this)',SRC)
        self.assertIn('"clone_consent"',VOICE)
    def test_hud_refresh_immediately(self):
        self.assertIn('window.fridayRefreshNative = refreshNative;',HUD)
        self.assertIn('refreshVoiceModeOnHud();',SRC)
    def test_can_activate_existing_sample_without_rerecording(self):
        self.assertIn('Activer mon échantillon déjà enregistré', SRC)
        self.assertIn('this::activateRecordedVoice', SRC)
    def test_preview_no_manual_save_required(self):
        self.assertIn('if (!MistralVoice.enabled(this, prefs))', SRC)
if __name__=='__main__': unittest.main()
