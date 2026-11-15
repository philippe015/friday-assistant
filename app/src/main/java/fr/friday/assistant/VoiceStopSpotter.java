package fr.friday.assistant;

/**
 * Commandes vocales courtes, séparées de la dictée libre Vosk.
 *
 * Utilisé seulement lorsque FRIDAY génère / prononce une réponse. Les résultats
 * partiels doivent être confirmés pour réduire les fausses alertes causées par
 * les haut-parleurs ; un résultat final peut être traité immédiatement.
 */
public final class VoiceStopSpotter {
    public enum Action { NONE, STOP, CLARIFY, STOP_MIC }

    /** JSON échappé et constant : pas d'injection du prompt ni du profil utilisateur. */
    public static final String GRAMMAR =
            "[\"[unk]\",\"stop\",\"stoppe\",\"arrete\",\"arrête\",\"silence\","
          + "\"attends\",\"pause\",\"tais toi\",\"arrete de parler\","
          + "\"j ai une precision\",\"j'ai une précision\",\"une precision\","
          + "\"je veux preciser\",\"je voudrais preciser\","
          + "\"friday stop\",\"friday arrete\",\"friday arrête\","
          + "\"friday attends\",\"friday silence\",\"friday j ai une precision\","
          + "\"friday j'ai une précision\",\"friday arrete l ecoute\"]";

    private Action last = Action.NONE;
    private int confirmations = 0;

    public void reset() {
        last = Action.NONE;
        confirmations = 0;
    }

    public static Action classify(String recognized, String speech) {
        if (recognized == null || recognized.trim().isEmpty()) return Action.NONE;
        if (BargeInPolicy.isStopListening(recognized)) return Action.STOP_MIC;
        if (BargeInPolicy.wantsToAddDetails(recognized)) {
            if (BargeInPolicy.startsWithWake(recognized)
                    ? !BargeInPolicy.looksLikeEcho(recognized, speech)
                    : BargeInPolicy.explicitControlWhileSpeaking(recognized, speech)) {
                return Action.CLARIFY;
            }
            return Action.NONE;
        }
        if (BargeInPolicy.isStopAnswer(recognized)) {
            return !BargeInPolicy.looksLikeEcho(recognized, speech) ? Action.STOP : Action.NONE;
        }
        return BargeInPolicy.explicitControlWhileSpeaking(recognized, speech)
                ? Action.STOP : Action.NONE;
    }

    /** Double observation au Vosk partiel, résultat final immédiatement. */
    public Action accept(String recognized, String speech, boolean finalResult) {
        Action choice = classify(recognized, speech);
        if (choice == Action.NONE) {
            reset();
            return Action.NONE;
        }
        if (choice != last) {
            last = choice;
            confirmations = 1;
        } else {
            confirmations++;
        }
        if (finalResult || confirmations >= 2) {
            reset();
            return choice;
        }
        return Action.NONE;
    }
}
