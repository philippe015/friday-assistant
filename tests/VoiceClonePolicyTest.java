import fr.friday.assistant.VoiceClonePolicy;
public class VoiceClonePolicyTest {
    public static void main(String[] args) {
        if (!VoiceClonePolicy.ENDPOINT.equals("https://api.mistral.ai/v1/audio/speech")) throw new AssertionError("endpoint");
        if (!VoiceClonePolicy.MODEL.equals("voxtral-mini-tts-3-0")) throw new AssertionError("model");
        if (VoiceClonePolicy.hasConsent(false,true)) throw new AssertionError("consent");
        if (VoiceClonePolicy.hasConsent(true,false)) throw new AssertionError("sample");
        if (!VoiceClonePolicy.hasConsent(true,true)) throw new AssertionError("valid");
        if (VoiceClonePolicy.isValidWavSize(50)) throw new AssertionError("too short");
        if (!VoiceClonePolicy.isValidWavSize(384044)) throw new AssertionError("wav valid");
        if (VoiceClonePolicy.chunks("Bonjour. " .repeat(200)).size() < 3) throw new AssertionError("chunks");
        System.out.println("VoiceClonePolicyTest OK");
    }
}
