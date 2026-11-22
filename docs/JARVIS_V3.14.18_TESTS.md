# JARVIS Android v3.14.18 — correctifs en validation

Branche : `feature/jarvis-v31418-memory-gps-20261122`
Base : v3.14.17 (`feature/extended-conversation-memory-v31417-20261122`)

## Pourquoi cette version

Le rapport de conversation du 22/11/2026 signale :
- « Actualités 2 en entier » déclenche une nouvelle recherche au lieu de lire l'article n° 2.
- « Quelle est ma position GPS actuelle ? » et « Quels services sont disponibles autour de moi ? » en mode écrit reçoivent une réponse générique.
- La voix de synthèse issue du dialogue écrit risque d'être reprise par Vosk.

Le rapport contient des conversations privées ; il n'est pas ajouté au dépôt.

## Changements

1. **Article numéroté :** les variantes « actualités 2 », « articles deux » et « l'article 2 en entier » font référence à l'index de la dernière liste RSS chiffrée, déjà gérée par `JarvisInternet`. S'il n'y a aucune liste, JARVIS demande d'en consulter une. Un texte intégral soumis au droit d'auteur ou inaccessible n'est pas inventé.
2. **GPS en texte :** passe directement par `FridayLocation` (permissions, fournisseur Android, âge, précision). Les coordonnées ne sont pas historisées par défaut.
3. **Services voisins :** OpenStreetMap/Overpass, opt-in séparé. Autorisation Android + `nearby_online_enabled` + Internet JARVIS + question explicite nécessaires. Coordon­nées transmises arrondies à trois décimales (~100 mètres), rayon 900 m, six résultats maximum. Les fiches de commerces restent indicatives.
4. **Anti-écho TTS :** le TTS de l'écran de dialogue écrit suspend le décodage général Vosk jusqu'à la fin du dernier segment, puis applique 2,5 s de queue d'écho.
5. **Diagnostics :** événements techniques codifiés (GPS, Overpass, voix d'interface) exclusivement dans le journal chiffré. Aucun dialogue ni GPS brut ajouté aux logs.

## Vérifications sur Android

- Poser une question « Donne les trois dernières actualités ». Demander « Actualités 2 en entier » par écrit, puis « Articles 2 en entier » à la voix. Vérifier l'identité du titre et l'absence de nouveau bulletin.
- Dans les réglages, activer la localisation et la permission Android, puis demander « Quelle est ma position GPS actuelle ? » **par écrit**.
- Sans autorisation OpenStreetMap, demander « Quels services sont autour de moi ? » : aucun appel externe ne doit être lancé.
- Activer la case OpenStreetMap : répéter la question et contrôler les noms, distances approximatives, et éventuel message d'indisponibilité (HTTP 429/502/504).
- Laisser la voix Android lire une longue réponse écrite avec le microphone en marche : Vosk ne doit pas lancer une question issue du haut-parleur. Tester aussi une courte vraie question après 2,5 s.
- Exporter un journal de diagnostic **sans conversation** et vérifier qu'il ne contient pas de coordonnées, d'URL ni de propos bruts.

## Publication / limites

La compilation GitHub Actions de PR produit un **APK debug** de test, pas une version stable signée. Son identifiant Android de test est `fr.friday.assistant.v31418preview` : il s'installe à côté des versions existantes et ne remplace pas leurs données. Sa signature peut être différente de celle de l'APK déjà installé : **ne pas désinstaller l'application utilisée** sans sauvegarde sûre de la mémoire chiffrée et des préférences.

La fusion et la publication d'une version release v3.14.18 exigent les tests CI verts, un essai réel sur Android, et une signature compatible avec les versions distribuées.
