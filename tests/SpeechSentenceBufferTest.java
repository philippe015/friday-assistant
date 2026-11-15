import fr.friday.assistant.SpeechSentenceBuffer;
import java.util.List;

public final class SpeechSentenceBufferTest {
    static void ok(boolean yes) {
        if (!yes) throw new AssertionError("SpeechSentenceBuffer");
    }
    public static void main(String[] args) {
        SpeechSentenceBuffer buffer = new SpeechSentenceBuffer();
        ok(buffer.accept("Bonjour").isEmpty());
        ok(buffer.accept(" ! Comment ça va").size() == 1);
        ok(buffer.accept(" ? Merci. ").size() == 2);
        ok(buffer.flush().isEmpty());
        SpeechSentenceBuffer decimal = new SpeechSentenceBuffer();
        ok(decimal.accept("La température est de 12.").isEmpty());
        ok(decimal.accept("5 degrés. Bonne journée. ").size() == 2);
        SpeechSentenceBuffer longSpeech = new SpeechSentenceBuffer();
        ok(!longSpeech.accept("texte ".repeat(70)).isEmpty());
        ok(!longSpeech.flush().isEmpty());
        SpeechSentenceBuffer.EventLines events = new SpeechSentenceBuffer.EventLines();
        ok(events.accept("event: message") == null);
        ok(events.accept("data: {\"choices\":") == null);
        ok(events.accept("data: []}") == null);
        ok("{\"choices\":\n[]}".equals(events.accept("")));
        ok(events.accept("data: [DONE]") == null);
        ok("[DONE]".equals(events.flush()));
        System.out.println("SpeechSentenceBufferTest OK");
    }
}
