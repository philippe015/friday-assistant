package fr.friday.assistant;

import java.util.ArrayList;
import java.util.List;

/** Reconstitution de phrases vocales depuis les fragments texte SSE. */
public final class SpeechSentenceBuffer {
    private final StringBuilder pending = new StringBuilder();

    public List<String> accept(String delta) {
        if (delta != null && !delta.isEmpty()) pending.append(delta);
        ArrayList<String> out = new ArrayList<>();
        // Ne pas couper « 12.5 » ou un point à la fin d'un fragment SSE.
        for (int i = 1; i < pending.length(); i++) {
            char prev = pending.charAt(i - 1);
            char next = pending.charAt(i);
            if ((prev == '.' || prev == '?' || prev == '!' || prev == '\n')
                    && (Character.isWhitespace(next) || next == '"' || next == '»')) {
                String part = pending.substring(0, i).trim();
                if (!part.isEmpty()) out.add(part);
                pending.delete(0, i);
                i = 0;
            }
        }
        while (pending.length() > 230) {
            int cut = pending.lastIndexOf(" ", Math.min(190, pending.length()-1));
            if (cut < 90) cut = Math.min(190, pending.length());
            String part = pending.substring(0, cut).trim();
            if (!part.isEmpty()) out.add(part);
            pending.delete(0, cut);
        }
        return out;
    }

    public String flush() {
        String text = pending.toString().trim();
        pending.setLength(0);
        return text;
    }

    /** Regroupe les lignes data: multi-lignes d'un événement SSE. */
    public static final class EventLines {
        private final StringBuilder data = new StringBuilder();
        public String accept(String line) {
            if (line == null || line.isEmpty()) {
                if (data.length() == 0) return null;
                String event = data.toString();
                data.setLength(0);
                return event;
            }
            if (line.startsWith("data:")) {
                if (data.length() > 0) data.append('\n');
                data.append(line.substring(5).trim());
            }
            return null;
        }
        public String flush() { return accept(""); }
    }
}
