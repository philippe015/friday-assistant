# FRIDAY v3.13.2 — Jarvis Internet, inférence locale

## Fonctionnement
Jarvis reste le **seul moteur d'IA**. Un modèle Android compatible `.task` (MediaPipe) ou `.litertlm` (LiteRT-LM) doit être installé sur le téléphone.

Quand la case **Autoriser Jarvis à rechercher des informations sur Internet** est activée :
1. FRIDAY détermine si la question requiert Internet (recherche explicite, URL HTTPS, fait actuel, actualités).
2. FRIDAY envoie **la requête de recherche uniquement** à Bing RSS. En cas d'échec, il essaie DuckDuckGo HTML, puis Google Actualités RSS pour les questions d'actualité.
3. Les résultats sont filtrés : HTTPS public uniquement, pas d'adresses intranet/localhost, trois redirections maximum pour une page, volumes et délais bornés. Une page inaccessible (403, 429, JavaScript obligatoire) n'est pas contournée.
4. Les pages publiques accessibles sont lues, les extraits limités en taille et traités comme **données non fiables**.
5. L'application fournit ces extraits au **modèle local**, qui produit la réponse en français. Aucune question n'est envoyée à Mistral ni à un autre LLM distant.
6. Les sources et URL sont ajoutées à la réponse écrite ; la voix lit l'explication sans épeler les URL.

## Réglages et diagnostics
- Réglages > **Internet Jarvis** : activer/désactiver la recherche.
- **Tester l'accès Internet Jarvis** vérifie réellement l'obtention de résultats sans demander une inférence LLM.
- **Tester Jarvis IA locale sans Internet** vérifie séparément le chargement et la génération locale.
- Le diagnostic Internet doit réussir **et** le test IA local doit réussir pour obtenir une réponse Web synthétisée.

## Données personnelles
Le texte des recherches est transmis aux moteurs de recherche ; la consultation d'une page transmet normalement l'IP aux sites visités. **L'historique chiffré, les notes personnelles, les coordonnées GPS et les clés API ne sont pas envoyés.** Ne mettez pas d'information confidentielle dans une recherche Internet. Le modèle peut faire des erreurs malgré les sources.

## Limitations
- Recherche publique HTTPS : pas d'accès automatique aux comptes, réseaux privés, pages payantes, ou sites exigeant JavaScript.
- Les moteurs publics peuvent limiter ou modifier leur interface. Un message d'erreur est affiché plutôt qu'une fausse actualité.
- La qualité des résultats et de la synthèse dépend du réseau et du modèle local choisi.
- Compilation et tests statiques GitHub ne remplacent pas une validation sur le téléphone.

## Tests sur l'appareil
1. Mettre à jour FRIDAY sans désinstaller l'ancienne version.
2. Vérifier le modèle local et effectuer son test.
3. Activer Internet Jarvis et effectuer le test de recherche.
4. Demander « Cherche les actualités scientifiques d'aujourd'hui et cite tes sources. »
5. Confirmer les URL visibles dans la conversation et le rendu vocal français sur le haut-parleur du téléphone.
6. Désactiver Internet Jarvis : les recherches doivent cesser et les réponses locales restent disponibles.
