package fr.friday.assistant;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Distingue une question sur nos échanges d'une recherche documentaire.
 * Sans Android ni accès réseau : utilisable dans tous les moteurs.
 */
public final class DialogueMemoryPolicy {
    private DialogueMemoryPolicy() { }

    static String fold(String text) {
        return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.FRENCH)
                .replaceAll("[^a-z0-9 ]+", " ")
                .replaceAll("\\s+", " ").trim()
                .replaceFirst("^(?:jarvis|jarvise|jervis|jervi)\\s+", "");
    }

    /** Questions privées : ne jamais les transformer en recherche Web. */
    public static boolean isPrivateDialogue(String question) {
        String q = fold(question);
        return q.matches(".*\\b(?:nos|notre|mes)\\s+(?:dialogues?|conversations?|discussions?|echanges?)\\b.*")
                || q.matches(".*\\b(?:ce qu on|ce que nous|ce que je t ai|ce que tu m as)\\s+(?:dit|demande|raconte|explique|parle)\\b.*")
                || q.matches(".*\\b(?:tu te souviens|te souviens tu|tu te rappelles|rappelle toi|reprends notre|on parlait de|nous parlions de|de quoi parlions nous)\\b.*")
                || q.matches(".*\\b(?:historique|memoire|souvenir)\\s+(?:de |des |de nos |de notre )?(?:nos |notre |mes )?(?:dialogues?|conversations?|discussions?|echanges?)\\b.*")
                || q.matches(".*\\b(?:nos |notre )(?:dernier|derniers|derniere|dernieres)\\s+(?:sujets?|questions?|echanges?)\\b.*")
                || q.matches(".*\\b(?:tu as|as tu|avez vous|tu gardes|gardes tu)\\s+(?:une |de la |un )?(?:memoire|historique)\\s+(?:de |des |sur )?(?:nos |notre |mes )?(?:dialogues?|conversations?|discussions?|echanges?)\\b.*");
    }

    /** Capacité de mémoire, et non demande de résumer une conversation précise. */
    public static boolean asksMemoryStatus(String question) {
        String q = fold(question);
        boolean asksCapacity = q.matches(".*\\b(?:as tu|tu as|avez vous|tu gardes|gardes tu|conserves tu|peux tu|tu peux|est ce que tu as|est ce que tu gardes)\\b.*")
                && q.matches(".*\\b(?:memoire|historique|memoriser|souvenir|souvenir de)\\b.*");
        return asksCapacity && isPrivateDialogue(q)
                && !q.matches(".*\\b(?:qu est ce|qu avons|que nous|qu est ce que|resume|raconte)\\b.*");
    }

    /** Relances elliptiques à traiter avec l'historique local déjà enregistré. */
    public static boolean isContextualFollowUp(String question) {
        String q = fold(question);
        if (q.length() > 125 || q.isEmpty()) return false;
        if (q.matches(".*\\b(?:internet|web|actualites?|news|meteo|aujourd hui|maintenant|"
                + "recherche|cherche|verifie|verifier|source|sources|en ligne)\\b.*")) return false;
        if (q.matches("^(?:pourquoi|pourquoi ca|pourquoi donc|et pourquoi|"
                + "et pourquoi ca|comment ca|et ensuite|et apres|continue|"
                + "poursuis|reprends|precise|et la suite)$")) return true;
        return q.matches("^(?:developpe|developpe davantage|approfondis|"
                + "approfondis davantage|explique davantage|explique moi davantage|"
                + "explique un peu plus|donne des exemples|donne moi des exemples|"
                + "donne plus de details|peux tu detailler|peux tu approfondir|"
                + "peux tu developper|sois plus precis|"
                + "qu est ce que tu veux dire|qu est ce que tu voulais dire|"
                + "de quoi parlais tu|que veux tu dire|"
                + "et lui|et elle|et eux|et pour|et a)\\b.*")
                || (q.startsWith("et ") && q.split(" ").length <= 7);
    }

    public static boolean keepLocal(String question) {
        return isPrivateDialogue(question) || isContextualFollowUp(question);
    }
}
