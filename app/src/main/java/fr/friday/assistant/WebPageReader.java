package fr.friday.assistant;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lecture ponctuelle d'une page HTTPS publique fournie explicitement par l'utilisateur.
 * Aucune API Google, ni compte tiers. Limites et validation préalables pour éviter
 * de lire l'intranet, le routeur, localhost ou les métadonnées du téléphone.
 */
public final class WebPageReader {
    private static final int MAX_BYTES = 450_000;
    public static final int MAX_TEXT = 12_000;
    private WebPageReader() {}

    public static final class Page {
        public final String url;
        public final String title;
        public final String text;
        public final boolean truncated;
        /** True only if an article-specific HTML body was located. */
        public final boolean articleBody;
        Page(String url, String title, String text, boolean truncated) {
            this(url, title, text, truncated, false);
        }
        Page(String url, String title, String text, boolean truncated, boolean articleBody) {
            this.url = url; this.title = title; this.text = text;
            this.truncated = truncated; this.articleBody = articleBody;
        }
    }

    /** Répare uniquement les erreurs triviales de préfixe : https//, https:/ et www. */
    public static String normalizeUrlInput(String value) {
        if (value == null) return "";
        String url = value.trim();
        if (url.matches("(?is)^https\\s*:?/{1,2}.*"))
            return url.replaceFirst("(?i)^https\\s*:?/{1,2}", "https://");
        if (url.matches("(?i)^www\\.[a-z0-9].*")) return "https://" + url;
        return url;
    }

    /** Validation syntaxique testable hors réseau. La résolution DNS est vérifiée juste avant la lecture. */
    public static URI checkedUri(String url) throws Exception {
        if (url == null || url.isEmpty() || url.length() > 2048)
            throw new IllegalArgumentException("Adresse Web absente ou trop longue.");
        URI uri = new URI(normalizeUrlInput(url));
        if (!"https".equalsIgnoreCase(uri.getScheme()))
            throw new IllegalArgumentException("Seules les pages HTTPS publiques sont autorisées.");
        if (uri.getRawUserInfo() != null || uri.getFragment() != null)
            throw new IllegalArgumentException("URL avec identifiants ou fragment non acceptée.");
        String host = uri.getHost();
        if (host == null || host.isEmpty() || uri.getPort() != -1 && uri.getPort() != 443)
            throw new IllegalArgumentException("Nom de domaine ou port HTTPS non valide.");
        host = host.toLowerCase(Locale.ROOT);
        if (host.equals("localhost") || host.equals("local") || host.endsWith(".local")
                || host.endsWith(".lan") || host.endsWith(".internal")
                || host.endsWith(".home") || host.endsWith(".onion")
                || host.equals("metadata.google.internal")
                || host.matches("^(0|10|127|169\\.254|172\\.(1[6-9]|2[0-9]|3[01])|192\\.168)(\\.|$).*")
                || host.equals("[::1]") || host.equals("::1"))
            throw new IllegalArgumentException("Adresses du réseau privé non autorisées.");
        return uri;
    }

