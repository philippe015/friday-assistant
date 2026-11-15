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
        check(BargeInPolicy.isBareStopAnswer("Stop"), "arrêt vocal direct");
        check(BargeInPolicy.isBareStopAnswer("Arrête"), "arrêt direct accents");
        check(BargeInPolicy.isBareStopAnswer("Silence"), "silence direct");
        check(BargeInPolicy.isBareStopAnswer("Attends"), "pause directe");
        check(BargeInPolicy.isBareStopAnswer("Arrête de parler"), "arrêt verbal direct");
        check(!BargeInPolicy.isBareStopAnswer("Stop l'écoute"), "stop sans wake ne coupe pas microphone");
        check(!BargeInPolicy.isBareStopAnswer("Je ne veux pas stopper"), "éviter faux positif");
        check(BargeInPolicy.earlyBarge("stop", "Voici la réponse progressive"), "stop pendant streaming");
        check(BargeInPolicy.earlyBarge("j'ai une précision", "Voici ma réponse"), "précision sans wake pendant streaming");
        check(!BargeInPolicy.earlyBarge("stop", "Stop. Voilà pourquoi."), "éviter arrêt par écho");
        check(BargeInPolicy.earlyBarge("Friday stop", "Bonjour"), "stop avec wake");
        check(BargeInPolicy.earlyBarge("Friday, j'ai une précision", "Bonjour"), "précision avec wake");
        check(!BargeInPolicy.earlyBarge("bonjour", "Bonjour"), "ignorer son du haut-parleur");
        check(BargeInPolicy.contextualFollowUp("Friday, donne plus de précisions"), "demande de détails conserve sujet");
        check(BargeInPolicy.contextualFollowUp("Peux-tu préciser ?"), "demande de précision conserve sujet");
        check(!BargeInPolicy.isStopAnswer("stop"), "ne pas désactiver la validation wake des commandes générales");
        check(BargeInPolicy.isBareStopAnswer("Stop stop"), "stop répété deux fois");
        check(BargeInPolicy.isBareStopAnswer("Stop, stop, stop !"), "stop répété trois fois");
        check(BargeInPolicy.isBareStopAnswer("STOP STOP STOP STOP STOP"), "stop répété cinq fois");
        check(BargeInPolicy.isBareStopAnswer("Arrête, arrête !"), "arrête répété");
        check(BargeInPolicy.isBareStopAnswer("stop arrête stop"), "ordres alternés");
        check(BargeInPolicy.isBareStopAnswer("Stop tout de suite"), "arrêt immédiat");
        check(!BargeInPolicy.isBareStopAnswer("stop stop de parler de cinéma"), "ne pas détourner une question");
        check(!BargeInPolicy.isBareStopAnswer("stop and go"), "faux positif en conversation");
        check(BargeInPolicy.earlyBarge("stop stop stop", "Voici mon explication"),
                "répétition pendant streaming coupe la voix");
        check(!BargeInPolicy.earlyBarge("stop stop", "On dit stop stop pour arrêter"),
                "anti-écho de la commande répétée");
        System.out.println("BargeInPolicyTest OK (44 tests)");
    }
}
