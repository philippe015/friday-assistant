package fr.friday.assistant;

import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Questions vocales portant sur des informations très récentes.
 * Ni navigateur ni dépendance Android : règles déterministes et testables.
 */
public final class RecentNewsPolicy {
    private RecentNewsPolicy() { }

    private static final Pattern NEWS = Pattern.compile(
            "\\b(?:actualites?|actus?|nouvelles?|infos?|informations?|news|flash|journal|evenements?)\\b");
    private static final Pattern HOURS = Pattern.compile(
            "\\b(\\d{1,2}|sept|six|douze|trois|quatre|vingt quatre)"
            + "(?:\\s+(?:derniers?|dernieres?|precedents?|precedentes?)){0,3}\\s+heures?\\b");
    private static final Pattern REVERSE_HOURS = Pattern.compile(
            "\\b(?:derniers?|dernieres?|precedents?|precedentes?)\\s+"
            + "(\\d{1,2}|sept|six|douze|trois|quatre|vingt quatre)\\s+heures?\\b");
    private static final Pattern DENIAL = Pattern.compile(
            "(?:je ne peux pas acceder|je n ai pas acces|je n ai aucun acces|"
            + "ne peux pas consulter|ne peut pas consulter|"
            + "je ne dispose pas d acces|pas acces a internet|"
            + "ne peux pas parcourir le web|"
            + "je ne suis pas en mesure d acceder)");

    private static String fold(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.FRENCH)
                .replaceAll("[^a-z0-9 ]+", " ").replaceAll("\\s+", " ").trim();
    }

    public static boolean isNews(String question) {
        return NEWS.matcher(fold(question)).find();
    }

    public static int requestedHours(String question) {
        String s = fold(question);
        Matcher m = HOURS.matcher(s);
        if (!m.find()) m = REVERSE_HOURS.matcher(s);
        if (!m.find(0)) return 0;
        String value = m.group(1);
        int hours;
        switch (value) {
            case "sept": hours = 7; break;
            case "six": hours = 6; break;
            case "douze": hours = 12; break;
            case "trois": hours = 3; break;
            case "quatre": hours = 4; break;
            case "vingt quatre": hours = 24; break;
            default:
                try { hours = Integer.parseInt(value); }
                catch (NumberFormatException ignored) { return 0; }
        }
        return hours > 0 && hours <= 48 ? hours : 0;
    }

    public static boolean requiresRecentNews(String question) {
        return isNews(question) && requestedHours(question) > 0;
    }

    public static String searchPrompt(String original, long nowMs, TimeZone zone) {
        int hours = requestedHours(original);
        if (!isNews(original) || hours < 1) return original;
        Calendar start = Calendar.getInstance(zone, Locale.FRANCE);
        start.setTimeInMillis(nowMs);
        start.add(Calendar.HOUR_OF_DAY, -hours);
        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm z", Locale.FRANCE);
        fmt.setTimeZone(zone);
        return original + "\n\n[DEMANDE D'ACTUALITÉ TRÈS RÉCENTE — RECHERCHE WEB OBLIGATOIRE]"
                + "\nPériode demandée : de " + fmt.format(start.getTime())
                + " à " + fmt.format(new Date(nowMs)) + " (dernières " + hours + " heures)."
                + "\nRecherche des articles réellement publiés pendant CETTE période."
                + "\nPour chaque information, donne le titre, la source, l'URL et"
                + " la date/heure de publication quand la source la fournit."
                + "\nN'affirme pas qu'un article a moins de " + hours
                + " heures si son horodatage n'est pas vérifiable."
                + "\nSi aucune source récente n'est trouvée, dis-le clairement ;"
                + " ne remplace pas ces actualités par des faits anciens ou inventés."
                + "\nN'affirme pas ne pas disposer d'Internet sans vérifier"
                + " si l'outil web_search a été utilisé.\n[FIN DE LA DEMANDE]";
    }

    /** Une dénégation générique du modèle ne prouve pas que FRIDAY est hors ligne. */
    public static boolean genericWebDenial(String response) {
        return DENIAL.matcher(fold(response)).find();
    }
}
