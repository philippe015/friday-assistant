# FRIDAY v3.8.1 — Accès Web Mistral et lecture HTTPS

## Diagnostic de la v3.8.0
Les requêtes de recherche d'informations récentes passent par `/v1/conversations` avec `tools: [{"type":"web_search"}]`. L'ancienne version passait silencieusement à Chat Completions lorsqu'une recherche échouait : une réponse sans Internet pouvait alors être affichée malgré une demande explicite.

Le mode streaming de Chat Completions ne donne pas directement accès à `web_search`. FRIDAY utilise le mode non streaming de Conversations quand la question demande des informations Web ; le TTS reprend quand cette réponse est reçue.

## Utilisation
1. Dans Réglages, activer « Recherche Web » et cliquer **Activer et tester la recherche Web Mistral**.
2. En cas d'échec, lire l'erreur réelle : clé non valide (401), compte non autorisé (403), modèle/outil incompatible (400/422), quota (429), délai réseau.
3. Poser une question explicite telle que « Cherche sur Internet la dernière réglementation ... ». FRIDAY ne prétendra pas avoir recherché si Mistral n'a pas exécuté l'outil.
4. Pour lire une page : saisir son adresse **HTTPS publique** dans le champ prévu, démarrer l'écoute et cliquer **Analyser cette page Web avec Mistral**.
5. Le test de lecture directe HTTPS est distinct de celui de l'outil `web_search` : le premier teste la connexion Android vers un site public, pas les permissions Mistral.

## Sécurité
- Lecture ponctuelle de texte d'une page HTTPS fournie explicitement ; aucune exploration du réseau local, aucune API Google, aucune permission supplémentaire.
- Blocage DNS des adresses locales/privées, des ports non standards et des redirections non HTTPS ; réponse bornée.
- Les instructions présentes sur la page sont traitées comme contenu non fiable. Une page longue est tronquée ; les pages construites uniquement en JavaScript peuvent ne renvoyer aucun contenu exploitable.
- Le contenu de la page et la question sont envoyés à Mistral **uniquement lorsque l'utilisateur demande l'analyse**. Ne pas fournir de liens privés contenant des jetons ou identifiants.
- Les notes personnelles Memory+ et l'historique vocal restent chiffrés localement, sans synchronisation GitHub.
- Une lecture HTTPS locale ne prouve pas l'exécution d'une recherche Web Mistral. L'accès aux outils dépend de la clé API, du compte, du modèle et des quotas.

## Validation
- Tests unitaires Java : sécurité URL, extraction HTML, restrictions domaines/ports.
- Tests Python : choix de l'outil Mistral, consentement et messages d'erreur.
- Compilation Android CI et test sur téléphone réel requis avant de considérer l'intégration comme validée pour l'utilisateur.
