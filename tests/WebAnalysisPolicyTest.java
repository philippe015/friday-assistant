package fr.friday.assistant;

import java.util.List;

public final class WebAnalysisPolicyTest {
    private static void ok(boolean valid, String label) {
        if (!valid) throw new AssertionError(label);
    }

    public static void main(String[] args) {
        ok(WebAnalysisPolicy.deepSearchRequested(
                "Jarvis, analyse toutes les pages web trouvées"), "deep web intent");
        ok(WebAnalysisPolicy.deepSearchRequested(
                "Compare les articles et leurs sources sur Internet"), "source compare intent");
        ok(!WebAnalysisPolicy.deepSearchRequested(
                "Quelle heure est-il ?"), "local question not deep search");
        ok(!WebAnalysisPolicy.deepSearchRequested(
                "Donne-moi les actualités de la journée"), "news headline intent unchanged");
        ok(WebAnalysisPolicy.verbatimRequested(
                "Lis cette page en entier"), "full text requested");
        ok(WebAnalysisPolicy.verbatimRequested(
                "Lire l'article intégralement"), "integral article reading");
        ok(!WebAnalysisPolicy.verbatimRequested(
                "Résume cet article"), "summary is not verbatim");
        ok(WebAnalysisPolicy.split("").isEmpty(), "empty document");

        String source = ("Français important, description de l'article. "
                + "Numéro 2026, texte complet. ").repeat(540);
        List<String> chunks = WebAnalysisPolicy.split(source);
        ok(chunks.size() > 8, "long article is segmented");
        ok(chunks.size() <= WebAnalysisPolicy.MAX_SECTIONS, "segments bounded");
        ok(WebAnalysisPolicy.fullyCovered(source, chunks), "whole public text covered");
        for (String piece : chunks)
            ok(piece.length() <= WebAnalysisPolicy.CHUNK_CHARS, "each segment bounded");
        String tooLong = source.repeat(4);
        ok(!WebAnalysisPolicy.fullyCovered(
                tooLong, WebAnalysisPolicy.split(tooLong)), "truncation is reported");
        System.out.println("WebAnalysisPolicyTest OK");
    }
}
