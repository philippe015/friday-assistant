package fr.friday.assistant;

/** Stratégie de recherche Web : ne jamais confondre un accès autorisé et une recherche effectivement réalisée. */
public final class WebPolicy {
    private WebPolicy() {}
    public static String normalized(String mode) {
        if ("always".equals(mode)) return "always";
        if ("off".equals(mode)) return "off";
        return "auto";
    }
    public static boolean enabled(String mode) { return !"off".equals(normalized(mode)); }
    public static boolean shouldSearch(String mode, boolean localPlaces, boolean freshnessNeeded) {
        String policy = normalized(mode);
        return !"off".equals(policy) && ("always".equals(policy) || localPlaces || freshnessNeeded);
    }
}