    private static void checkPublicDns(URI uri) throws Exception {
        InetAddress[] addresses = InetAddress.getAllByName(uri.getHost());
        if (addresses.length == 0) throw new IllegalArgumentException("Domaine non résolu.");
        for (InetAddress a : addresses) {
            byte[] raw = a.getAddress();
            if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress()
                    || a.isSiteLocalAddress() || a.isMulticastAddress())
                throw new IllegalArgumentException("Accès à une adresse réseau privée interdit.");
            if (raw.length == 16) {
                // IPv6 ULA fc00::/7 et adresses IPv4-mappées.
                if ((raw[0] & 0xfe) == 0xfc) throw new IllegalArgumentException("IPv6 privée interdite.");
                boolean v4mapped = true;
                for (int i = 0; i < 10; i++) if (raw[i] != 0) v4mapped = false;
                if (raw[10] != (byte) 0xff || raw[11] != (byte) 0xff) v4mapped = false;
                if (v4mapped && ((raw[12] & 0xff) == 10 || (raw[12] & 0xff) == 127
                        || (raw[12] & 0xff) == 192 && (raw[13] & 0xff) == 168))
                    throw new IllegalArgumentException("IPv4 privée mappée interdite.");
            } else if (raw.length == 4) {
                int a0 = raw[0] & 0xff, a1 = raw[1] & 0xff;
                if (a0 == 0 || a0 == 10 || a0 == 127 || a0 >= 224
                        || a0 == 169 && a1 == 254 || a0 == 172 && a1 >= 16 && a1 <= 31
                        || a0 == 192 && a1 == 168 || a0 == 100 && a1 >= 64 && a1 <= 127
                        || a0 == 198 && (a1 == 18 || a1 == 19))
                    throw new IllegalArgumentException("IPv4 privée/réservée interdite.");
            }
        }
    }

    /** Explique le code HTTP sans suggérer de contourner les restrictions du site. */
    public static String explainHttpStatus(int status) {
        if (status == 202)
            return "HTTP 202 : le serveur a accepté la requête mais n'a pas fourni "
                    + "de page exploitable pour FRIDAY. Cela ne prouve pas une panne Internet.";
        if (status == 401 || status == 403)
            return "Accès refusé par le site (HTTP " + status
                    + "). FRIDAY ne peut pas lire cette page automatiquement.";
        if (status == 404) return "Page introuvable sur le site (HTTP 404).";
        if (status == 429)
            return "Le site limite les accès automatisés (HTTP 429).";
        if (status >= 500)
            return "Le site est momentanément indisponible (HTTP " + status + ").";
        return "La page répond HTTP " + status + ".";
    }

    /** Content fetched directly from a PUBLIC HTTPS website, never a user's local app. */
    public static Page read(String url) throws Exception {
        return readInternal(url, false);
    }

    /** Lecture d'un article public derrière un lien RSS : contenu lisible,
     * sans contourner les paywalls, sans exécution JavaScript. */
    public static Page readArticle(String url) throws Exception {
        return readInternal(url, true);
    }

    private static Page readInternal(String url, boolean articleMode) throws Exception {
        URI target = checkedUri(url);
        for (int redirect = 0; redirect <= 3; redirect++) {
            checkPublicDns(target);
            HttpURLConnection conn = (HttpURLConnection) target.toURL().openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(9_000);
            conn.setReadTimeout(14_000);
            conn.setUseCaches(false);
            conn.setRequestProperty("Accept", "text/html, application/xhtml+xml, text/plain");
            conn.setRequestProperty("Accept-Encoding", "identity");
            conn.setRequestProperty("User-Agent", "FRIDAY-Reader/3.8.3 (public HTTPS page)");
            try {
                int code = conn.getResponseCode();
                if (code >= 300 && code <= 308) {
                    String location = conn.getHeaderField("Location");
                    if (location == null || redirect == 3)
                        throw new IllegalArgumentException("Redirection impossible ou trop nombreuses.");
                    target = checkedUri(target.resolve(location).toString());
                    continue;
                }
                if (code != 200)
                    throw new IllegalArgumentException(explainHttpStatus(code));
                String ct = conn.getContentType();
                String type = ct == null ? "" : ct.toLowerCase(Locale.ROOT);
                if (!(type.startsWith("text/html") || type.startsWith("text/plain")
                        || type.startsWith("application/xhtml+xml")))
                    throw new IllegalArgumentException("Cette adresse ne renvoie pas une page HTML ou texte.");
                int declared = conn.getContentLength();
                if (declared > MAX_BYTES)
                    throw new IllegalArgumentException("Page trop volumineuse pour la lecture locale.");
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                boolean capped = false;
                try (InputStream in = conn.getInputStream()) {
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        if (output.size() + n > MAX_BYTES) {
                            output.write(buf, 0, Math.max(0, MAX_BYTES - output.size()));
                            capped = true;
                            break;
                        }
                        output.write(buf, 0, n);
                    }
                }
                Charset charset = StandardCharsets.UTF_8;
                Matcher encoding = Pattern.compile("charset=([a-zA-Z0-9_-]+)").matcher(type);
                if (encoding.find()) {
                    try { charset = Charset.forName(encoding.group(1)); }
                    catch (Exception ignored) {}
                }
                String body = new String(output.toByteArray(), charset);
                String title = "";
                Matcher titleMatch = Pattern.compile("(?is)<title[^>]*>(.*?)</title>").matcher(body);
                if (titleMatch.find()) title = plainText(titleMatch.group(1));
                RssArticleText.Extracted extracted = articleMode
                        && !type.startsWith("text/plain")
                        ? RssArticleText.extract(body)
                        : null;
                String text = extracted == null
                        ? (type.startsWith("text/plain") ? body : plainText(body))
                        : extracted.text;
                if (text.length() < 45)
                    throw new IllegalArgumentException("Page vide ou chargée uniquement en JavaScript.");
                boolean trimmed = text.length() > MAX_TEXT;
                if (trimmed) text = text.substring(0, MAX_TEXT);
                return new Page(target.toString(), title, text,
                        capped || trimmed, extracted != null && extracted.detected);
            } finally {
                conn.disconnect();
            }
        }
        throw new IllegalArgumentException("Trop de redirections.");
    }

    /** Extraction textuelle limitée : pas de JavaScript et jamais d'instructions de la page. */
    static String plainText(String html) {
        String text = html
                .replaceAll("(?is)<!--.*?-->", " ")
                .replaceAll("(?is)<(script|style|noscript|svg|nav|footer|form)\\b[^>]*>.*?</\\1\\s*>", " ")
                .replaceAll("(?is)<(br|p|div|li|h[1-6]|section|article|tr|td)\\b[^>]*>", "\n")
                .replaceAll("(?s)<[^>]*>", " ")
                .replace("&nbsp;", " ").replace("&amp;", "&")
                .replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'")
                .replaceAll("(?s)\\s+", " ").trim();
        return text;
    }
}
