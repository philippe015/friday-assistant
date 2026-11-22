package fr.friday.assistant;

import java.nio.charset.StandardCharsets;

/**
 * LiteRT-LM 0.18: maxNumTokens=2048 limits the tokenizer context, not Java
 * characters. We cannot accurately count the model-specific tokens here.
 * Constrain UTF-8 bytes before JNI, keeping the active identity/personality
 * and the CURRENT question. A stricter retry is available after a native
 * "Input token ids are too long" error. No private prompt is logged/sent.
 */
public final class LiteRtPromptBudget {
    private LiteRtPromptBudget() { }

    /** First-pass conservative proxy; native tokenizer remains authoritative. */
    public static final int INITIAL_UTF8_BYTES = 3600;
    /**
     * An UTF-8 token cannot consume fewer than one input byte. 1400 input
     * bytes leave substantial headroom under 2048 input token IDs and model
     * control tokens even for unusual punctuation/non-French text.
     */
    public static final int RETRY_UTF8_BYTES = 1400;

    public static int utf8Bytes(String text) {
        return text == null ? 0 : text.getBytes(StandardCharsets.UTF_8).length;
    }

    public static String fit(String original, int maxBytes) {
        if (original == null) return "";
        if (maxBytes < 500) throw new IllegalArgumentException("Budget trop petit");
        if (utf8Bytes(original) <= maxBytes) return original;

        final String questionMarker = "\nDemande ACTUELLE : ";
        final String closing = "\nJARVIS :";
        int questionAt = original.lastIndexOf(questionMarker);
        // All production prompts have the current-question marker.
        if (questionAt < 0) {
            return firstBytes(original, maxBytes);
        }

        String before = original.substring(0, questionAt);
        String question = original.substring(questionAt + questionMarker.length());
        if (question.endsWith(closing)) question = question.substring(
                0, question.length() - closing.length());

        int headerBudget = maxBytes <= 1800 ? 215 : 365;
        int profileBudget = maxBytes <= 1800 ? 225 : 640;
        int contextBudget = maxBytes <= 1800 ? 0 : 270;

        String header = firstBytes(before, headerBudget);
        String profile = "";
        int profileAt = before.indexOf("[PROFIL ACTIF");
        if (profileAt >= 0) {
            int profileEnd = before.indexOf("[FIN DU PROFIL ACTIF]", profileAt);
            profileEnd = profileEnd >= 0
                    ? profileEnd + "[FIN DU PROFIL ACTIF]".length()
                    : before.length();
            profile = "\n" + firstBytes(
                    before.substring(profileAt, profileEnd), profileBudget);
        }

        String dialogue = "";
        int dialogueAt = before.lastIndexOf("[CONTEXTE DE DIALOGUE PRIVÉ");
        if (contextBudget > 0 && dialogueAt >= 0)
            dialogue = "\n[CONTEXTE PRIVÉ RÉDUIT]\n"
                    + lastBytes(before.substring(dialogueAt), contextBudget);

        String marker = "\n[CONTEXTE RÉDUIT POUR LITERT-LM]\n"
                + questionMarker;
        int remainder = maxBytes - utf8Bytes(header + profile + dialogue
                + marker + closing);
        // Keep the current request first; it must never be replaced by old
        // dialogue or an inactive personality. Mark a partial Web excerpt.
        if (remainder < 180) {
            dialogue = "";
            profile = profile.isEmpty() ? "" : "\n" + firstBytes(profile, 125);
            remainder = maxBytes - utf8Bytes(header + profile + marker + closing);
        }

        String current = question;
        if (utf8Bytes(question) > remainder) {
            String lost = "\n[FIN PARTIELLE DU CONTENU : CONTEXTE LIMITE]\n";
            int available = Math.max(0, remainder - utf8Bytes(lost));
            // First and last sections protect both the user's instruction
            // and a concluding request. The missing middle is explicit.
            int front = (int) (available * 0.75);
            current = firstBytes(question, front) + lost
                    + lastBytes(question, available - front);
        }
        String result = header + profile + dialogue + marker + current + closing;
        return utf8Bytes(result) <= maxBytes ? result : firstBytes(result, maxBytes);
    }

    private static String firstBytes(String text, int limit) {
        if (text == null || limit <= 0) return "";
        int i = 0, bytes = 0;
        while (i < text.length()) {
            int cp = text.codePointAt(i);
            int size = bytesPerCodepoint(cp);
            if (bytes + size > limit) break;
            bytes += size;
            i += Character.charCount(cp);
        }
        return text.substring(0, i);
    }

    private static String lastBytes(String text, int limit) {
        if (text == null || limit <= 0) return "";
        int i = text.length(), bytes = 0;
        while (i > 0) {
            int cp = text.codePointBefore(i);
            int size = bytesPerCodepoint(cp);
            if (bytes + size > limit) break;
            bytes += size;
            i -= Character.charCount(cp);
        }
        return text.substring(i);
    }

    private static int bytesPerCodepoint(int cp) {
        if (cp <= 0x7f) return 1;
        if (cp <= 0x7ff) return 2;
        if (cp <= 0xffff) return Character.isSurrogate((char) cp) ? 1 : 3;
        return 4;
    }
}
