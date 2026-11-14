package fr.friday.assistant;

/** Transition explicite des préférences lors du succès de la capture vocale. */
public final class VoiceCloneActivation {
    @FunctionalInterface
    public interface Store { void put(String key, String value); }
    private VoiceCloneActivation() {}

    public static void enable(Store store, boolean consent, boolean recorded) {
        if (store == null) throw new IllegalArgumentException("Stockage indisponible.");
        if (!VoiceClonePolicy.hasConsent(consent, recorded))
            throw new IllegalStateException("Consentement ou échantillon vocal absent.");
        store.put("clone_consent", "1");
        store.put("clone_enabled", "1");
        store.put("voice_enabled", "1");
    }
}
