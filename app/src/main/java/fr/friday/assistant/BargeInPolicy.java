package fr.friday.assistant;

/** Distinction entre arrêt du service, interruption d'une réponse et question de suivi. */
public final class BargeInPolicy {
    private BargeInPolicy() {}
    public static boolean startsWithWake(String phrase) {
        String s = VoiceText.normalize(phrase).trim();
        return s.matches("^(friday|frida|freiday|freday|fride|vendredi)\\b.*");
    }
    public static boolean isStopListening(String phrase) {
        if (!startsWithWake(phrase)) return false;
        String s = VoiceText.normalize(VoiceText.withoutWakeWord(phrase));
        return s.matches(".*(arrete|stoppe|coupe|desactive|ferme) (l.ecoute|le micro|le microphone|le service|completement).*" )
            || s.equals("eteins toi") || s.equals("arrete friday");
    }
    public static boolean isStopAnswer(String phrase) {
        if (!startsWithWake(phrase) || wantsToAddDetails(phrase)) return false;
        String s = VoiceText.normalize(VoiceText.withoutWakeWord(phrase)).replaceAll("[.,!?;:]", " ").trim();
        return s.matches("(stop|stoppe|arrete|tais toi|silence|pause|attends|arrete de parler|coupe la voix|interromps|interromps toi|stop la reponse|arrete la reponse)(\\s.*)?");
    }
    /** L'utilisateur veut ajouter un contexte, pas couper définitivement la réponse. */
    public static boolean wantsToAddDetails(String phrase) {
        String s = VoiceText.normalize(VoiceText.withoutWakeWord(phrase))
                .replaceAll("[^a-z0-9 ]+", " ").replaceAll("\\s+", " ").trim();
        return s.matches("(?:(?:attends|pause|arrete|stop|interromps)(?: un instant| deux secondes| moi)? )?"
                + "(?:j ai (?:une|des) (?:precision|precisions|question|details|correction)"
                + "|je (?:veux|voudrais|souhaite) (?:preciser|ajouter|corriger)"
                + "|je veux ajouter quelque chose|laisse moi preciser|une precision|une question)"
                + "(?: s il te plait)?");
    }
    /** Exemples : « et demain », « pour Paris », « je voulais dire samedi ». */
    public static boolean contextualFollowUp(String phrase) {
        String s = VoiceText.normalize(VoiceText.withoutWakeWord(phrase))
                .replaceAll("[^a-z0-9 ]+", " ").replaceAll("\\s+", " ").trim();
        if (s.isEmpty()) return false;
        return s.matches("^(?:et|mais|pour|plutot|non|je voulais dire|je veux dire|"
                + "en fait|c est|ce serait|au sujet de|a propos de|par rapport a|"
                + "ma precision|avec|sans|demain|hier|la semaine prochaine)\\b.*")
                || (s.split(" ").length <= 5
                    && !s.matches("^(?:qui|comment|quelle|quel|pourquoi|qu est ce que)\\b.*"));
    }
    public static boolean isWakeOnly(String phrase) {
        return startsWithWake(phrase) && VoiceText.withoutWakeWord(phrase).trim().isEmpty();
    }
    /** Une interruption vocale au milieu d'une réponse impose le mot-clé AU DÉBUT. */
    public static boolean canBarge(String phrase, String assistantSpeech) {
        return startsWithWake(phrase) && !looksLikeEcho(phrase, assistantSpeech);
    }
    public static boolean looksLikeEcho(String phrase, String assistantSpeech) {
        String s = VoiceText.normalize(phrase).replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
        String a = VoiceText.normalize(assistantSpeech).replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
        if (a.length() < 14) return false;
        // Courtes commandes répétées à voix haute par Friday : évite l'auto-arrêt.
        if (s.length() >= 7 && startsWithWake(phrase) && a.contains(s)) return true;
        if (s.length() < 14) return false;
        return a.contains(s) || (startsWithWake(phrase) &&
            VoiceText.withoutWakeWord(phrase).trim().length() >= 14 &&
            a.contains(VoiceText.normalize(VoiceText.withoutWakeWord(phrase)).replaceAll("[^a-z0-9 ]", " ").trim()));
    }
    /** Au Vosk partiel : arrêt précoce seulement sur commandes explicites ou question assez longue. */
    public static boolean earlyBarge(String partial, String assistantSpeech) {
        if (!canBarge(partial, assistantSpeech)) return false;
        if (isStopAnswer(partial) || isStopListening(partial) || wantsToAddDetails(partial)) return true;
        String words = VoiceText.withoutWakeWord(partial).trim();
        return !words.isEmpty() && words.split("\\s+").length >= 3;
    }
}
