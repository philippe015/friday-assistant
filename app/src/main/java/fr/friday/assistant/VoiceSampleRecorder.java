package fr.friday.assistant;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.SystemClock;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Enregistre uniquement à la demande une référence WAV 16 kHz PCM mono, privée à l'application. */
public final class VoiceSampleRecorder {
    private static final int RATE = 16000;
    private VoiceSampleRecorder() {}
    public static File sample(Context context) { return new File(context.getFilesDir(), "friday_voice_reference.wav"); }
    public static boolean exists(Context context) {
        File file = sample(context);
        return file.isFile() && VoiceClonePolicy.isValidWavSize(file.length());
    }
    public static boolean erase(Context context) {
        File f = sample(context);
        return !f.exists() || f.delete();
    }
    public static void capture(Context context, int seconds) throws IOException {
        if (seconds < 6 || seconds > 20) throw new IOException("Durée d'enregistrement non autorisée.");
        int min = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (min <= 0) throw new IOException("Enregistrement PCM 16 kHz non disponible.");
        AudioRecord audio = null;
        File temp = new File(context.getFilesDir(), "friday_voice_reference.part");
        try {
            audio = new AudioRecord(MediaRecorder.AudioSource.MIC, RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(min * 2, 8192));
            if (audio.getState() != AudioRecord.STATE_INITIALIZED) throw new IOException("Microphone indisponible.");
            audio.startRecording();
            if (audio.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING)
                throw new IOException("Enregistrement refusé ; une autre édition utilise peut-être le microphone.");
            ByteArrayOutputStream pcm = new ByteArrayOutputStream(RATE * 2 * seconds);
            byte[] buf = new byte[3200];
            long until = SystemClock.elapsedRealtime() + seconds * 1000L;
            long squared = 0L, count = 0L;
            while (SystemClock.elapsedRealtime() < until && pcm.size() < RATE * 2 * (seconds + 1)) {
                int n = audio.read(buf, 0, buf.length);
                if (n <= 0) throw new IOException("Lecture du microphone interrompue : " + n);
                n &= ~1;
                pcm.write(buf, 0, n);
                for (int i = 0; i < n; i += 2) {
                    short value = (short)((buf[i] & 0xff) | (buf[i+1] << 8));
                    squared += (long)value * value;
                    count++;
                }
            }
            byte[] data = pcm.toByteArray();
            if (!VoiceClonePolicy.isValidWavSize(44L + data.length))
                throw new IOException("Enregistrement trop court. Réessayez en parlant pendant 12 secondes.");
            // Vérifie le signal pour ne pas sauvegarder une référence pratiquement silencieuse.
            if (count == 0 || Math.sqrt((double)squared / count) < 90.0)
                throw new IOException("La voix n'a pas été détectée : rapprochez le téléphone et réessayez.");
            try (FileOutputStream out = new FileOutputStream(temp)) {
                writeWavHeader(out, data.length);
                out.write(data);
                out.getFD().sync();
            }
            File target = sample(context);
            if (!temp.renameTo(target)) throw new IOException("Impossible de conserver l'échantillon vocal.");
        } finally {
            if (audio != null) {
                try { if (audio.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) audio.stop(); }
                catch (Exception ignored) {}
                audio.release();
            }
            if (temp.exists()) temp.delete();
        }
    }
    private static void writeWavHeader(FileOutputStream stream, int pcmSize) throws IOException {
        ByteArrayOutputStream h = new ByteArrayOutputStream(44);
        putText(h,"RIFF"); le32(h,pcmSize + 36); putText(h,"WAVEfmt ");
        le32(h,16); le16(h,1); le16(h,1); le32(h,RATE); le32(h,RATE*2); le16(h,2); le16(h,16);
        putText(h,"data"); le32(h,pcmSize);
        stream.write(h.toByteArray());
    }
    private static void putText(ByteArrayOutputStream b,String s) throws IOException { b.write(s.getBytes("US-ASCII")); }
    private static void le16(ByteArrayOutputStream b,int n) { b.write(n & 255); b.write((n >> 8) & 255); }
    private static void le32(ByteArrayOutputStream b,int n) { le16(b,n); le16(b,n >> 16); }
}
