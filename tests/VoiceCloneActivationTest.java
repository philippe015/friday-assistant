import fr.friday.assistant.VoiceCloneActivation;
import java.util.HashMap;
import java.util.Map;
public class VoiceCloneActivationTest {
    static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        Map<String,String> store = new HashMap<>();
        try {
            VoiceCloneActivation.enable(store::put, false, true);
            throw new AssertionError("consent required");
        } catch (IllegalStateException expected) { }
        require(store.isEmpty(), "must not save when consent missing");
        try {
            VoiceCloneActivation.enable(store::put, true, false);
            throw new AssertionError("sample required");
        } catch (IllegalStateException expected) { }
        require(store.isEmpty(), "must not save when sample missing");
        VoiceCloneActivation.enable(store::put, true, true);
        require("1".equals(store.get("clone_consent")), "consent stored");
        require("1".equals(store.get("clone_enabled")), "clone persists");
        require("1".equals(store.get("voice_enabled")), "speech enabled");
        System.out.println("VoiceCloneActivationTest OK");
    }
}
