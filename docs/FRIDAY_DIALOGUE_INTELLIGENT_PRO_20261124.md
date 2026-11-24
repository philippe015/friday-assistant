# FRIDAY 3.14.27 — Dialogue Intelligent Pro+

Préversion Android, branche dérivée de FRIDAY 3.14.26, sans changement du
package release Google Play.

## Fonctionnalités

- Nouvelle politique ConversationTurnPolicy **locale**, sans réseau, pour
  les relances elliptiques courtes : « Et pourquoi ? », « Continue »,
  « Peux-tu détailler la deuxième étape ? », « Et pour lui ? »,
  « Donne-moi des exemples », etc.
- La décision de routage est prise **dans les deux parcours**, vocal
  (FridayService) et écrit/Matrix (MainActivity) ; une relance réellement
  contextuelle avec des échanges récents est envoyée au modèle IA local
  **sans recherche Web**, même si le réglage Web « toujours » est activé.
- Les demandes explicitement actuelles, météo, actualités, URL, recherche
  Web et articles RSS conservent leurs outils spécialisés. Aucun détournement
  des commandes d'articles numérotés.
- Une question sans antécédent identifiable donne lieu à une clarification
  ciblée, **sans inventer** un sujet, un lieu, une personne ou une liste.
- Les commandes explicites « Changeons de sujet », « Nouveau sujet » et
  équivalents répondent en demandant le nouveau sujet.
- L'historique privé AES-GCM de Android est extrait localement. Pour les
  relances courtes, jusqu'à 16 messages récents (8 paires maximum) sont
  choisis avec des extraits bornés ; 10 messages pour les questions
  ordinaires. Les anciennes références thématiques conservent leur statut
  d'indices non vérifiés. Les modèles ne reçoivent qu'une fenêtre
  compacte ; ce n'est **pas** une mémoire intégrale.
- Une directive explicite à l'IA locale lui demande de résoudre les pronoms
  à partir des échanges précédents et de solliciter une précision si
  nécessaire, sans inventer de sources.

## Banc d'essai de dialogue (six tours)

Dans Terminal Matrix → **DIALOGUE INTELLIGENT PRO+** :

1. « Je crée une application de suivi de plantes ... sans Internet »
2. « Quel premier écran proposerais-tu ? »
3. « Pourquoi ce choix ? »
4. « Donne une fonctionnalité supplémentaire adaptée à ce projet »
5. « Quel problème peut survenir sans connexion ? »
6. « Résume nos décisions et distingue-les de tes suggestions »

Le banc d'essai exécute les six questions *sur le modèle réellement chargé*
(MediaPipe ou LiteRT-LM), en lui repassant les réponses précédentes
de cette conversation **fictive et publique**. Il mesure les temps,
affiche les réponses brutes, permet l'arrêt et chiffre le rapport
sur Android avec le Keystore AES-GCM. Aucune conversation personnelle
n'est utilisée, aucun accès Internet ou GPS, aucun appel API d'IA.
La copie vers le presse-papiers nécessite une action explicite et n'est
pas chiffrée.

**Les indices lexicaux ne prouvent pas que le modèle comprend ou se
souvient correctement : lisez les six réponses.** Ce test ne certifie
pas la cohérence sur des conversations réelles plus longues.

## Vérification

Tests Java/Python de décisions de routage, six tours synthétiques,
clarification sans contexte, séparation données privées/Web, boutons et
état de diagnostic, plus compilation Gradle Android ARM64.

DEBUG : `fr.friday.assistant.v31421matrix`,
version `3.14.27-dialogue-pro-preview`, code `121`.
Release Google Play inchangée : `3.14.18`, code `112`.

Le GitHub PR est chaîné sur FRIDAY 3.14.26, non automatiquement
fusionné à la branche principale.
