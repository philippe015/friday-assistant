package fr.friday.assistant;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Historique vocal local : données AES-GCM via Android Keystore.
 * Migration de l'ancien SharedPreferences clair après confirmation de l'écriture.
 * Aucun accès réseau ni GitHub.
 */
public final class PrivateConversationHistory {
    private static final String KEY = "voice_history_v380";
    private static final String OLD = "friday_voice_history";
    // Les 40 derniers échanges (80 messages) restent chiffrés sur le téléphone.
    // L'IA ne reçoit qu'un extrait court sélectionné à chaque réponse.
    private static final int MAX_EVENTS = 80;
    private PrivateConversationHistory() {}

    public static synchronized String read(Context ctx, SecurePrefs prefs) throws Exception {
        if (prefs.hasPrivateSecret(KEY)) {
            String result = prefs.privateSecret(KEY);
            new JSONArray(result); // validation, pas de repli en clair
            // Réessayer de purger toute ancienne copie en clair si la
            // suppression avait été interrompue après migration.
            SharedPreferences legacy = ctx.getSharedPreferences(OLD, Context.MODE_PRIVATE);
            if (legacy.contains("events") && !legacy.edit().clear().commit())
                throw new IllegalStateException("Ancien historique non supprimé");
            return result;
        }
        SharedPreferences old = ctx.getSharedPreferences(OLD, Context.MODE_PRIVATE);
        String previous = old.getString("events", "[]");
        JSONArray data = new JSONArray(previous);
        JSONArray clean = new JSONArray();
        for (int i=Math.max(0,data.length()-MAX_EVENTS); i<data.length(); i++) {
            JSONObject obj = data.optJSONObject(i);
            if (obj == null) continue;
            String kind = obj.optString("kind", "");
            String value = obj.optString("text", "");
            if (!("user".equals(kind) || "assistant".equals(kind))) continue;
            clean.put(new JSONObject().put("seq", obj.optLong("seq", i+1))
                    .put("kind",kind).put("text",value));
        }
        String json = clean.toString();
        prefs.putPrivateSecret(KEY, json);
        if (!json.equals(prefs.privateSecret(KEY)))
            throw new IllegalStateException("Migration de l'historique non vérifiée");
        if (!old.edit().clear().commit())
            throw new IllegalStateException("Ancien historique non supprimé");
        return json;
    }

    public static synchronized long append(Context ctx, SecurePrefs prefs,
            String kind, String text) throws Exception {
        return append(ctx, prefs, kind, text, "unknown");
    }

    /** Each NEW encrypted exchange has a wall-clock timestamp and source.
     * Older messages remain intact but without an invented timestamp. */
    public static synchronized long append(Context ctx, SecurePrefs prefs,
            String kind, String text, String channel) throws Exception {
        return append(ctx, prefs, kind, text, channel,
                System.currentTimeMillis());
    }

    /** A typed question can be timestamped on SUBMISSION even if its encrypted
     * history write must wait until after the model has answered. */
    public static synchronized long append(Context ctx, SecurePrefs prefs,
            String kind, String text, String channel, long occurredAtMs)
            throws Exception {
        if (occurredAtMs <= 0)
            throw new IllegalArgumentException("Horodatage invalide");
        if (!("user".equals(kind) || "assistant".equals(kind)))
            throw new IllegalArgumentException("Type d'événement invalide");
        if (!("voice".equals(channel) || "text".equals(channel)
                || "unknown".equals(channel)))
            throw new IllegalArgumentException("Canal de conversation invalide");
        String json = read(ctx, prefs);
        JSONArray old = new JSONArray(json);
        JSONArray clean = new JSONArray();
        long seq = 0;
        for (int i=Math.max(0, old.length()-(MAX_EVENTS-1)); i<old.length();i++) {
            JSONObject item = old.getJSONObject(i);
            seq = Math.max(seq, item.optLong("seq", 0));
            clean.put(item);
        }
        seq++;
        clean.put(new JSONObject().put("seq", seq)
                .put("kind", kind).put("text", text == null ? "" : text)
                .put("time_ms", occurredAtMs)
                .put("channel", channel));
        prefs.putPrivateSecret(KEY, clean.toString());
        return seq;
    }

    public static synchronized void clear(Context ctx, SecurePrefs prefs) throws Exception {
        prefs.putPrivateSecret(KEY, "[]");
        if (!ctx.getSharedPreferences(OLD, Context.MODE_PRIVATE).edit().clear().commit())
            throw new IllegalStateException("Impossible d'effacer l'ancien historique");
    }
}
