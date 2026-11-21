package fr.friday.assistant;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.StatFs;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

/**
 * Import local du modèle officiel Zipformer FR : quatre fichiers sélectionnés
 * dans le gestionnaire de fichiers Android, jamais envoyés sur Internet.
 * L'archive tar.bz2 officielle doit être extraite avant cette sélection.
 */
public final class SherpaVoiceModels {
    private SherpaVoiceModels() { }

    private static final String DIR = "sherpa-onnx-fr";
    private static final String TMP = "sherpa-fr-import.tmp";
    private static final String BAK = "sherpa-fr-old.tmp";
    private static final long MAX_FILE = 320_000_000L;
    private static final long MAX_TOTAL = 400_000_000L;

    public static File directory(Context ctx) { return new File(ctx.getFilesDir(), DIR); }

    private static String role(String name) {
        String n = name == null ? "" : name.toLowerCase(Locale.ROOT);
        // N'accepter que les fichiers du modèle français officiel, sans mélange
        // fp32/int8 ni format renommé arbitrairement.
        if ("tokens.txt".equals(n)) return "tokens.txt";
        if ("encoder-epoch-29-avg-9-with-averaged-model.int8.onnx".equals(n))
            return "encoder.onnx";
        if ("decoder-epoch-29-avg-9-with-averaged-model.onnx".equals(n))
            return "decoder.onnx";
        if ("joiner-epoch-29-avg-9-with-averaged-model.int8.onnx".equals(n))
            return "joiner.onnx";
        return "";
    }

