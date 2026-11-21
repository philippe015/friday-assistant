import fr.friday.assistant.SherpaArchivePolicy;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class SherpaArchivePolicyTest {
    private static final String NAME = "sherpa-onnx-streaming-zipformer-fr-2023-04-14";
    private static void check(boolean p, String why) {
        if (!p) throw new AssertionError(why);
    }
    private static void reject(byte[] bytes, String why) throws Exception {
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            try (InputStream valid = SherpaArchivePolicy.checkedBzip2(in)) {
                throw new AssertionError(why);
            } catch (IOException expected) {
                check(expected.getMessage().contains("format incorrect"),
                        "Must explain corrupt archive: " + expected.getMessage());
            }
        }
    }
    public static void main(String[] args) throws Exception {
        check(SherpaArchivePolicy.acceptedDisplayName(NAME + ".tar.bz2"), "official");
        check(SherpaArchivePolicy.acceptedDisplayName(NAME + ".tar.bz"), "Android .bz truncation");
        check(SherpaArchivePolicy.acceptedDisplayName(NAME + " (1).tar.bz2"), "numbered before ext");
        check(SherpaArchivePolicy.acceptedDisplayName(NAME + ".tar.bz2 (2)"), "numbered after ext");
        check(SherpaArchivePolicy.acceptedDisplayName(NAME.toUpperCase() + ".TAR.BZ2"), "case");
        check(SherpaArchivePolicy.acceptedDisplayName(null), "provider has no DISPLAY_NAME");
        check(!SherpaArchivePolicy.acceptedDisplayName("random.tar.bz2"), "other model rejected");
        check(!SherpaArchivePolicy.acceptedDisplayName(NAME + ".tar.bz2.crdownload"),
                "unfinished Chrome download");
        check(!SherpaArchivePolicy.acceptedDisplayName(NAME + ".zip"), "wrong archive type");
        reject("<html>error</html>".getBytes("UTF-8"), "HTML is not an archive");
        reject(new byte[]{'B', 'Z', 'h'}, "truncated archive");
        try (InputStream in = SherpaArchivePolicy.checkedBzip2(
                new ByteArrayInputStream(new byte[]{'B', 'Z', 'h', '9', 7, 8}))) {
            check(in.read() == 'B' && in.read() == 'Z' && in.read() == 'h'
                    && in.read() == '9' && in.read() == 7, "header preserved");
        }
        System.out.println("SherpaArchivePolicyTest OK");
    }
}
