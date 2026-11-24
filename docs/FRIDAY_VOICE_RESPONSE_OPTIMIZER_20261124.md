# FRIDAY/JARVIS 3.14.29 — Voice Response Optimizer+

Préversion Android DEBUG du 24 novembre 2026, dérivée de la branche FRIDAY 3.14.28. Aucune fusion ni publication Google Play implicite.

## Pourquoi cette version ?

Le rapport de FRIDAY 3.14.28 a confirmé une réponse TV préparée en **1 560 ms**, mais une lecture vocale d'environ **54,5 secondes** pour six programmes. Certaines apostrophes apparaissaient comme `&#039;` dans les titres. Une transcription Vosk contenait `service donne-moi le programme...`.

## Correctifs

1. **Lecture vocale raccourcie** : la réponse écrite conserve toutes les chaînes et horaires extraits du guide public, tandis que le texte destiné au TTS ne lit que les **deux premiers programmes au maximum**, puis propose de demander une chaîne particulière.
2. **Apostrophes et HTML** : décodage des entités HTML numériques décimales et hexadécimales ainsi que des entités usuelles dans les titres TV. Les balises HTML/scripts ne sont pas exécutés ; le contenu vient d'une extraction HTTPS publique.
3. **Détail conversationnel d'une chaîne** : après une consultation du guide français valide, `Détaille France 4` ou `Et sur France 4 ?` restitue uniquement **chaîne, titre, heure et type film** déjà vérifiés sur le guide. Le **synopsis n'est pas inventé**, ni une fiche non consultée. Cette réponse réutilise un instantané limité au même jour à Paris et vieux de moins de six heures, même sans relancer le Web.
4. **Confidentialité** : les horaires et titres publics sont conservés, si possible, dans `SecurePrefs` sous chiffrement AES-GCM Android Keystore. Aucune conversation personnelle, position GPS ni contenu de profil n'est envoyé au guide TV.
5. **Vosk, correction très ciblée** : dans une **question directe déjà autorisée**, `service donne-moi le programme TV` peut être ramené à `donne-moi le programme TV`. Le mot « service » n'est **jamais ajouté aux mots de réveil**, afin d'éviter les déclenchements intempestifs. Les autres dictées et le texte écrit ne sont pas modifiés.
6. **Reprise du dialogue** : tous les traitements préexistants d'anti-écho, réactivation du microphone, reconnaissance Vosk et interruption manuelle sont conservés.

## Limites

- La synthèse vocale reste dépendante du moteur TTS du téléphone et sa durée ne peut être garantie.
- Une grille TV non datée, périmée, bloquée ou inaccessible ne fournit aucun horaire prétendument vérifié.
- Le détail de chaîne n'est pas un synopsis éditorial ; il reprend la dernière grille vérifiée et expire après six heures ou changement de jour à Paris.
- L'IA locale LiteRT-LM reste inchangée. La correction Vosk ne garantit pas une reconnaissance parfaite de tous les accents ou bruits.

## Validation prévue

- Tests Java de décodage `&#039;`, `&#x2019;`, extraction de six chaînes fictives et réponse TTS courte ;
- Tests des questions de suivi, des différences de chaînes et de l'expiration du cache ;
- Tests de non-régression Web/Vosk/Mémoire et compilation Android ARM64 via GitHub Actions ;
- Tests ultérieurs sur téléphone : demander « programme TV de ce soir », vérifier réponse écrite complète mais TTS bref, puis « Détaille France 4 ».

Version DEBUG `3.14.29-voice-response-optimizer-preview`, versionCode **123**, package `fr.friday.assistant.v31421matrix`. Version de publication Google Play inchangée `3.14.18` / code `112`.
