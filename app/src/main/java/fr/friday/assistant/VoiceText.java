package fr.friday.assistant;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Règles testables de dictée, mot-clé et lecture vocale. Aucune dépendance Android. */
public final class VoiceText {
    private static final Pattern WAKE = Pattern.compile("(?:^|\\b)(?:jarvis|jarvise|jarvice|jervis|jervi)(?:\\b)");
    private VoiceText() { }

    public static String normalize(String text) {
        return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.FRENCH);
    }

    public static boolean hasWakeWord(String heard) { return WAKE.matcher(normalize(heard)).find(); }
    public static String withoutWakeWord(String heard) {
        if (heard == null) return "";
        String clean = heard.trim();
        Matcher found = WAKE.matcher(normalize(clean));
        // Ne retirer le mot d'activation qu'en début de phrase.
        // « Qui est Jarvis ? » reste une question entière, y compris par écrit.
        if (!found.lookingAt()) return clean;
        return clean.substring(Math.min(clean.length(), found.end()))
                .replaceFirst("^[\\s,.:;!?—-]+", "").trim();
    }

    /** Ajoute un segment final en éliminant les éventuels mots chevauchants. */
    public static String join(String left, String right) {
        left = left == null ? "" : left.trim();
        right = right == null ? "" : right.trim();
        if (left.isEmpty()) return right;
        if (right.isEmpty()) return left;
        String a = normalize(left), b = normalize(right);
        if (a.equals(b) || a.endsWith(" " + b)) return left;
        String[] aw = a.split("\\s+"), bw = b.split("\\s+");
        String[] actual = right.split("\\s+");
        int overlap = 0;
        for (int n = 1; n <= Math.min(aw.length, bw.length); n++) {
            boolean equal = true;
            for (int i=0; i<n; i++) if (!aw[aw.length-n+i].equals(bw[i])) {equal=false;break;}
            if (equal) overlap=n;
        }
        if (overlap >= actual.length) return left;
        StringBuilder out = new StringBuilder(left);
        for (int i=overlap; i<actual.length; i++) out.append(' ').append(actual[i]);
        return out.toString();
    }

    /** Découpe les phrases sans tronquer les mots ni perdre le texte prononcé. */
    public static List<String> ttsChunks(String text, int maximum) {
        List<String> out = new ArrayList<>();
        if (text == null || maximum < 20) return out;
        String clean = text.replaceAll("(?m)https?://\\S+", " lien dans l'historique ")
                .replaceAll("[`*_#|>]", " ").replaceAll("\\s+", " ").trim();
        int pos = 0;
        while (pos < clean.length()) {
            int end = Math.min(clean.length(), pos + maximum);
            if (end < clean.length()) {
                int split = clean.lastIndexOf(' ', end);
                if (split > pos + maximum/3) end = split;
            }
            String part = clean.substring(pos, end).trim();
            if (!part.isEmpty()) out.add(part);
            pos = end;
            while (pos < clean.length() && clean.charAt(pos)==' ') pos++;
        }
        return out;
    }
}
