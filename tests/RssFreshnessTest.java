import fr.friday.assistant.RssFreshness;

public final class RssFreshnessTest {
    private static void ok(boolean value, String name) {
        if (!value) throw new AssertionError(name);
    }
    public static void main(String[] args) {
        long now = RssFreshness.publishedMillis("Thu, 19 Nov 2026 15:00:00 GMT");
        long recent = RssFreshness.publishedMillis("Thu, 19 Nov 2026 12:30:00 GMT");
        long stale = RssFreshness.publishedMillis("Wed, 18 Nov 2026 03:00:00 GMT");
        long future = RssFreshness.publishedMillis("Fri, 20 Nov 2026 03:00:00 GMT");
        ok(now > 0 && recent > 0, "RFC1123 RSS parse");
        ok(RssFreshness.withinHours(recent, now, 7), "recent within 7h");
        ok(!RssFreshness.withinHours(recent, now, 2), "reject older than 2h");
        ok(!RssFreshness.withinHours(stale, now, 7), "reject stale");
        ok(!RssFreshness.withinHours(future, now, 7), "reject future");
        ok(!RssFreshness.withinHours(0, now, 7), "missing timestamp not recent");
        ok(!RssFreshness.withinHours(recent, now, 0), "invalid zero window");
        ok(RssFreshness.publishedMillis("invalid") == 0, "malformed date");
        ok(RssFreshness.display(0).contains("non vérifiée"), "unknown date label");
        System.out.println("RssFreshnessTest OK");
    }
}
