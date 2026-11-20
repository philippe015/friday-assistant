package fr.friday.assistant;

import java.net.URI;
import java.text.Normalizer;
import java.util.Locale;

/**
 * Politique de recherche de sources primaires publiques, sans API, sans GPS
 * et sans mémoire de conversation. Une appartenance de domaine n'est PAS
 * une validation du contenu, de la date ou du caractère à jour de la page.
 */
public final class OfficialSourcePolicy {
    private OfficialSourcePolicy() { }

    private static String fold(String text) {
        return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.FRENCH)
                .replaceAll("[^a-z0-9 ]+", " ").replaceAll("\\s+", " ").trim();
    }

    /** Les catégories concernées seulement : pas de biais étatique sur l'actualité générale. */
    public static String[] preferredSites(String question) {
        String q = fold(question);
        if (q.matches(".*\\b(?:android|google play|smartphone|microphone android|"
                + "permissions android|application android|apk|gradle)\\b.*"))
            return new String[] {"developer.android.com"};
        if (q.matches(".*\\b(?:rgpd|cnil|donnees personnelles|vie privee|"
                + "cybersecurite|rancongiciel|phishing)\\b.*"))
            return new String[] {"cnil.fr", "cyber.gouv.fr"};
        if (q.matches(".*\\b(?:sante|maladie|medecin|traitement|vaccin|"
                + "assurance maladie|remboursement|medicament|ordonnance|"
                + "hopital|tension arterielle|bilan sanguin)\\b.*"))
            return new String[] {"ameli.fr", "has-sante.fr"};
        if (q.matches(".*\\b(?:loi|lois|article de loi|decret|code penal|"
                + "code du travail|code de procedure|legislation|"
                + "reglementation|contrat de travail|licenciement|"
                + "succession|heritage|impots|retraite|securite sociale|"
                + "droit|droits|formalites|demarches|service public|"
                + "ssiap|agent de securite)\\b.*"))
            return new String[] {"service-public.gouv.fr", "legifrance.gouv.fr"};
        if (q.matches(".*\\b(?:statistiques|population|chomage|inflation|"
                + "recensement|pib|demographie)\\b.*"))
            return new String[] {"insee.fr"};
        return new String[0];
    }

    private static boolean hostOrSubdomain(String host, String officialDomain) {
        return host.equals(officialDomain) || host.endsWith("." + officialDomain);
    }

    /**
     * Liste de domaines institutionnels ciblés : ne jamais classer
     * faux-site.gouv.fr.evil.example comme source officielle.
     */
    public static boolean isOfficialUrl(String url) {
        try {
            URI uri = URI.create(url);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null)
                return false;
            String host = uri.getHost();
            if (host == null) return false;
            host = host.toLowerCase(Locale.ROOT);
            return hostOrSubdomain(host, "gouv.fr")
                    || hostOrSubdomain(host, "ameli.fr")
                    || hostOrSubdomain(host, "has-sante.fr")
                    || hostOrSubdomain(host, "santepubliquefrance.fr")
                    || hostOrSubdomain(host, "cnil.fr")
                    || hostOrSubdomain(host, "insee.fr")
                    || hostOrSubdomain(host, "vie-publique.fr")
                    || hostOrSubdomain(host, "banque-france.fr")
                    || hostOrSubdomain(host, "developer.android.com")
                    || hostOrSubdomain(host, "europa.eu")
                    || hostOrSubdomain(host, "who.int");
        } catch (Exception invalid) {
            return false;
        }
    }

    public static String sourceLabel(String url) {
        return isOfficialUrl(url)
                ? "Domaine institutionnel (contenu et actualisation à vérifier)"
                : "Source secondaire ou non institutionnelle";
    }
}