    private static String fileName(Context ctx, Uri uri) {
        try (Cursor cursor = ctx.getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int i = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (i >= 0) return cursor.getString(i);
            }
        } catch (Exception ignored) { }
        return "";
    }

    private static void clean(File file) {
        if (!file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) clean(child);
        }
        file.delete();
    }

    /** Réparation après interruption entre la sauvegarde et la permutation. */
    private static void recover(Context ctx) {
        File base = ctx.getFilesDir();
        File target = directory(ctx);
        File backup = new File(base, BAK);
        if (!target.exists() && backup.isDirectory()) backup.renameTo(target);
    }

    public static boolean ready(Context ctx) {
        recover(ctx);
        File folder = directory(ctx);
        return folder.isDirectory()
                && new File(folder, "encoder.onnx").length() > 1_000_000L
                && new File(folder, "decoder.onnx").length() > 100_000L
                && new File(folder, "joiner.onnx").length() > 10_000L
                && new File(folder, "tokens.txt").length() > 1_000L;
    }

    /** À appeler hors thread UI lorsque l'écoute est arrêtée. */
    public static synchronized String importSelected(Context ctx, List<Uri> uris)
            throws IOException {
        if (uris == null || uris.size() != 4)
            throw new IOException("Sélectionnez ensemble les quatre fichiers du modèle français.");
        Map<String, Uri> selected = new HashMap<>();
        for (Uri uri : uris) {
            if (uri == null || !"content".equals(uri.getScheme()))
                throw new IOException("Utilisez le sélecteur de documents Android.");
            String mapped = role(fileName(ctx, uri));
            if (mapped.isEmpty() || selected.put(mapped, uri) != null)
                throw new IOException("Fichier inconnu ou doublon : utilisez encoder INT8, decoder ONNX, joiner INT8 et tokens.txt.");
        }
        if (selected.size() != 4) throw new IOException("Le modèle est incomplet.");
        File root = ctx.getFilesDir();
        File target = directory(ctx);
        File staging = new File(root, TMP);
        File backup = new File(root, BAK);
        recover(ctx);
        clean(staging);
        if (!staging.mkdirs()) throw new IOException("Stockage temporaire indisponible.");
        long total = 0;
        try {
            if (new StatFs(root.getAbsolutePath()).getAvailableBytes() < 450_000_000L)
                throw new IOException("Prévoyez au moins 450 Mo d'espace libre pour cet import.");
            for (Map.Entry<String, Uri> item : selected.entrySet()) {
                try (InputStream in = ctx.getContentResolver().openInputStream(item.getValue());
                     FileOutputStream out = new FileOutputStream(new File(staging, item.getKey()))) {
                    if (in == null) throw new IOException("Fichier inaccessible.");
                    byte[] chunk = new byte[64 * 1024];
                    int n;
                    long bytes = 0;
                    while ((n = in.read(chunk)) >= 0) {
                        if (n == 0) continue;
                        bytes += n;
                        total += n;
                        if (bytes > MAX_FILE || total > MAX_TOTAL)
                            throw new IOException("Taille du modèle excessive ou archive incorrecte.");
                        out.write(chunk, 0, n);
                    }
                    out.getFD().sync();
                }
            }
            if (!isComplete(staging)) throw new IOException("Fichiers ONNX manquants ou tronqués.");
            clean(backup);
            if (target.exists() && !target.renameTo(backup))
                throw new IOException("Impossible de sécuriser l'ancien modèle.");
            if (!staging.renameTo(target)) {
                if (!target.exists()) backup.renameTo(target);
                throw new IOException("Impossible de finaliser l'importation.");
            }
            clean(backup);
            return "Modèle sherpa-onnx français importé dans le stockage privé. "
                    + "Activez le moteur expérimental puis relancez l'écoute.";
        } finally {
            clean(staging);
        }
    }

    /**
     * Import direct d'une archive .tar.bz2 officielle depuis Téléchargements.
     * N'extrait que les quatre fichiers sélectionnés par leur nom exact.
     * Aucun chemin de l'archive n'est utilisé comme destination (anti zip-slip).
     */
    public static synchronized String importOfficialArchive(Context ctx, Uri uri)
            throws IOException {
        if (uri == null || !"content".equals(uri.getScheme()))
            throw new IOException("Sélectionnez le fichier officiel .tar.bz2 via Android.");
        String sourceName = fileName(ctx, uri);
        if (!SherpaArchivePolicy.acceptedDisplayName(sourceName))
            throw new IOException("Nom d'archive non reconnu : " + sourceName
                    + ". Choisissez le modèle officiel Zipformer français "
                    + "(extension .tar.bz2, éventuellement renommé par Android).");
        File root = ctx.getFilesDir();
        File target = directory(ctx);
        File staging = new File(root, TMP);
        File backup = new File(root, BAK);
        recover(ctx);
        clean(staging);
        if (!staging.mkdirs()) throw new IOException("Stockage temporaire indisponible.");
        long total = 0;
        Map<String, Boolean> found = new HashMap<>();
        try {
            if (new StatFs(root.getAbsolutePath()).getAvailableBytes() < 450_000_000L)
                throw new IOException("Espace libre insuffisant : prévoyez 450 Mo.");
            try (InputStream input = ctx.getContentResolver().openInputStream(uri)) {
                if (input == null) throw new IOException("Archive inaccessible.");
                try (TarArchiveInputStream tar = new TarArchiveInputStream(
                        new BZip2CompressorInputStream(
                                SherpaArchivePolicy.checkedBzip2(input)))) {
                    TarArchiveEntry entry;
                    while ((entry = tar.getNextTarEntry()) != null) {
                        if (!entry.isFile()) continue;
                        String entryName = entry.getName();
                        String fileName = entryName.substring(entryName.lastIndexOf('/') + 1);
                        String role = role(fileName);
                        if (role.isEmpty()) continue;
                        if (found.put(role, true) != null)
                            throw new IOException("Doublon dans l'archive française.");
                        try (FileOutputStream out =
                                     new FileOutputStream(new File(staging, role))) {
                            byte[] chunk = new byte[64 * 1024];
                            int n;
                            long bytes = 0;
                            while ((n = tar.read(chunk)) != -1) {
                                if (n == 0) continue;
                                bytes += n;
                                total += n;
                                if (bytes > MAX_FILE || total > MAX_TOTAL)
                                    throw new IOException("Fichier du modèle trop volumineux.");
                                out.write(chunk, 0, n);
                            }
                            out.getFD().sync();
                        }
                    }
                }
            }
            if (found.size() != 4 || !isComplete(staging))
                throw new IOException("L'archive du modèle français est incomplète.");
            clean(backup);
            if (target.exists() && !target.renameTo(backup))
                throw new IOException("Ancien modèle impossible à sauvegarder.");
            if (!staging.renameTo(target)) {
                if (!target.exists()) backup.renameTo(target);
                throw new IOException("Importation non finalisée : ancien modèle restauré.");
            }
            clean(backup);
            return "Modèle sherpa-onnx français installé depuis l'archive officielle. "
                    + "Activez l'essai puis redémarrez l'écoute.";
        } finally {
            clean(staging);
        }
    }

    private static boolean isComplete(File dir) {
        return new File(dir, "encoder.onnx").length() > 1_000_000L
                && new File(dir, "decoder.onnx").length() > 100_000L
                && new File(dir, "joiner.onnx").length() > 10_000L
                && new File(dir, "tokens.txt").length() > 1_000L;
    }
}
