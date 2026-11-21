package fr.friday.assistant;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Validation légère du fichier Sherpa français sélectionné dans Android.
 * Le nom affiché par un fournisseur SAF peut être absent ou renommé (1).
 * Le format et les quatre fichiers exacts sont vérifiés pendant l'extraction.
 */
public final class SherpaArchivePolicy {
    private static final String STEM = "sherpa-onnx-streaming-zipformer-fr-2023-04-14";
    private static final Pattern FILE_NAME = Pattern.compile(
            Pattern.quote(STEM) + "(?: \\([1-9][0-9]?\\))?\\.tar\\.bz2?(?: \\([1-9][0-9]?\\))?");
    private SherpaArchivePolicy() { }

    public static boolean acceptedDisplayName(String displayed) {
        if (displayed == null || displayed.trim().isEmpty()) {
            // Certains fournisseurs de documents ne transmettent pas DISPLAY_NAME.
            return true;
        }
        return FILE_NAME.matcher(displayed.trim().toLowerCase(Locale.ROOT)).matches();
    }

    /**
     * L'extension .tar.bz peut venir d'un renommage Android. Elle n'autorise
     * jamais un fichier invalide : contrôler l'en-tête BZip2 avant extraction.
     */
    public static InputStream checkedBzip2(InputStream input) throws IOException {
        if (input == null) throw new IOException("Archive introuvable ou inaccessible.");
        PushbackInputStream checked = new PushbackInputStream(input, 4);
        byte[] header = new byte[4];
        int read = 0;
        while (read < header.length) {
            int n = checked.read(header, read, header.length - read);
            if (n < 0) break;
            if (n == 0) continue;
            read += n;
        }
        if (read != 4 || header[0] != 'B' || header[1] != 'Z'
                || header[2] != 'h' || header[3] < '1' || header[3] > '9') {
            throw new IOException("Téléchargement incomplet ou format incorrect. "
                    + "Il faut une véritable archive .tar.bz2 (BZip2), non une page Web.");
        }
        checked.unread(header);
        return checked;
    }
}
