package fr.friday.assistant;

import android.app.ActivityManager;
import android.content.Context;

/**
 * Conseil factuel pour choisir un modèle local compatible avec les formats
 * déjà importables. Ne télécharge rien et ne remplace jamais un modèle actif.
 */
public final class LocalModelAdvisor {
    private LocalModelAdvisor() {}

    public static String advice(Context context) {
        long available = 0;
        try {
            ActivityManager manager = (ActivityManager)
                    context.getSystemService(Context.ACTIVITY_SERVICE);
            if (manager != null) {
                ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
                manager.getMemoryInfo(info);
                available = info.availMem;
            }
        } catch (RuntimeException ignored) { }
        return adviceForAvailableRam(available);
    }

    public static String adviceForAvailableRam(long availableBytes) {
        StringBuilder message = new StringBuilder("Choix IA Intelligence+ : ");
        if (availableBytes >= 3_000_000_000L) {
            message.append("la RAM actuellement disponible peut permettre "
                    + "d'essayer un petit modèle conversationnel comme Qwen3 0.6B "
                    + "au format LiteRT-LM .litertlm compatible, sans garantie "
                    + "de performance ni de compatibilité avec le GPU. ");
        } else if (availableBytes > 0) {
            message.append("la RAM actuellement disponible est limitée : "
                    + "privilégiez un modèle quantifié léger et compatible, "
                    + "par exemple un modèle Gemma compact prévu pour Android. ");
        } else {
            message.append("la mémoire disponible n'a pas pu être estimée ; "
                    + "privilégiez d'abord un petit modèle Android compatible. ");
        }
        return message.append("Le fichier doit être un vrai .task MediaPipe "
                    + "ou .litertlm LiteRT-LM, pas un GGUF renommé. "
                    + "Import manuel et test local obligatoires ; aucun "
                    + "téléchargement ou remplacement automatique.").toString();
    }
}
