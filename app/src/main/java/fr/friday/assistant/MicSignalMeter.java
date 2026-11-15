package fr.friday.assistant;

/**
 * Niveau acoustique relatif du PCM16 mono reçu d'AudioRecord.
 * Mesure seulement l'énergie audio : ne distingue pas la voix de l'utilisateur
 * du bruit ambiant ou de l'écho de la synthèse vocale.
 */
public final class MicSignalMeter {
    private MicSignalMeter() {}

    /** Échelle indicative 0..100, sans accès réseau ni conservation du son. */
    public static int level(byte[] pcm, int bytesRead) {
        if (pcm == null || bytesRead < 2) return 0;
        int len = Math.min(bytesRead, pcm.length);
        double squared = 0;
        int samples = 0;
        // Échantillonner 1 paire de bytes sur 8, économie CPU.
        for (int i = 0; i + 1 < len; i += 16) {
            int value = (short) ((pcm[i] & 0xff) | ((pcm[i+1] & 0xff) << 8));
            squared += (double) value * value;
            samples++;
        }
        if (samples == 0) return 0;
        double rms = Math.sqrt(squared / samples);
        if (rms < 1) return 0;
        double dbfs = 20.0 * Math.log10(rms / 32768.0);
        return Math.max(0, Math.min(100, (int) Math.round((dbfs + 65.0) * (100.0 / 65.0))));
    }
}
