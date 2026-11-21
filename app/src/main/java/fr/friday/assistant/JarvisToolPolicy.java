package fr.friday.assistant;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Deterministic, offline Android intent catalog inspired by OpenJarvis tools.
 * No shell commands, arbitrary code, external skill installation or model-output
 * execution. Restrict matching to questions explicitly about JARVIS/the phone.
 */
public final class JarvisToolPolicy {
    public enum Kind { NONE, CAPABILITIES, MAPS, MICROPHONE, MODEL, DEVICE, PRIVACY, SYSTEM }
    private JarvisToolPolicy() { }

    public static String fold(String utterance) {
        String text = Normalizer.normalize(utterance == null ? "" : utterance,
                        Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.FRENCH)
                .replaceAll("[^a-z0-9]+", " ").replaceAll("\\s+", " ").trim();
        return text.replaceFirst("^(?:jarvis|jarvise|jervis|jervi)\\s+", "");
    }

    public static Kind classify(String utterance) {
        String q = fold(utterance);
        if (q.isEmpty() || q.length() > 155) return Kind.NONE;
        if (q.matches("^(?:quelles? sont tes (?:fonctions|capacites|outils)|"
                + "que (?:peux|sais) tu faire|quels outils (?:as|utilises) tu|"
                + "montre (?:moi )?tes outils|liste (?:moi )?tes outils|"
                + "tes capacites|tes fonctions|tes outils|"
                + "capacites de jarvis|outils de jarvis|"
                + "comment fonctionne ton assistant)$"))
            return Kind.CAPABILITIES;
        // Demandes sur l'accès de JARVIS à Google Maps : réponse locale
        // vérifiable sur les permissions et outils réellement intégrés.
        // Ne pas traiter « map » seul : c'est ambigu dans une recherche générale.
        if (q.matches("^(?:as tu|est ce que tu as|tu as|avez vous|est ce que vous avez)"
                + " (?:un |l )?acces (?:a |au |aux )?(?:google )?(?:map|maps|google maps|carte|cartes)"
                + "(?: du telephone)?$")
                || q.matches("^(?:peux tu|est ce que tu peux|pouvez vous|sais tu)"
                + " (?:utiliser|ouvrir|consulter|acceder a|lire)"
                + " (?:google )?(?:map|maps|google maps|la carte|les cartes)$")) {
            return Kind.MAPS;
        }
        if (q.matches("^(?:diagnostic|diagnostic complet|bilan|etat|statut|"
                + "diagnostic du systeme|diagnostic de jarvis|"
                + "etat du systeme|etat de jarvis|statut de jarvis|"
                + "fais (?:un )?diagnostic(?: de jarvis| du systeme)?|"
                + "comment fonctionne jarvis|"
                + "est ce que jarvis fonctionne)$"))
            return Kind.SYSTEM;
        if (q.matches("^(?:etat du (?:micro|microphone)|statut du (?:micro|microphone)|"
                + "micro actif|microphone actif|le micro fonctionne|"
                + "est ce que (?:le micro|le microphone) (?:est actif|fonctionne)|"
                + "tu m entends|est ce que tu m entends|tu m ecoutes|"
                + "es tu a l ecoute|est ce que tu ecoutes|"
                + "pourquoi tu ne m entends pas|diagnostic microphone)$"))
            return Kind.MICROPHONE;
        if (q.matches("^(?:quel est ton modele(?: ia| local)?|"
                + "quel modele(?: ia| local)? utilises tu|"
                + "ton modele(?: ia| local)?|etat du modele(?: ia)?|"
                + "statut du modele(?: ia)?|"
                + "quel est ton moteur(?: ia)?|"
                + "est ce que ton modele(?: ia)? est (?:installe|charge|pret)|"
                + "as tu un modele(?: ia)? installe|"
                + "diagnostic modele(?: ia)?|"
                + "as tu gemma|gemma est il installe)$"))
            return Kind.MODEL;
        if (q.matches("^(?:etat (?:du|de mon) telephone|"
                + "diagnostic (?:du|de mon) telephone|"
                + "niveau de batterie|etat de la batterie|"
                + "quelle est (?:ma|la) batterie|"
                + "combien (?:reste|me reste) (?:t il )?de batterie|"
                + "combien d espace (?:libre|disponible) sur (?:mon|le) telephone|"
                + "espace (?:libre|disponible) du telephone|"
                + "memoire ram du telephone|memoire disponible du telephone)$"))
            return Kind.DEVICE;
        if (q.matches("^(?:est ce que tu (?:envoies|partages|transmets) (?:mes|nos) "
                + "(?:conversations|dialogues|donnees|messages)|"
                + "ou sont (?:stockees|conservees|enregistrees) (?:mes|nos) "
                + "(?:conversations|donnees|dialogues)|"
                + "comment proteges tu (?:mes|nos) donnees|"
                + "ta confidentialite|confidentialite de jarvis|"
                + "mes conversations restent elles sur le telephone)$"))
            return Kind.PRIVACY;
        return Kind.NONE;
    }
}
