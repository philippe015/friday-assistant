package fr.friday.assistant;

import java.util.Locale;

/**
 * Safe offline arithmetic tool ported from OpenJarvis-style tool dispatch.
 * No scripts, shell, arbitrary Java reflection or external network access.
 */
public final class LocalJarvisArithmetic {
    private LocalJarvisArithmetic() { }

    public static String tryAnswer(String question) {
        if (question == null) return null;
        String input = question.trim().toLowerCase(Locale.FRENCH)
                .replaceFirst("^(?:friday|jarvis|open ?jarvis)[\\s,.:;!?-]*", "")
                .replaceFirst("^(?:calcule|calculer|calcules|combien font|combien fait|quelle est la valeur de)\\s+", "");
        if (input.equals(question.trim().toLowerCase(Locale.FRENCH))) return null;
        // Vosk retranscrit souvent les opérateurs et chiffres en toutes lettres.
        input = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        String[] words = {"zero","un","deux","trois","quatre","cinq","six",
                "sept","huit","neuf","dix","onze","douze","treize","quatorze",
                "quinze","seize","dix-sept","dix-huit","dix-neuf","vingt"};
        for (int i = words.length - 1; i >= 0; i--)
            input = input.replaceAll("(?iu)\\b" + words[i] + "\\b", String.valueOf(i));
        input = input.replaceAll("(?iu)\\b(?:fois|multiplie par|multiplie avec)\\b", "*")
                .replaceAll("(?iu)\\b(?:divise par|sur)\\b", "/")
                .replaceAll("(?iu)\\bplus\\b", "+")
                .replaceAll("(?iu)\\bmoins\\b", "-")
                .replace(',', '.').replaceAll("[?=]$", "").trim()
                .replace('×', '*').replace('÷', '/').replace('−', '-')
                .replaceAll("\\s+", "");
        if (input.length() < 3 || input.length() > 90
                || !input.matches("[0-9.()*/+\\-^]+"))
            return null;
        try {
            double value = new Parser(input).parse();
            if (!Double.isFinite(value) || Math.abs(value) > 1e15)
                return "Le résultat dépasse la plage de calcul locale.";
            String v = Math.rint(value) == value ? String.format(Locale.FRANCE, "%.0f", value)
                    : String.format(Locale.FRANCE, "%.8f", value).replaceAll("0+$", "").replaceAll(",$", "");
            return "Calcul effectué localement sur le téléphone : " + v + ".";
        } catch (IllegalArgumentException invalid) {
            return "Expression mathématique invalide. Utilisez les chiffres et +, -, *, / ou parenthèses.";
        }
    }

    private static final class Parser {
        final String text;
        int at;
        Parser(String expression) { text = expression; }
        double parse() {
            double x = expression();
            if (at != text.length()) throw new IllegalArgumentException("suffix");
            return x;
        }
        double expression() {
            double v = term();
            while (at < text.length()) {
                char c = text.charAt(at);
                if (c == '+') { at++; v += term(); }
                else if (c == '-') { at++; v -= term(); }
                else break;
            }
            return v;
        }
        double term() {
            double v = power();
            while (at < text.length()) {
                char c = text.charAt(at);
                if (c == '*') { at++; v *= power(); }
                else if (c == '/') {
                    at++;
                    double denominator = power();
                    if (denominator == 0) throw new IllegalArgumentException("division by zero");
                    v /= denominator;
                } else break;
            }
            return v;
        }
        double power() {
            double left = unary();
            if (at < text.length() && text.charAt(at) == '^') {
                at++;
                double exp = power();
                left = Math.pow(left, exp);
            }
            return left;
        }
        double unary() {
            if (at < text.length() && text.charAt(at) == '-') {at++;return -unary();}
            if (at < text.length() && text.charAt(at) == '+') {at++;return unary();}
            return atom();
        }
        double atom() {
            if (at < text.length() && text.charAt(at) == '(') {
                at++;
                double v = expression();
                if (at == text.length() || text.charAt(at++) != ')')
                    throw new IllegalArgumentException("Missing )");
                return v;
            }
            int from = at;
            while (at < text.length() && (text.charAt(at) == '.' || Character.isDigit(text.charAt(at)))) at++;
            if (from == at) throw new IllegalArgumentException("number missing");
            try {return Double.parseDouble(text.substring(from,at));}
            catch(NumberFormatException bad){throw new IllegalArgumentException("bad number",bad);}
        }
    }
}
