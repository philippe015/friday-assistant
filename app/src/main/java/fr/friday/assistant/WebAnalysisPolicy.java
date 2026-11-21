package fr.friday.assistant;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Analyse approfondie des pages HTTPS publiques, sans accès réseau propre.
 * Découpe déterministe : chaque segment est ensuite analysé sur le téléphone.
 */
public final class WebAnalysisPolicy {
    private WebAnalysisPolicy() { }

    /** 24 segments x 1 700 caractères : le plafond Web actuel de 36 000
     * caractères tient intégralement, sans couper silencieusement la fin. */
    public static final int CHUNK_CHARS = 1_700;
    public static final int MAX_SECTIONS = 24;
    public static final int GROUP_SIZE = 5;

    private static String folded(String question) {
        String value = Normalizer.normalize(
                question == null ? "" : question, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.FRENCH);
        return value.replaceAll("[^a-z0-9]+", " ").trim();
    }

    /** Réservé aux demandes explicites : la recherche ordinaire reste rapide. */
    public static boolean deepSearchRequested(String question) {
        String q = folded(question);
        boolean action = q.matches(".*\\b(analyse|analyser|analyses|etudie|etudier|"
                + "approfondis|approfondir|compare|comparer|lis|lire|lecture|"
                + "explique|expliquer|resume|resumer)\\b.*");
        boolean source = q.matches(".*\\b(pages?|sites?|articles?|sources?|"
                + "web|internet|liens?|resultats?)\\b.*");
        return action && source;
    }

    /** Ne déclenche une lecture mot-à-mot que si elle est expressément demandée. */
    public static boolean verbatimRequested(String question) {
        String q = folded(question);
        return q.matches(".*\\b(lis|lire|lecture|recite|reciter|affiche)\\b.*")
                && q.matches(".*\\b(entier|entiere|entierement|integral|integrale|"
                        + "integralement|complet|complete|completement|mot pour mot)\\b.*");
    }

    public static List<String> split(String raw) {
        if (raw == null || raw.trim().isEmpty()) return Collections.emptyList();
        String text = raw.trim();
        ArrayList<String> pieces = new ArrayList<>();
        int start = 0;
        while (start < text.length() && pieces.size() < MAX_SECTIONS) {
            int end = Math.min(start + CHUNK_CHARS, text.length());
            if (end < text.length()) {
                int space = text.lastIndexOf(' ', end);
                if (space >= start + CHUNK_CHARS / 2) end = space;
                if (end > start && Character.isHighSurrogate(text.charAt(end - 1)))
                    end--;
            }
            if (end <= start) end = Math.min(start + CHUNK_CHARS, text.length());
            String piece = text.substring(start, end).trim();
            if (!piece.isEmpty()) pieces.add(piece);
            start = end;
            while (start < text.length() && Character.isWhitespace(text.charAt(start)))
                start++;
        }
        return pieces;
    }

    public static boolean fullyCovered(String raw, List<String> pieces) {
        if (raw == null || raw.trim().isEmpty()) return true;
        if (pieces == null || pieces.isEmpty()) return false;
        int covered = 0;
        for (String piece : pieces) covered += piece.length();
        int nonWhitespace = raw.replaceAll("\\s+", "").length();
        int piecesNonWhitespace = 0;
        for (String piece : pieces)
            piecesNonWhitespace += piece.replaceAll("\\s+", "").length();
        return piecesNonWhitespace == nonWhitespace;
    }
}
