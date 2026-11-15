import fr.friday.assistant.MicSignalMeter;

public final class MicSignalMeterTest {
    private static void yes(boolean ok, String msg) { if (!ok) throw new AssertionError(msg); }

    public static void main(String[] args) {
        yes(MicSignalMeter.level(null, 0) == 0, "null");
        yes(MicSignalMeter.level(new byte[400], 400) == 0, "silence");
        byte[] strong = new byte[1600];
        for (int i = 0; i + 1 < strong.length; i += 2) {
            // 0x3FFF : échantillon de forte amplitude sans saturation.
            strong[i] = (byte) 0xff;
            strong[i + 1] = 0x3f;
        }
        int value = MicSignalMeter.level(strong, strong.length);
        yes(value > 60 && value <= 100, "signal de forte amplitude");
        yes(MicSignalMeter.level(strong, 0) == 0, "aucun octet reçu");
        yes(MicSignalMeter.level(strong, -1) == 0, "longueur invalide");
        yes(MicSignalMeter.level(strong, strong.length + 1000) == value,
                "lecture limitée à la longueur effective du tableau");
        System.out.println("MicSignalMeterTest OK");
    }
}
