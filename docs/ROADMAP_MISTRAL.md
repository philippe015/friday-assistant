# Feuille de route FRIDAY — Mistral uniquement

> Plan validé le 15 novembre 2026. **Documentation de projet, pas une déclaration de fonctions déjà implémentées.**
>
> Dépôt : [philippe015/friday-assistant](https://github.com/philippe015/friday-assistant) · Base : **FRIDAY v3.4.1**.

## Vision et principes non négociables

FRIDAY doit devenir un assistant personnel **fiable, réactif, conversationnel et vérifiable** :

- **Identité** : le nom, les comportements et le style de l'assistant proviennent du **profil de personnalité actif**. Mistral est le fournisseur de génération, pas l'identité de FRIDAY.
- **Mistral uniquement pour l'IA** : aucune réintroduction de DeepSeek ni restauration de la v2.4.0 ; possibilité d'ajouter plus tard des services annexes uniquement après décision explicite.
- **Position GPS** : l'activation du GPS Android ne vaut **pas** autorisation de transmission à Mistral. Les coordonnées ne sont partagées que ponctuellement, pour une question locale et avec consentement. La précision annoncée doit être réellement mesurée, jamais garantie.
- **Recherche Web** : ne prétendre consulter Internet que lorsque le connecteur l'a réellement fait ; distinguer modèle compatible, outil disponible, échec et sources vérifiables.
- **Dialogue** : l'utilisateur peut interrompre la parole et préciser sa question ; une réponse obsolète ne doit pas être prononcée après interruption.
- **Sécurité et continuité** : préserver les clés/API, profils chiffrés, données locales, signature Android et mécanismes d'annulation. Toute nouvelle fonction exige tests, documentation et vérification sur appareil.

## État du code avant les travaux

| Composant | Constat sur v3.4.1 | Conclusion |
|---|---|---|
| Fournisseur IA | Mistral seul, Chat Completions et Conversations | **Conserver** |
| Profil actif | Injecté dans les instructions ; réponse d'identité locale pour les questions explicites | **Tester contre régressions** |
| GPS | Acquisition Android à la demande ; partage Mistral distinct et autorisé explicitement | **Fiabiliser et tester sur téléphone** |
| Web | Outil Mistral `web_search` via Conversations | **Tester réellement compte/modèle/sources** |
| Modèles | `GET /v1/models` disponible | **Ajouter cache et vérification des capacités** |
| Lecture vocale | TTS Android ; réponse Mistral Chat en `stream: false` | **Ajouter streaming progressif** |
| Réseau | Annulation HTTP disponible ; pas de reprise transitoire générale | **Ajouter backoff annulable** |
| Reconnaissance | Vosk français hors connexion, confiance et anti-écho | **Mesurer et améliorer** |

Le rapport d'origine décrivait encore **v3.2.0 / DeepSeek** : cette référence était obsolète dès la validation du plan. Les chiffres de latence et de coût donnés dans ce rapport sont **des hypothèses**, non des engagements de performance ni des tarifs contractuels.

## v3.5.0 — Fiabilité d'abord

Suivi : [Issue #29 — Fiabilité Mistral, GPS, Web et diagnostics](https://github.com/philippe015/friday-assistant/issues/29).

1. Conserver et renforcer les tests de l'identité et du **prompt de profil actif**, aussi bien en Chat qu'en mode Web.
2. Tester systématiquement l'acquisition GPS, le redémarrage nécessaire du service Android et le **consentement indépendant** `location_share`.
3. Diagnostiquer l'accès aux connecteurs Mistral : modèle, offre, HTTP, exécution de l'outil, références effectivement fournies.
4. Ajouter des **reprises limitées** pour `429` et erreurs transitoires `5xx` : backoff exponentiel avec aléa, respect de `Retry-After` si présent, durée maximale et annulation immédiate lors d'une interruption.
5. Ajouter la découverte et une mémorisation temporaire de la liste de modèles ; vérifier leur compatibilité réelle avant de changer le modèle actif.
6. Exposer des diagnostics utiles **sans stocker la clé API, les coordonnées GPS ou le contenu des profils dans les journaux**.

**Livrable attendu :** version Android signée avec tests réseau simulés et essais GPS/Web réels, sans régression de v3.4.1.

## v3.6.0 — Streaming et conversation plus rapide

Suivi : [Issue #30 — Streaming et TTS progressif](https://github.com/philippe015/friday-assistant/issues/30).

1. Lire de façon robuste les évènements SSE de Mistral Chat Completions (`stream: true`), avec traitement des fragments Unicode et des fins de flux.
2. Constituer des **phrases complètes** à partir des fragments, puis les passer au TTS Android au fur et à mesure.
3. Ajouter une **file TTS bornée** et un mécanisme d'arrêt atomique : abandon de l'ancienne génération, vidage de la file, arrêt de la voix et maintien du sujet précédent pour une précision.
4. Prévoir un repli non-streaming si le flux n'est pas supporté ; ne pas casser le chemin Conversations/Web.
5. Mesurer la **latence avant la première parole**, la durée totale, les interruptions et les échecs sur téléphone. Ne pas promettre de délai fixe sans mesures.

**Livrable attendu :** voix progressive fluide, interruptions fiables, comparatif chiffré avant/après et APK testé.

## v3.7.0 — Compréhension vocale avancée

Suivi : [Issue #31 — Reconnaissance française et batterie](https://github.com/philippe015/friday-assistant/issues/31).

1. Tester un **vocabulaire Vosk spécialisé optionnel** : noms propres, lieux, commandes, termes professionnels, sans dégrader les questions ouvertes.
2. Mieux présenter le choix des modèles Mistral avec leurs capacités réelles (chat, Web, vision si disponible).
3. Étudier un **ASR Mistral Voxtral** facultatif ; rendre son coût, l'envoi audio et les autorisations transparents, avec conservation du fonctionnement Vosk hors connexion.
4. Proposer un mode **push-to-talk / écoute non continue** pour réduire la batterie, sans supprimer l'écoute continue démarrée volontairement.
5. Étudier un modèle Vosk plus grand et la voix clonée **seulement en option**, avec mesures et consentement.

**Livrable attendu :** meilleure compréhension mesurée (taux d'erreurs et cas d'usage), économies de batterie mesurées et parcours de consentement clair.

## Stratégie de qualité et livraison

Chaque changement doit suivre une branche dédiée, une pull request, les tests automatisés existants et une **compilation Android CI réussie** avant fusion. Chaque version publiée doit fournir :

- APK signé et lien GitHub Releases vérifié ;
- tests Java/Python/Rust/JS pertinents, tests de régression de l'identité et des permissions ;
- validation sur appareil **séparément** des validations CI ;
- changelog indiquant ce qui est réellement implémenté, partiel ou non testé ;
- pas de nouvelle API tierce ni de migration de stockage non demandée.

### Décisions retenues / reportées

**Validé :** robustesse réseau, diagnostic Web/GPS, streaming, TTS progressif, vocabulaire Vosk optionnel, découverte des modèles, mesure de latence.

**Non validé :** retour à DeepSeek, restauration brute d'une ancienne version, affirmation « première parole à 0,8 s », accès GPS sans consentement, recherche Web simulée.

**Reporté :** voix clonée et reconnaissance cloud obligatoire ; ces options demandent une étude de confidentialité, de coût et de performance.
