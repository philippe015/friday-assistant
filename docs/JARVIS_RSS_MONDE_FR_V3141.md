# JARVIS — actualités RSS mondiales en français (v3.14.1, préversion)

## Objectif

JARVIS peut rechercher des **bulletins internationaux RSS datés** et traduire les titres et courts extraits en français **à l'aide du modèle local** `gemma-4-E2B-it-gpu.litertlm` déjà importé sur le téléphone.

Aucune garantie d'accès à *tous* les RSS de la planète : le catalogue actuel contient **17 flux / éditions géographiques** en français, anglais, espagnol, portugais, allemand, japonais, arabe, hindi et italien. Seuls les flux sélectionnés, au plus sept, sont interrogés pour une question.

## Exemples à dire ou écrire

- « Jarvis, donne les actualités mondiales des dernières 24 heures, traduites en français. »
- « Quelles sont les actualités au Japon aujourd'hui ? »
- « Jarvis, les actualités de l'Amérique latine. »
- « Les actualités internationales des 7 dernières heures. »

Le cockpit contient le bouton **RSS Monde FR** pour le premier exemple.

## Réglages

Ouvrir **Réglages → Internet Jarvis → Actualités mondiales** :
- Conserver « Autoriser Jarvis à rechercher des informations sur Internet ».
- Cocher **Traduire les titres et extraits RSS internationaux en français avec Gemma local** (activé par défaut).
- Si l'option est décochée ou si le modèle échoue, les textes originaux restent visibles. Aucun service de traduction externe n'est appelé.

## Sources et vérification

- Franceinfo Monde (français) ; BBC World, Guardian, Deutsche Welle, Al Jazeera (anglais).
- NHK (japonais), Tagesschau (allemand).
- Google Actualités par édition : États-Unis, Espagne, Brésil, Japon, pays arabes, Inde, Afrique du Sud, Italie, Canada, Australie.

Les flux publics peuvent changer d'URL, refuser un accès automatisé ou renvoyer peu d'articles. Le programme les interroge en HTTPS sous délais et taille bornés, ignore les publications sans date RSS exploitable et dédoublonne les articles. Le bulletin indique toujours les sources, liens, dates RSS et titres originaux.

**La date RSS n'est pas nécessairement la date de l'événement ou la date de première publication.** Les traductions de Gemma peuvent comporter des erreurs. JARVIS n'invente pas un titre s'il n'y a pas de flux récent exploitable.

## Confidentialité et modèles

La traduction est réalisée uniquement sur Android avec le modèle déjà installé. Aucune API de traduction cloud ou Mistral n'est utilisée. Les recherches RSS ne transmettent ni historique conversationnel, ni notes privées, ni coordonnées GPS. Les notes privées ne sont pas injectées au prompt de traduction locale.

Les modèles IA ne sont ni téléchargés ni remplacés automatiquement. Pour un appareil qui utilise déjà `gemma-4-E2B-it-gpu.litertlm`, conserver ce fichier.

## Installation et statut

Cette préversion **v3.14.1** est distincte de la **v3.14.0 Intelligence+ déjà validée sur téléphone**. L'édition debug de test possède l'identifiant `fr.friday.assistant.rsspreview`, différent de l'édition fonctionnelle `fr.friday.assistant.smartpreview`.

L'APK test **ne constitue pas une mise à jour en place** : les fichiers de modèle et l'historique chiffré ne sont pas partagés entre les deux packages. Il faut éventuellement réimporter Gemma 4 E2B GPU dans l'édition RSS. **Ne pas désinstaller la v3.14.0 fonctionnelle.**

La compilation CI ne remplace pas la validation sur téléphone de l'écoute, de l'IA et des différents flux.
