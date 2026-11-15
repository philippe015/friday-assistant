package fr.friday.assistant;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Grammaire Vosk optionnelle : liste courte de commandes / noms utiles.
 * "[unk]" conserve un chemin de reconnaissance ouverte mais ne garantit
 * PAS la qualité d'une dictée libre ; ne jamais activer par défaut.
 */
public final class VoiceVocabulary {
    public static final int MAX_LINES = 24;
    private VoiceVocabulary() {}

    public static List<String> phrases(String text) {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        if (text == null) return new ArrayList<>();
        for (String row : text.split("\\r?\\n")) {
            String clean = Normalizer.normalize(row, Normalizer.Form.NFC)
                    .toLowerCase(Locale.FRENCH)
                    .replaceAll("[^\\p{L}0-9' -]+", " ")
                    .replaceAll("\\s+", " ").trim();
            if (clean.length() >= 2 && clean.length() <= 85)
                unique.add(clean);
            if (unique.size() >= MAX_LINES) break;
        }
        return new ArrayList<>(unique);
    }

    /** Renvoie null quand il faut conserver le décodeur Vosk normal. */
    public static String grammar(String raw) {
        List<String> phrases = phrases(raw);
        if (phrases.isEmpty()) return null;
        StringBuilder out = new StringBuilder("[\"[unk]\",\"friday\",\"stop\",\"arrete\",\"silence\",\"attends\",\"pause\",\"j ai une precision\",\"friday stop\",\"friday attends\",\"friday arrete\",\"friday j ai une precision\"");
        for (String phrase : phrases) {
            if ("friday".equals(phrase)) continue;
            out.append(",\"").append(phrase.replace("\\", "\\\\").replace("\"", "\\\"")).append("\"");
        }
        return out.append(']').toString();
    }
}
