package fr.friday.assistant;

/**
 * Commandes vocales courtes, séparées de la dictée libre Vosk.
 *
 * Utilisé seulement lorsque FRIDAY génère / prononce une réponse. Les résultats
 * partiels avec mot-clé doivent être confirmés ; les ordres isolés sans
 * « Jarvis » attendent une fin de phrase afin d'éviter un faux STOP après
 * deux trames identiques provenant du haut-parleur.
 */
public final class VoiceStopSpotter {
    public enum Action { NONE, STOP, CLARIFY, STOP_MIC }

    /** JSON échappé et constant : pas d'injection du prompt ni du profil utilisateur. */
    public static final String GRAMMAR =
            "[\"[unk]\",\"stop\",\"stoppe\",\"arrete\",\"arrête\",\"silence\","
          + "\"attends\",\"pause\",\"tais toi\",\"arrete de parler\","
          + "\"j ai une precision\",\"j'ai une précision\",\"une precision\","
          + "\"je veux preciser\",\"je voudrais preciser\","
          + "\"jarvis stop\",\"jarvis arrete\",\"jarvis arrête\","
          + "\"jarvis attends\",\"jarvis silence\",\"jarvis j ai une precision\","
          + "\"jarvis j'ai une précision\",\"jarvis arrete l ecoute\","
          + "\"stop stop\",\"stop stop stop\",\"stop stop stop stop\","
          + "\"arrete arrete\",\"arrete arrete arrete\","
          + "\"jarvis stop stop\",\"jarvis stop stop stop\"]";

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

    /** Les partiels sans mot-clé ne doivent jamais couper la lecture. */
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
        // La répétition est déjà une confirmation volontaire dans un même
        // résultat Vosk : « stop stop stop » n'a pas à attendre deux callbacks.
        String normalized = VoiceText.normalize(recognized)
                .replaceAll("[^a-z0-9 ]+", " ").replaceAll("\\s+", " ").trim();
        boolean repeated = choice == Action.STOP && normalized.matches(
                ".*\\b(stop|stoppe|arrete|silence|pause|attends)\\s+(stop|stoppe|arrete|silence|pause|attends)\\b.*");
        if (finalResult || repeated
                || (BargeInPolicy.startsWithWake(recognized) && confirmations >= 2)) {
            reset();
            return choice;
        }
        return Action.NONE;
    }
}
