import fr.friday.assistant.HttpRetryPolicy;

public final class HttpRetryPolicyTest {
    private static void yes(boolean condition) {
        if (!condition) throw new AssertionError("Échec validation reprise HTTP");
    }
    public static void main(String[] args) {
        yes(HttpRetryPolicy.retryable(429));
        yes(HttpRetryPolicy.retryable(500));
        yes(HttpRetryPolicy.retryable(502));
        yes(HttpRetryPolicy.retryable(503));
        yes(HttpRetryPolicy.retryable(504));
        for (int status : new int[]{400,401,403,404,422,200}) yes(!HttpRetryPolicy.retryable(status));
        yes(HttpRetryPolicy.MAX_ATTEMPTS == 3);
        yes(HttpRetryPolicy.retryAfterMs("2", 0) == 2000);
        yes(HttpRetryPolicy.retryAfterMs("300", 0) == 5000);
        yes(HttpRetryPolicy.retryAfterMs("impossible", 0) == -1);
        long first = HttpRetryPolicy.delayMs(1, null, 0);
        yes(first >= 450 && first < 700);
        long second = HttpRetryPolicy.delayMs(2, null, 0);
        yes(second >= 1050 && second < 1300);
        yes(HttpRetryPolicy.delayMs(1, "3", 0) == 3000);
        System.out.println("HttpRetryPolicyTest OK");
    }
}
