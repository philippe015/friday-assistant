import fr.friday.assistant.WebPolicy;
public class WebPolicyTest {
 public static void main(String[] args) {
  if (!WebPolicy.shouldSearch("always", false, false)) throw new AssertionError("always");
  if (!WebPolicy.shouldSearch("auto", false, true)) throw new AssertionError("auto current");
  if (WebPolicy.shouldSearch("auto", false, false)) throw new AssertionError("auto simple");
  if (WebPolicy.shouldSearch("off", true, true)) throw new AssertionError("off local");
  if (!WebPolicy.shouldSearch("auto", true, false)) throw new AssertionError("local");
  System.out.println("WebPolicyTest OK");
 }
}
