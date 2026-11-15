import fr.friday.assistant.BargeInPolicy;
public class BargeInPolicyTest {
    static void check(boolean ok, String s) { if (!ok) throw new AssertionError(s); }
    public static void main(String[] args) {
        check(BargeInPolicy.startsWithWake("Friday, stop"), "wake");
        check(!BargeInPolicy.startsWithWake("Bonjour friday"), "mot clé pas au début");
        check(BargeInPolicy.isStopAnswer("Friday stop"), "arrêt de parole");
        check(BargeInPolicy.isStopAnswer("Friday, arrête la réponse"), "arrêt de réponse");
        check(!BargeInPolicy.isStopListening("Friday stop"), "stop != arrêt micro");
        check(BargeInPolicy.isStopListening("Friday, arrête l'écoute"), "arrêt complet");
        check(BargeInPolicy.wantsToAddDetails("Friday, j'ai une précision"), "précision");
        check(BargeInPolicy.isWakeOnly("Friday"), "wake seul");
        check(BargeInPolicy.earlyBarge("Friday explique moi autrement", "Bonjour je suis Friday"), "question interrupt");
        check(!BargeInPolicy.canBarge("Friday où est l'hôpital", "Friday où est l'hôpital proche, dit Friday où est l'hôpital"), "anti-echo");
        check(!BargeInPolicy.canBarge("le vendredi dernier", ""), "pas de wake");
        check(BargeInPolicy.wantsToAddDetails("Friday attends j'ai une précision"), "attends plus précision");
        check(BargeInPolicy.wantsToAddDetails("Friday, je voudrais préciser"), "demande correction");
        check(!BargeInPolicy.isStopAnswer("Friday attends j'ai une précision"), "précision ne doit pas être stop");
        check(BargeInPolicy.isStopAnswer("Friday attends"), "arrêt parole seul");
        check(BargeInPolicy.contextualFollowUp("Et demain à Paris ?"), "relance temporelle");
        check(BargeInPolicy.contextualFollowUp("Je voulais dire samedi"), "correction contexte");
        check(!BargeInPolicy.contextualFollowUp("Quelle est la population de Tokyo ?"), "nouvelle question indépendante");
        System.out.println("BargeInPolicyTest OK (18 tests)");
    }
}
