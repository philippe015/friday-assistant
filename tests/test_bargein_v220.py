import unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
JAVA=(ROOT/'app/src/main/java/fr/friday/assistant')
class InterruptIntegration(unittest.TestCase):
    def test_capture_kept_during_answer(self):
        src=(JAVA/'FridayService.java').read_text()
        assert 'new Thread(() -> answerTurn(request, turn)' in src
        assert 'handlePhrase(phrase.trim())' in src
        assert 'ar.stop(); ar.release(); recorder = null; ar = null;' not in src
    def test_interrupt_button_and_notification(self):
        service=(JAVA/'FridayService.java').read_text()
        ui=(ROOT/'app/src/main/assets/index.html').read_text()
        assert 'ACTION_INTERRUPT' in service
        assert 'interruptBtn' in ui and "call('interrupt')" in ui
    def test_micro_echo_effect_and_listen_setting(self):
        service=(JAVA/'FridayService.java').read_text()
        assert 'AcousticEchoCanceler' in service and 'VOICE_COMMUNICATION' in service
        assert 'barge_in_enabled' in service
    def test_cancel_mistral_network_and_voice(self):
        ai=(JAVA/'NativeAI.java').read_text()
        tts=(JAVA/'MistralVoice.java').read_text()
        assert 'class CancelToken' in ai and 'connection.disconnect()' in ai
        assert 'class Playback' in tts and 'playback.live(keepPlaying)' in tts
    def test_only_mistral(self):
        ai=(JAVA/'NativeAI.java').read_text()
        assert 'https://api.mistral.ai' in ai
        assert 'api.anthropic.com' not in ai and 'api.openai.com' not in ai
if __name__=='__main__': unittest.main()
