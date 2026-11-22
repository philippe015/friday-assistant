# JARVIS · Intelligence+ Kotlin

## Objectif

Améliorer l'interprétation locale des questions sans augmenter artificiellement
la « puissance » du modèle LiteRT-LM : Kotlin sert ici de répartiteur
déterministe avant les moteurs de recherche et l'IA générative.

## Code intégré

- `JarvisKotlinBrain.kt` : module Kotlin sans réseau ni accès au stockage.
- `FridayService.java` : confirmation de reconnaissance Vosk lorsque sa confiance
  est réellement faible ; clarification sur le téléphone avant toute recherche.
- `MainActivity.java` : mêmes clarifications dans le dialogue écrit.
- `tests/test_kotlin_intent_router_20261122.py` : vérifications d'intégration,
  cas réels du rapport et compilation JVM si Kotlin CLI est disponible.

## Règles

1. Ne jamais considérer l'absence de score Vosk comme une erreur.
2. Une demande avec mot d'activation reconnu bénéficie d'un seuil moins strict.
3. Les courtes réponses (« non », « OK », « merci ») ne sont jamais envoyées
   comme mots-clés à Wikipédia ou à un moteur de recherche.
4. Une phrase ou une référence d'article ambiguë déclenche une précision
   locale au lieu d'inventer un article ou un lieu.
5. Une ville seule peut déclencher une demande de confirmation après une
   question météo récente ; le serveur météo existant reste seul responsable
   des prévisions factuelles.
6. Le texte des questions n'est ni journalisé par ce module, ni synchronisé.

## Limites

Le module ne change ni les poids du modèle, ni ses tokens disponibles, ni
la qualité intrinsèque du micro. Les tests Android avec Vosk, écoute continue
et TTS restent nécessaires. Le modèle LiteRT-LM existant reste utilisé pour
les questions plus complexes ; Open-Meteo, RSS et GPS conservent leurs
moteurs spécifiques. Aucun abonnement, clé API ou service distant n'est
ajouté par ce module.

## Tests manuels

- Demander « Non » après une question de précision ; aucune recherche Web.
- Dire « Tout de suite, c'est tout de suite » ; demande de clarification.
- Dire « Article trois en entier » ; la sélection RSS existante reste active.
- Demander « Météo pour Joinville-le-Pont 94 » ; le moteur météo reste actif.
- Tester une phrase faible confiance Vosk puis une vraie demande claire.
- Contrôler les événements codifiés, sans envoyer les conversations en clair.

Ne pas fusionner ni présenter comme une version stable sans compilation
Android et essais sur téléphone.