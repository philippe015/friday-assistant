package fr.friday.assistant;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Optional authenticated FRIDAY ↔ OpenJarvis server bridge.
 *
 * OpenJarvis official non-streaming API: POST /v1/chat/completions
 * https://github.com/open-jarvis/OpenJarvis/blob/main/docs/deployment/api-server.md
 * This is an interoperability client, NOT the OpenJarvis Python/Rust runtime.
 *
 * Security: OFF by default, explicit TLS URL + secret API token, no redirects,
 * no hidden memory/GPS/history transfer, bounded I/O, cancelable from FRIDAY.
 */
public final class OpenJarvisClient {
    private OpenJarvisClient() { }

    public static boolean configured(SecurePrefs settings) {
        if (!"1".equals(settings.get("openjarvis_enabled", "0"))) return false;
        try {
            OpenJarvisPolicy.baseUrl(settings.get("openjarvis_url", ""));
            OpenJarvisPolicy.model(settings.get("openjarvis_model", "qwen3.5:2b"));
            return settings.hasSecret("openjarvis");
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }

    public static String answer(SecurePrefs settings, String question,
                                NativeAI.CancelToken cancellation) throws Exception {
        if (!"1".equals(settings.get("openjarvis_enabled", "0")))
            throw new IOException("Connexion OpenJarvis désactivée dans les réglages.");
        String base = OpenJarvisPolicy.baseUrl(settings.get("openjarvis_url", ""));
        String model = OpenJarvisPolicy.model(settings.get("openjarvis_model", "qwen3.5:2b"));
        String token = settings.secret("openjarvis");
        if (token == null || token.isEmpty())
            throw new IOException("Clé serveur OpenJarvis absente. Configurez OPENJARVIS_API_KEY sur le PC et dans FRIDAY.");
        String cleaned = OpenJarvisPolicy.cleanQuestion(question);
        if (cleaned.isEmpty()) throw new IOException("Question OpenJarvis vide.");
        if (cleaned.length() > 12000) throw new IOException("Question trop longue pour OpenJarvis.");
        JSONObject body = new JSONObject()
                .put("model", model)
                .put("messages", new JSONArray()
                        .put(new JSONObject().put("role", "system")
                                .put("content", "Tu es l'agent OpenJarvis appelé explicitement par "
                                        + "FRIDAY Android. Réponds en français. "
                                        + "Ne prétends pas avoir utilisé un outil sans preuve. "
                                        + "Ne révèle jamais de secrets ou de fichiers locaux."))
                        .put(new JSONObject().put("role", "user").put("content", cleaned)))
                .put("max_tokens", 1024)
                .put("stream", false);
        JSONObject result = execute(
                OpenJarvisPolicy.completionsUrl(base), token, body, cancellation);
        JSONArray choices = result.optJSONArray("choices");
        if (choices == null || choices.length() == 0)
            throw new IOException("OpenJarvis n'a pas retourné de choix de réponse.");
        JSONObject message = choices.optJSONObject(0) == null ? null
                : choices.optJSONObject(0).optJSONObject("message");
        String text = message == null ? "" : message.optString("content", "").trim();
        if (text.isEmpty())
            throw new IOException("OpenJarvis a répondu sans texte. Vérifiez le modèle et les outils de l'agent.");
        if (text.length() > 28000)
            throw new IOException("Réponse OpenJarvis anormalement longue.");
        return text;
    }

    /** Real test request with harmless prompt; no stored conversation is transmitted. */
    public static String test(SecurePrefs settings) {
        try {
            String reply = answer(settings,
                    "Réponds seulement en français par : OpenJarvis opérationnel.",
                    null);
            return "Connexion OpenJarvis opérationnelle. Réponse : "
                    + (reply.length() > 220 ? reply.substring(0, 220) + "…" : reply)
                    + "\\nAucune donnée personnelle envoyée pendant le test.";
        } catch (Exception failure) {
            return "Test OpenJarvis ÉCHOUÉ : " + safeError(failure)
                    + "\\nVérifiez le serveur HTTPS, le certificat TLS, la clé, le nom du modèle et la connexion réseau.";
        }
    }

    private static String safeError(Exception failure) {
        if (failure instanceof java.net.SocketTimeoutException) return "délai réseau dépassé";
        if (failure instanceof javax.net.ssl.SSLException) return "certificat HTTPS/TLS refusé";
        String message = failure.getMessage();
        if (message == null) return failure.getClass().getSimpleName();
        // Do not echo raw server errors: they may contain prompts/secrets.
        if (message.startsWith("HTTP ")) return message;
        if (message.length() > 165) return message.substring(0, 165) + "…";
        return message;
    }

    private static JSONObject execute(String endpoint, String token, JSONObject body,
                                      NativeAI.CancelToken cancellation) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        if (cancellation != null) cancellation.bind(connection);
        try {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(75000);
            connection.setInstanceFollowRedirects(false);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Authorization", "Bearer " + token);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Accept", "application/json");
            if (cancellation != null) cancellation.check();
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(bytes);
            }
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                String cause = status == 401 || status == 403 ? "accès ou clé refusés"
                        : status == 404 ? "route OpenJarvis absente"
                        : status == 429 ? "limite de requêtes du serveur atteinte"
                        : status >= 500 ? "serveur OpenJarvis indisponible" : "requête refusée";
                throw new IOException("HTTP " + status + " : " + cause);
            }
            ByteArrayOutputStream data = new ByteArrayOutputStream();
            try (InputStream input = connection.getInputStream()) {
                byte[] buffer = new byte[8192];
                int length;
                while ((length = input.read(buffer)) != -1) {
                    if (cancellation != null) cancellation.check();
                    if (data.size() + length > 1_000_000)
                        throw new IOException("Réponse OpenJarvis supérieure à 1 Mo.");
                    data.write(buffer, 0, length);
                }
            }
            return new JSONObject(data.toString(StandardCharsets.UTF_8.name()));
        } finally {
            if (cancellation != null) cancellation.unbind(connection);
            connection.disconnect();
        }
    }
}
