package fr.friday.assistant;

import android.content.Context;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Téléchargement explicite depuis le fournisseur du modèle libre de reconnaissance français. */
public final class ModelInstaller {
    public static final String NAME = "vosk-model-small-fr-0.22";
    public static final String URL = "https://alphacephei.com/vosk/models/" + NAME + ".zip";
    public interface Progress { void update(String message); }
    public static File modelDir(Context c) { return new File(c.getFilesDir(), "models/" + NAME); }
    public static boolean installed(Context c) {
        return new File(modelDir(c), "am/final.mdl").isFile();
    }
    public static void download(Context c, Progress progress) throws Exception {
        File models = new File(c.getFilesDir(), "models");
        if (!models.exists() && !models.mkdirs()) throw new IOException("Impossible de créer le répertoire models");
        File stage = new File(c.getCacheDir(), "friday_model_stage");
        File zip = new File(c.getCacheDir(), "friday_model_download.zip");
        deleteTree(stage);
        if (!stage.mkdirs()) throw new IOException("Impossible de créer le répertoire temporaire");
        HttpURLConnection connection = null;
        long downloaded = 0;
        try {
            connection = (HttpURLConnection) new URL(URL).openConnection();
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(35000);
            connection.setInstanceFollowRedirects(true);
            if (connection.getResponseCode() != 200) throw new IOException("Téléchargement HTTP " + connection.getResponseCode());
            long expected = connection.getContentLengthLong();
            if (expected > 100_000_000L) throw new IOException("Fichier trop volumineux");
            try (InputStream in = connection.getInputStream(); OutputStream out = new BufferedOutputStream(new FileOutputStream(zip))) {
                byte[] buf = new byte[32768]; int n;
                while ((n = in.read(buf)) != -1) {
                    downloaded += n;
                    if (downloaded > 100_000_000L) throw new IOException("Taille limite dépassée");
                    out.write(buf, 0, n);
                    if (downloaded % (2 * 1024 * 1024) < n) progress.update("Modèle français : " + (downloaded / (1024 * 1024)) + " Mo" + (expected > 0 ? " / " + (expected / (1024 * 1024)) + " Mo" : ""));
                }
            }
            progress.update("Décompression du modèle…");
            long totalSize = 0;
            try (ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new FileInputStream(zip)))) {
                ZipEntry e;
                while ((e = zin.getNextEntry()) != null) {
                    String name = e.getName();
                    if (!name.startsWith(NAME + "/")) throw new IOException("Archive inattendue : " + name);
                    File out = new File(stage, name);
                    String base = stage.getCanonicalPath() + File.separator;
                    if (!out.getCanonicalPath().startsWith(base)) throw new IOException("Chemin d'archive non autorisé");
                    if (e.isDirectory()) { if (!out.exists() && !out.mkdirs()) throw new IOException("Répertoire impossible"); }
                    else {
                        File parent = out.getParentFile();
                        if (!parent.exists() && !parent.mkdirs()) throw new IOException("Répertoire parent impossible");
                        try (OutputStream output = new BufferedOutputStream(new FileOutputStream(out))) {
                            byte[] buf = new byte[32768]; int n;
                            while ((n = zin.read(buf)) > 0) {
                                totalSize += n;
                                if (totalSize > 500_000_000L) throw new IOException("Modèle décompressé trop volumineux");
                                output.write(buf, 0, n);
                            }
                        }
                    }
                    zin.closeEntry();
                }
            }
            File unpacked = new File(stage, NAME);
            if (!new File(unpacked, "am/final.mdl").isFile()) throw new IOException("Modèle Vosk incomplet");
            File finalDir = modelDir(c);
            File backupDir = new File(models, NAME + ".backup");
            deleteTree(backupDir);
            boolean hadModel = finalDir.exists();
            if (hadModel && !finalDir.renameTo(backupDir))
                throw new IOException("Impossible de sauvegarder l'ancien modèle ; installation annulée.");
            try {
                if (!unpacked.renameTo(finalDir)) copyTree(unpacked, finalDir);
                if (!new File(finalDir, "am/final.mdl").isFile())
                    throw new IOException("Vérification du modèle installé échouée.");
                deleteTree(backupDir);
            } catch (Exception e) {
                deleteTree(finalDir);
                if (hadModel && !backupDir.renameTo(finalDir))
                    throw new IOException("Échec de restauration du modèle : " + e.getMessage(), e);
                throw e;
            }
            progress.update("Modèle français installé et vérifié. Écoute hors écran disponible.");
        } finally {
            if (connection != null) connection.disconnect();
            zip.delete();
            deleteTree(stage);
        }
    }
    private static void copyTree(File from, File to) throws IOException {
        if (from.isDirectory()) {
            if (!to.exists() && !to.mkdirs()) throw new IOException("Impossible de créer : " + to);
            File[] children = from.listFiles();
            if (children != null) for (File child : children) copyTree(child, new File(to, child.getName()));
        } else {
            Files.copy(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
    private static void deleteTree(File f) {
        if (!f.exists()) return;
        if (f.isDirectory()) { File[] files = f.listFiles(); if (files != null) for (File item : files) deleteTree(item); }
        f.delete();
    }
}
