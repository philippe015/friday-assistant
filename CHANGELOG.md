## [3.8.9] — Stabilisation générale : microphone, Vosk, Mistral et synthèse vocale

- **Correction majeure du silence** : pendant qu'une requête Mistral/Web attend une réponse, la reconnaissance libre Vosk reste maintenant active. L'anti-écho coupe uniquement la dictée pendant une lecture TTS effective et pendant 2,5 secondes après.
- Le reconnaisseur STOP spécialisé fonctionne pendant la parole, pas en doublon de Vosk pendant les appels réseau ; une nouvelle question commençant par « Friday » peut interrompre une recherche en attente.
- **Micro Android muet** : une seule reconstruction Vosk par transition muet/prêt, reconstruction contrôlée de la capture après 8 secondes, au plus deux essais, avec repli sur `VOICE_RECOGNITION` en cas de problème `VOICE_COMMUNICATION`.
- **Requête bloquée** : garde de 150 secondes lorsqu'une requête est toujours en attente et que FRIDAY ne parle pas. Annulation explicite de ce tour, et nouvelle question autorisée.
- **Panne API** : FRIDAY annonce brièvement à voix haute l'impossibilité de répondre quand le moteur TTS fonctionne, tout en conservant un diagnostic texte.
- **Diagnostic local partageable** dans les Réglages : état du microphone, du décodeur STOP, du TTS, de la recherche Web et du mode d'écoute, sans copier de clé, de position GPS ni de conversation.
- Correction de confidentialité : les paramètres des URL consultées ne sont plus écrits dans la télémétrie Web générale.
- Préservation de la v3.8.8 (TTS direct pour les réponses Web, récupération TTS, anti-écho), de Memory+ Keystore, du GPS sous consentement et des actualités sourcées.
- Audit explicatif dans `docs/AUDIT_V389_VOICE_CONTINUITY.md`. Tests statiques Python, règles Java, CI Android et APK signé. **Essai prolongé sur appareil indispensable** : aucun test serveur ne reproduit tous les comportements de son microphone.

## [3.8.8] — Reprise du dialogue après une réponse Web

- Diagnostic du silence après plusieurs questions : le mode voix utilisait une **file de lecture progressive** même lorsque Mistral Conversations/Web avait déjà rendu une réponse complète. En cas de panne de synthèse TTS, le producteur pouvait attendre indéfiniment une file que le consommateur audio avait quittée, laissant le tour actif et la dictée générale suspendue par l'anti-écho.
- **Voix Web sans file inutile** : les questions envoyées à Mistral avec recherche Internet utilisent la réponse complète puis une lecture TTS directe, sans thread de streaming audio supplémentaire. Les flux SSE réels conservent la lecture progressive.
- **File progressive bornée** : plus aucun `queue.offer` bloquant lorsque le moteur audio n'est plus vivant. Le temps d'attente de fin audio est borné et le moteur signale un échec sans perdre le texte.
- **Reprise vocale** : si la synthèse TTS ne parvient pas à lire une réponse, FRIDAY avertit l'utilisateur et réinitialise le moteur vocal sur un thread indépendant, sans arrêter le microphone ni modifier les réglages Mistral. Une lecture audio déjà commencée n'est pas redoublée.
- L'interface précise la différence entre **Mode Continu** et **Mode une question**. Après l'expiration du délai de relance (75 secondes par défaut), dire « Friday » est nécessaire même si le micro reste actif.
- Fonctionnalités Web v3.8.7, STOP, anti-écho et Memory+ chiffrée locale conservées. Tests Java/Python et compilation Android CI. **Validation sur téléphone nécessaire** pour confirmer la stabilité des tours vocaux successifs.

## [3.8.7] — Actualités récentes : la recherche vocale utilise réellement le Web

- **Régression corrigée** : malgré un test Web Mistral réussi, la réponse vocale pouvait se contenter de « je n'ai pas accès aux pages Web » pour des informations très récentes.
- Reconnaissance des demandes vocales comme **« actualités des 7 dernières heures »**, y compris « sept » écrit en toutes lettres et des transcriptions Vosk imparfaites (« dernier heure »).
- Calcul de la **fenêtre horaire réelle** à partir de l'heure et du fuseau du téléphone. La requête Mistral demande des résultats publiés sur cette période, des sources et l'heure de publication lorsqu'elle est disponible.
- Routage préservé par **Conversations + web_search**, aussi en mode voix/streaming. Pour ces demandes, l'historique d'une réponse ancienne disant « pas d'Internet » ne biaise pas la recherche.
- Si Mistral exécute son outil mais ne fournit aucune URL pour une actualité horaire, FRIDAY déclenche **un seul deuxième essai ciblé**. Si le modèle affirme faussement ne pas avoir accès au Web, FRIDAY ne répète pas aveuglément ce texte et tente également une relance.
- À défaut de **sources exploitables**, FRIDAY explique que cette recherche n'a pas donné de résultat vérifiable sans remettre en cause le succès du test Web, et n'invente ni actualités ni heures de publication.
- Le service vocal affiche un statut spécifique pendant la recherche des nouvelles. Le Web mode Toujours v3.8.6, l'anti-écho/STOP, les autorisations GPS et Memory+ chiffrée locale sont conservés.
- Des sources trouvées ne prouvent pas automatiquement qu'elles ont été publiées dans les heures demandées : FRIDAY doit distinguer dates de publication attestées et absentes. La vérification finale sur téléphone avec une clé Mistral demeure nécessaire.

## [3.8.6] — Internet toujours activé pour les questions Mistral

- Migration unique à l'installation : **recherche Internet activée** et mode **Toujours** sélectionné automatiquement, y compris si v3.8.5 était en mode Automatique. Les choix ultérieurs de l'utilisateur sont respectés.
- Chaque question transmise à Mistral utilise désormais le chemin **Conversations + web_search** : réponse normale et réponse vocale streaming empruntent la même route Web. Les commandes entièrement locales ne génèrent pas de requête Internet inutile.
- Si un premier appel Conversations répond sans exécuter d'outil Web, FRIDAY effectue **une seule tentative supplémentaire** avec `mistral-small-latest` et une instruction de recherche explicite. Aucun rebouclage, aucun contournement 401/403/429 et aucune URL inventée.
- Les indicateurs **test de connexion Web** et **dernière recherche réelle** sont distincts : un échange sans sources ne fait plus disparaître le succès du test de la clé Mistral.
- **Limite du fournisseur :** l'API Conversations expose `web_search` mais n'offre pas de paramètre `tool_choice` forcé dans son schéma documenté. Toujours signifie donc **tenter la recherche à chaque question** ; l'exécution et les sources doivent encore être vérifiées dans la réponse.
- Les requêtes Web Mistral peuvent être facturées. Aucune recherche permanente en tâche de fond, aucune synchronisation GitHub des données personnelles. Chiffrement Memory+, STOP anti-écho et autorisations GPS conservés.
- Tests de régression Java/Python, compilation Android sur GitHub Actions et validation sur appareil avec la clé personnelle à effectuer.

## [3.8.5] — Diagnostic Web visible et mode Toujours (Mistral)

- Correction du bouton « Activer et tester la recherche Web Mistral » : son résultat n'était présenté que dans une ligne d'état **au-dessus** des Réglages, où il pouvait être masqué ou remplacé par les événements vocaux.
- Dès l'appui, FRIDAY affiche une **fenêtre de progression**, un état persistant dans le bloc Réglages et un indicateur de test en cours ; après le test, le résultat détaillé apparaît dans la même fenêtre et reste dans les Réglages.
- Les erreurs 401 (clé), 403 (outil non autorisé), 404/400/422 (modèle ou route), 429 (quota) et les délais réseau ont un message explicatif, sans montrer la clé privée.
- Ajout du mode **Toujours** inspiré de la v2.2.0, à activer volontairement : chaque question envoyée à Mistral tente une recherche via Conversations / web_search. **Automatique** reste le comportement par défaut pour limiter le coût.
- Compatibilité avec les réponses progressives : en mode Toujours, une recherche Web utilise le chemin non-streaming, puis la réponse peut être lue par TTS.
- Le changement de mode n'accorde pas d'accès Web supplémentaire si la clé, le compte ou le modèle ne prend pas en charge les outils. Le succès est confirmé uniquement si une source URL est fournie par Mistral.
- Tests Java/Python et build Android CI ; vérification réelle avec la clé personnelle sur le téléphone à effectuer.
- Fonction anti-écho/STOP v3.8.4, GPS autorisé et Memory+ chiffrée locale conservés.

## [3.8.4] — Protection anti-écho : FRIDAY ne relance plus Mistral avec sa propre voix

- **Cause corrigée** : la transcription générale Vosk traitait encore le son du haut-parleur pendant les réponses vocales, ce qui pouvait réinjecter le texte de FRIDAY comme nouvelle question.
- **Isolation de la dictée générale** tant que le tour de réponse est actif, y compris pendant le streaming TTS. Le décodeur de commandes dédié continue d'écouter « stop », « arrête » et « j'ai une précision » si activé.
- **Garde anti-réverbération** de 2,5 secondes à la fin normale de la réponse ; après interruption STOP, délai plus court de 900 ms. Le reconnaisseur Vosk général est réinitialisé lors de chaque transition pour vider les mots captés.
- Le microphone reste en marche pour l'interruption vocale, mais **les nouvelles questions vocales attendent la fin de la réponse et du délai anti-écho**. Les questions écrites restent possibles à tout moment.
- Les paramètres Mistral/Web v3.8.3, Memory+ chiffrée et permissions GPS sont conservés.
- Tests de la politique anti-écho Java, tests Python de l'ordre des décodeurs et build Android via CI. Test acoustique réel sur téléphone indispensable : la commande STOP peut toujours être perturbée par une acoustique défavorable.

## [3.8.3] — Accès Web Mistral : essai de modèle de repli et diagnostic réel

- Lorsque le modèle de dialogue sélectionné ne déclenche pas l'outil officiel `web_search`, FRIDAY effectue **un seul essai** via `mistral-small-latest`. Le modèle du dialogue reste inchangé.
- Si Conversations refuse le modèle sélectionné avec HTTP 400, 404 ou 422, FRIDAY teste une seule fois le modèle de repli. Les erreurs 401 (clé), 403 (autorisation) et 429 (quota) ne sont **pas contournées**.
- Le bouton **« Activer et tester la recherche Web Mistral »** utilise la même stratégie, affiche le modèle effectivement employé et ne déclare le test réussi que si l'API retourne au moins une véritable URL source.
- La télémétrie décrit désormais le résultat et le modèle utilisés : sources reçues, appel d'outil sans URL, ou outil non déclenché.
- Si un site public refuse la lecture directe (401/403), FRIDAY peut rechercher des **sources publiques alternatives** via Mistral et signale explicitement qu'il n'a **pas lu** la page refusée. L'URL et ses paramètres sont exclus de cette recherche secondaire. Les restrictions d'accès au site restent respectées.
- Conserve la commande STOP, Mistral comme seul moteur IA et Memory+ chiffrée locale sans synchronisation automatique.
- Tests de politique de repli hors réseau (Java), tests de régression d'intégration (Python) et compilation Android prévues par CI. **Essai réel Web avec la clé personnelle sur téléphone encore nécessaire.**

## [3.8.2] — Lecture Web : URL « https// », diagnostic HTTP 403, analyse sans microphone

- Normalise automatiquement une adresse publique mal saisie ou transcrite (`https//www.example.com`, `https:/www.example.com`, `www.example.com`) vers `https://www.example.com`, **avant de valider les règles HTTPS et les restrictions du réseau local**. Une URL sans domaine reste refusée.
- La détection des URL dans les questions accepte désormais les préfixes `https//` et `https:/`, fréquents dans la reconnaissance vocale.
- Distingue un vrai refus du site (401/403), une page introuvable (404), un quota (429) et une indisponibilité du site (5xx), sans simuler de contenu ni chercher à contourner les protections.
- Le bouton **« Analyser cette page Web avec Mistral »** fonctionne désormais **sans micro ni service d'écoute**. L'URL corrigée s'affiche dans les Réglages ; l'analyse s'effectue sur un fil réseau et la réponse s'ouvre dans une fenêtre Android.
- LCI (`www.lci.fr`) peut bloquer les requêtes automatisées (403) et indique s'opposer au moissonnage de ses contenus par IA. Ce correctif **n'essaie pas de contourner** un site qui refuse l'accès.
- Conserve Mistral comme seul fournisseur de génération, l'accès Web explicitement activé, les commandes STOP v3.7.3 et Memory+ v3.8.0 chiffrée locale, sans synchronisation GitHub.
- Tests Java sur les URL transcrites et les refus HTTP ; tests Python de l'analyse sans micro et de la confidentialité ; validation réelle sur téléphone encore nécessaire.

## [3.8.1] — Accès réel aux pages Web et diagnostic de recherche Mistral

- Recherche Internet via l'API **Mistral Conversations** et son outil officiel `web_search`. Le prompt incite à une recherche réellement exécutée, la requête désactive le stockage côté Conversations avec `store: false`, et la télémétrie distingue sources présentes, outil exécuté sans URLs et outil non déclenché.
- Une demande explicite « cherche sur Internet » ne donne plus une réponse trompeuse tirée des seules connaissances du modèle lorsque Mistral refuse ou n'utilise pas l'outil : l'échec réel est affiché (401, 403, 400/422, 429, connexion).
- **Lecture de pages HTTPS publiques** fournies dans la question : téléchargement direct sur le téléphone, extraction HTML/texte local, envoi d'un extrait limité à Mistral pour analyse avec l'URL effectivement lue comme référence.
- Protection lecture URL : HTTPS uniquement, refus des accès locaux/privés et des identifiants intégrés, validation DNS, redirections limitées, délais réseau et contenu borné à 450 Ko / 12 000 caractères.
- Réglages : bouton « Tester la lecture d'une page HTTPS publique » et champ « Analyser cette page Web avec Mistral », accessibles sans Google API.
- Les protections **FRIDAY Memory+ v3.8.0** restent inchangées : mémoire chiffrée locale, partage facultatif vers Mistral désactivé par défaut, aucune synchronisation automatique vers GitHub.
- **Limite** : l'outil Web de Mistral dépend des droits et quotas de la clé API. Une page dynamique, protégée par connexion, bloquée par robots ou autrement inaccessible ne peut pas être garantie. Aucun test avec la clé personnelle de l'utilisateur n'a été réalisé.

## [3.8.0] — FRIDAY Memory+ · mémoire locale chiffrée et consentement Mistral

- Capacité de 12 à 200 notes (600 caractères chacune), catégorisées par `[projet]`, `[travail]` ou « Général », avec recherche locale, déduplication, modification/suppression individuelle et marquage de relecture manuelle.
- Migration des 12 anciennes notes et de l'historique vocal vers un stockage AES-GCM **strict** sous Android Keystore. Les anciennes clés encodées Base64 sans chiffrement sont re-chiffrées avant d'être utilisées, sinon la lecture est refusée.
- Le Keystore indisponible n'entraîne **aucun repli en clair** ; l'écriture échoue explicitement, sans synchronisation vers GitHub.
- **Deux autorisations séparées** : activer la mémoire locale et autoriser le partage Mistral de notes pertinentes. La seconde est **désactivée par défaut**. Au maximum 5 notes et 1800 caractères sont ajoutés à une requête lorsqu'un lien lexical avec la question est détecté.
- Historique des 40 derniers messages vocaux chiffré localement, migration de l'ancien stockage partagé non chiffré, suppression locale contrôlée.
- Recherche vocale locale (« cherche dans ma mémoire projet ») sans appel au fournisseur IA. La suppression de toute la mémoire exige désormais une confirmation dans les Réglages, pas seulement un ordre vocal.
- Anciennes fonctions FRIDAY préservées : Mistral uniquement, profils actifs, STOP vocal v3.7.3, streaming, GPS avec consentement.
- Tests Java des règles Memory+, tests d'intégration statique sécurité, compilation Android CI. Validation réelle de migration Android/Keystore à faire sur appareil.
- Voir [Politique locale Memory+](docs/PRIVACY_MEMORY_PLUS_V380.md).

## [3.7.3] — Arrêt vocal répété et diagnostic acoustique

- Résout un cas ignoré par la v3.7.2 : « stop stop », « stop stop stop », « arrête arrête » et mélanges de commandes courtes étaient rejetés par la validation stricte des ordres sans mot-clé.
- Étend la grammaire Vosk du décodeur STOP aux répétitions ; une répétition dans la transcription partielle confirme immédiatement l'intention, avec filtre anti-écho conservé.
- Affiche dans la télémétrie « Interruption vocale » le niveau sonore PCM reçu par Android sur une échelle indicative 0–100, uniquement pendant une réponse. Cette valeur n'est pas une mesure de voix utilisateur : l'écho du haut-parleur peut également faire varier le niveau.
- Ajoute des tests automatisés de répétitions, de faux positifs et de mesure acoustique. La voix n'est ni enregistrée ni transmise à un serveur pour ce diagnostic.
- **Limitation** : sans signal microphone exploitable, aucun correctif textuel ou grammaire Vosk ne peut déclencher un stop verbal. Pour isoler le défaut, utiliser le bouton « Interrompre la réponse », et comparer la télémétrie microphone avec ou sans casque.

## [3.7.2] — Décodeur STOP vocal indépendant (correctif expérimental)

- Diagnostic : constat d'architecture de v3.7.1, où un seul décodeur Vosk devait simultanément transcrire la question et identifier les commandes courtes malgré l'audio du haut-parleur.
- Nouveau décodeur Vosk **réservé** aux commandes courtes « STOP », « Friday stop », « attends », « j'ai une précision ». Il n'est actif que pendant une réponse, traite le même flux micro PCM local, reconnaît les segments partiels et exige deux observations identiques pour éviter certains faux positifs.
- Le TTS Android est toujours interrompu directement ; connexion HTTP Mistral annulée ; le décodeur général est réinitialisé après une commande reconnue.
- Cockpit : nouvelle télémétrie « Interruption vocale » affichant le décodeur activé, non compatible, ou le microphone rendu muet par Android. Les commandes détectées apparaissent dans le diagnostic sans enregistrer de données audio.
- Aucun changement de fournisseur IA : Mistral seul. GPS et profils chiffrés conservés.
- Limitation réelle : si Android rend le microphone indisponible ou si la voix du haut-parleur empêche la compréhension, une commande vocale ne peut pas être garantie. Le bouton Interrompre demeure le recours sûr ; essais réels sur téléphone indispensables.

## [3.7.1] — Corrections des interruptions vocales

- **« stop » sans « Friday »** : commande courte acceptée pendant une réponse orale. « arrête », « silence », « attends » et « pause » sont également reconnues ; en dehors d'une réponse, le mot-clé reste requis pour les nouvelles demandes.
- **« j'ai une précision »** sans mot-clé reconnue pendant la réponse ; la génération et la lecture sont annulées et le sujet précédent est conservé pour le complément.
- **TTS Android** : arrêt vocal prioritaire à la déconnexion HTTP, protection contre la course lecture / arrêt, annulation HTTP indépendante du thread micro.
- **Mode « une question »** : le micro reste ouvert pendant la lecture de la réponse, puis se ferme après, au lieu de bloquer toute interruption vocale.
- **Test anti-écho** : filtre contre l'interruption quand le haut-parleur prononce lui-même le mot de commande ; tests Java et d'intégration renforcés.
- Les fonctions dépendent de la qualité de la reconnaissance Vosk et de l'accès au micro Android. Aucun test sur téléphone réel avec un microphone et le compte Mistral de l'utilisateur n'est revendiqué.

## [3.7.0] — Vosk spécialisé et écoute ponctuelle

- Nouveau **mode une question** : après une question détectée, arrêt du micro pendant la réponse, puis libération automatique du service et de son wake-lock. Démarrage manuel requis pour la question suivante. Dans ce mode, l'interruption vocale pendant la réponse n'est pas possible ; le bouton Interrompre reste opérationnel.
- Grammaire Vosk optionnelle configurée dans les réglages, 24 expressions max et `[unk]` pour les mots non listés. La grammaire est expérimentale : elle peut réduire la précision des questions ouvertes. **Désactivée par défaut**.
- Vocabulaire local enregistré par Android Keystore, sans le transmettre à Mistral comme données de configuration.
- Cockpit : états Mode d'écoute et Reconnaissance, noms des réponses alignés sur le profil actif.
- Sélection explicite des modèles Mistral issue de v3.5 préservée ; streaming de v3.6 préservé.
- Voxtral cloud, modèles Vosk géants et voix clonée **non intégrés** à ce stade (nécessitent étude de coûts, confidentialité et tests réels).
- Tests de grammaire et de comportement du service ; gains batterie/WER encore non mesurés sur téléphone.

## [3.6.0] — Streaming conversationnel et TTS progressif

- SSE Mistral Chat Completions : réponses texte reçues par fragments via `stream: true`, gestion des événements `data:`, de `[DONE]` et des fins de flux.
- Tampon de phrases complet, file TTS bornée à 12 phrases et synthèse vocale Android pendant que le modèle répond.
- Interruption : annulation réseau et thread TTS du tour précédent, sans réécouter la réponse après une interruption partielle.
- Première parole mesurée sur Android ; aucun délai universel garanti. Paramètre natif pour désactiver le streaming.
- Le connecteur Web Mistral Conversations reste en mode complet, pour préserver ses références vérifiables.
- Tests unitaires du découpage SSE/texte ; validation de la latence réelle et de la voix toujours nécessaire sur téléphone.

## [3.5.0] — Fiabilité Mistral et diagnostics

- Reprises HTTP bornées (429 et 500/502/503/504) avec Retry-After, backoff et arrêt lors des interruptions. Jamais de répétition des requêtes 4xx définitives.
- Liste des modèles Mistral mise en cache pendant une heure, sélection explicite, remise à zéro du test Web lors du changement de modèle.
- État Web plus détaillé : sources réelles, outil exécuté sans source ou service indisponible. Clés et coordonnées non journalisées.
- Identité du profil et autorisations GPS de la v3.4.1 préservées ; tests Java réseau supplémentaires.
- La recherche Web et les positions réelles exigent toujours des essais sur téléphone avec le compte Mistral ; APK CI ≠ preuve d'accès réel.

# Changelog

Format inspiré de [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/),
versionnage [SemVer](https://semver.org/lang/fr/).

## [3.4.1] — 2026-11-15

### Correctifs principaux
- **Nom et personnalité réellement actifs** : FRIDAY se présente avec l'identité définie par son profil actif et distingue clairement son nom de celui du fournisseur IA Mistral. Les prompts systèmes de Mistral Chat et Conversations insistent sur le profil sélectionné.
- **GPS transmis à Mistral après consentement** : corrige le cas « Où suis-je ? », auparavant résolu localement même lorsque le partage avec Mistral était autorisé. Les coordonnées et leur précision sont maintenant incluses dans la requête Mistral pour les demandes de position et d'alentours autorisées.
- **Respect de la vie privée** : activer simplement le GPS ne l'autorise pas à transmettre la localisation à Mistral. Il faut autoriser explicitement le partage ponctuel. Aucune position n'est envoyée pour les questions non locales, ni stockée dans l'historique.
- **Cockpit transparent** : nouveau statut « GPS vers Mistral » et bouton d'autorisation, avec indication de l'heure du dernier envoi API réussi (sans conserver les coordonnées).
- **Android** : indique lorsqu'un redémarrage de l'écoute vocale est requis après l'autorisation GPS, pour activer le type de service de localisation en premier plan.
- **Tests de régression** : identité du profil, non-équivalence GPS local / partage Mistral, reconnaissance de la question « as-tu accès à mon GPS ? » et versionnage.

### À vérifier sur le téléphone
- Enregistrer le profil voulu, demander « Friday, comment tu t'appelles ? »
- Dans le cockpit, choisir « Autoriser le GPS à Mistral » puis autoriser la permission Android ; arrêter et redémarrer l'écoute si demandé.
- Poser « Friday, où suis-je ? » puis « Quel temps fait-il autour de moi ? ». Le second test demande le Web Mistral et peut être facturé.
- Le GPS doit fournir une position récente ; l'adresse exacte n'est pas garantie. Les résultats Mistral ne peuvent pas être certifiés sans essais avec la clé personnelle.

## [3.4.0] — 2026-11-15

### Correctifs et améliorations
- **Profil actif Mistral** : instructions de la personnalité réellement sélectionnée renforcées en fin de prompt système, pour les conversations classiques et la recherche Web ; affichage du nom du profil actif en télémétrie et vérification de l'enregistrement.
- **Télémétrie Web** : suppression de la valeur codée en dur « Désactivé ». Indication réelle : désactivé, clé manquante, activé non testé, activé testé avec URL source.
- **GPS Android** : meilleure détection des questions « Où suis-je ? », « Où est-ce que je me trouve ? », « Quels services autour de moi ? ». Diagnostic séparé des permissions, des services GPS, de l'absence de position et de la précision mesurée.
- **Consentement localisation** : choix « GPS local uniquement » ou partage ponctuel autorisé avec Mistral pour des questions sur les alentours ; aucun envoi permanent de coordonnées.
- **Interruption intelligente** : la commande « Friday, j'ai une précision » et ses variantes arrêtent la réponse en cours sans perdre le sujet ; le contexte de la demande interrompue est réutilisé dans la précision suivante.
- **Cockpit** : état GPS, recherche Web, profil actif ; suppression des requêtes GPS en double lorsque le bouton GPS est utilisé.
- **Tests** : cas de régression Java pour les formulations GPS et l'interruption ; contrôles de câblage de l'interface et de Mistral.

### Limites
- Recherche Web Mistral soumise aux permissions, au quota et à la facturation de la clé API.
- Le GPS précis dépend du matériel, de l'autorisation Android et de la couverture ; 3 mètres ne sont pas garantis.
- Les tests automatisés ne remplacent pas les vérifications audio/GPS/Mistral sur un appareil Android réel.

## [3.3.0] — 2026-11-15

### Fournisseur unique : Mistral AI
- **Suppression de DeepSeek** des appels réseau, de l'écran des réglages, du cockpit et du service vocal.
- **Mistral Chat Completions** pour les conversations ordinaires (modèle par défaut `mistral-small-latest`).
- **Mistral Conversations** et son outil intégré `web_search` pour les questions nécessitant des informations en ligne. Les références Web réellement retournées sont affichées ; en l'absence de références, FRIDAY ne prétend pas avoir consulté une source.
- **Compréhension contextuelle** des questions posées oralement et des relances courtes avec historique de dialogue.
- **Migration des préférences** : suppression locale de l'ancienne clé DeepSeek ; les clés Mistral antérieurement présentes sont préservées, mais l'ancienne version peut les avoir effacées.
- **Réglages Mistral** : clé API Mistral, détection des modèles autorisés par le compte et test explicite de la recherche Web.
- **Compatibilité Android** : même application et signature release, versionCode 54 pour mise à jour de la v3.2.2.

### Limites
- La nouvelle version nécessite une **clé API Mistral personnelle** ; la clé DeepSeek ne fonctionne pas avec Mistral.
- L'outil Web dépend des droits, du quota et de la facturation du compte Mistral. Si une recherche échoue, la réponse est signalée comme non vérifiée sur Internet.
- Tests de code et de compilation automatisés ; fonctionnement vocal et Web réel à valider sur un appareil Android.

## [3.2.2] — 2026-11-15

### Amélioré
- **Recherche Web intelligente** : les questions factuelles, les demandes de recherche explicites et les relances contextuelles déclenchent l'API de recherche DeepSeek, si elle est activée.
- **Lecture des liens** : un lien HTTP(S) fourni par l'utilisateur peut être lu grâce à l'API officielle DeepSeek Fetch (lecture ponctuelle).
- **Meilleure compréhension des relances** : « Et à Lyon ? » reprend le sujet de la question précédente pour la recherche, avec contexte conversationnel conservé pour le modèle.
- **Fiabilité des réponses** : lorsque la recherche échoue ou ne retourne aucune source, FRIDAY l'indique au lieu de prétendre avoir consulté le Web.
- **Réglages natifs** : bouton « Activer et tester la recherche Web », test réel de l'API, avertissement de facturation éventuelle.
- **Performances** : les modèles disponibles ne sont plus recherchés systématiquement à chaque question, mais avec un cache de 24 h.
- **Tests** : couverture Java des requêtes factuelles, recherches explicites, liens et relances, et validation du build Android en CI.

### Limites
- Les recherches et les lectures de pages requièrent une **clé DeepSeek valide**, du réseau et un compte autorisé à utiliser les API Search/Fetch. Les appels peuvent être facturés.
- FRIDAY ne contourne pas les sites privés, comptes protégés ou politiques d'accès.
- Les résultats de recherche ne garantissent pas à eux seuls l'exactitude d'une information.

## [3.2.1] — 2026-11-15

### Corrigé
- **Réponses locales trompeuses** : la logique Python prioritaire appliquait encore
  d'anciens tests par sous-chaîne pour l'heure/la date, annulant le correctif Java de v3.2.0.
  Les demandes contenant un lieu ou un autre sujet sont maintenant transmises à DeepSeek.
- **Confiance Vosk inefficace** : activation de `setWords(true)` sur tous les décodeurs
  et prise en compte des mots de chaque segment reconnu, pas seulement du segment final.
- **Cycle de vie audio** : libération du contrôle automatique de gain (AGC) à la fermeture
  du microphone.
- **Tests de non-régression** : scénarios français couvrant météo, villes, transports,
  dates et heure.

## [3.2.0] — 2026-11-15

### Corrigé
- **Questions mal comprises** : les réponses locales (« il est X heures », « nous sommes le … »)
  interceptaient des phrases contenant simplement le mot « heure ». Elles exigent désormais que
  **tous** les mots de la phrase appartiennent au champ « heure » ou « date », et que la question
  fasse **8 mots maximum**. « Quelle est la météo à Vitry ? » part donc bien à l'IA.
- **Transcription douteuse** : la confiance moyenne des mots Vosk est vérifiée. En dessous de
  **55 %** sur une phrase de 3 mots ou plus, FRIDAY affiche ce qu'il a entendu et demande de
  répéter, au lieu d'envoyer une question erronée au modèle.

## [3.1.1] — 2026-11-15

### Corrigé
- **La détection des modèles n'appliquait pas le résultat** : la liste était affichée/copiée, mais
  le champ « Modèle DeepSeek » conservait l'ancienne valeur (souvent obsolète).
- Désormais, si le modèle configuré n'est pas proposé par le compte, l'app **applique
  automatiquement** un modèle valide détecté (priorité à ceux contenant « flash »), met à jour
  le champ et le réglage, et affiche une confirmation.

## [3.1.0] — 2026-11-15

### Ajouté
- **Accès Web** via l'API DeepSeek : `POST /v1/tools/search` (recherche native DeepSeek).
  Les meilleurs résultats (titre, URL, résumé) sont injectés dans la requête, avec consigne
  de **citer les sources** ; les pages Web sont traitées comme des données non fiables.
- Heuristique `WebSearch.needsWeb()` : la recherche ne se déclenche que si la question le
  justifie (actualité, prix, météo, dates récentes, URL…), pour éviter les appels facturés.
- Réglage **« Recherche Web (API DeepSeek) »** activé par défaut.

### Amélioré
- **Qualité du microphone** : ajout de l'**AGC** (`AutomaticGainControl`) en plus de
  l'annulation d'écho et du réducteur de bruit, et **tampon audio ×4** (16384 octets minimum)
  pour réduire les coupures et mieux capter les voix faibles.

## [3.0.5] — 2026-11-15

### Corrigé
- **La clé DeepSeek saisie n'était pas prise en compte** tant que l'on n'appuyait pas sur
  « Enregistrer les réglages » (situé tout en bas du panneau, hors écran).
- Nouveau bouton **« 💾 Enregistrer la clé DeepSeek »** juste sous le champ, avec retour immédiat
  (`✔ enregistrée` / `✘ échec`).
- Le bouton **« 🔍 Détecter les modèles »** enregistre d'abord la clé présente dans le champ,
  puis interroge `/models` — plus besoin d'aller plus bas dans les réglages.

## [3.0.4] — 2026-11-15

### Ajouté
- **Détection automatique des modèles** : l'app interroge `GET /models` avec votre clé et
  utilise un modèle réellement proposé par votre compte (plus de nom obsolète).
- Bouton **« 🔍 Détecter les modèles disponibles »** dans les réglages : affiche la liste
  et la copie dans le presse-papiers.
- La liste détectée est mise en cache (`available_models`) et réutilisée comme ordre de repli.

## [3.0.3] — 2026-11-15

### Corrigé
- **Le champ « Modèle DeepSeek » conservait l'ancien nom** (`deepseek-chat`) enregistré dans les
  préférences, alors que la valeur par défaut était corrigée : l'écran affichait donc toujours
  l'ancien modèle.
- **Migration au démarrage** : `deepseek-chat`, `deepseek-reasoner` et `deepseek-v3*` sont
  remplacés par **`deepseek-flash`**.
- Le nom retiré n'est plus envoyé à l'API avant le repli.

## [3.0.2] — 2026-11-15

### Corrigé
- **Modèle DeepSeek invalide** : `deepseek-chat` n'existe plus dans l'API 2026
  (modèles actuels : `deepseek-flash`, `deepseek-v4-pro`). Le modèle par défaut devient
  **`deepseek-flash`**.
- **Repli automatique** : si le modèle configuré est refusé (400/404/422), la question est
  rejouée avec `deepseek-flash` puis `deepseek-v4-pro`, et le réglage est corrigé.
- **URL de base** : `https://api.deepseek.com` (chemin documenté) au lieu de `…/v1`.

## [3.0.1] — 2026-11-15

### Ajouté
- **Diagnostic d'erreur** dans les réglages natifs : la dernière erreur du moteur IA est
  mémorisée, affichée avec son horodatage, et copiable dans le presse-papiers
  (bouton « ⧉ Copier la dernière erreur ») — avec modèle, URL et présence de la clé.
- Nettoyage : fonction `vision()` résiduelle retirée du cockpit.

## [3.0.0] — 2026-11-15

### Supprimé
- **Tout le code Mistral** : `MistralVoice` (clonage vocal Voxtral), `WebIntent` / `WebPolicy`
  (recherche Web via l'API Conversations), `NearbyIntent` (analyse des alentours),
  `VoiceClonePolicy` / `VoiceCloneActivation` / `VoiceSampleRecorder`, analyse d'images (Vision).
- Réglages associés : clé Mistral, modèle Mistral, modèle vision, mode Web, raisonnement approfondi,
  voix clonée. Le bouton « Choisir Mistral Medium » disparaît.
- Tests obsolètes liés à Mistral.

### Modifié
- **DeepSeek est le seul moteur** (endpoint `https://api.deepseek.com/v1`, modèle `deepseek-chat`),
  avec le bouton « Se connecter / créer ma clé sur platform.deepseek.com ».
- Le service annonce « Réflexion DeepSeek… » ; le cockpit et les libellés ne mentionnent plus Mistral.
- `debug` conserve le suffixe `.smartpreview` (cohabitation avec les anciennes versions).

### Notes
- L'application est plus légère et n'envoie plus rien à Mistral.
- La reconnaissance vocale (Vosk), la synthèse Android (TTS), la mémoire chiffrée, les profils de
  personnalité et la localisation restent disponibles.

## [2.4.0] — 2026-11-15

### Modifié
- **DeepSeek devient le fournisseur unique** (Mistral retiré de l'interface).
- Nouveau bouton **« Se connecter / créer ma clé sur platform.deepseek.com »** : ouvre la page
  web officielle dans le navigateur, où l'utilisateur crée/copie sa clé API.
- Le cockpit affiche désormais **MOTEUR DEEPSEEK**.
- La clé reste chiffrée localement (Android Keystore).

### Notes
- La **vision** et la **recherche Web** ne sont plus exposées : DeepSeek Chat ne propose pas
  de connecteurs équivalents (les fonctions Mistral restent dans le code mais inactives).

## [2.3.0] — 2026-11-15

### Ajouté
- **Fournisseur IA configurable** dans les réglages natifs :
  - `Mistral AI` (comportement historique)
  - **`Compatible OpenAI`** : URL de base + modèle + clé — fonctionne avec
    **DeepSeek**, **Ollama**, **LM Studio**, **vLLM**, etc.
- La clé du fournisseur OpenAI est chiffrée comme celle de Mistral (`SecurePrefs`).

### Notes
- La **vision** et la **recherche Web** restent liées à Mistral (connecteurs non pris en charge
  par les autres fournisseurs). Les questions texte utilisent le fournisseur choisi.

## [2.2.4] — 2026-11-15

### Corrigé
- **Chaque question affichait « ERREUR »** : le mode Web utilisait l'API *Conversations* avec un modèle
  `ministral-*`, qui ne prend pas en charge les connecteurs :
  `HTTP 400 — Model ministral-8b-latest currently does not support builtin connectors`.
- Les modèles `ministral-*` **n'utilisent plus** l'API Conversations : réponse directe via
  `/v1/chat/completions`, avec une note si la question nécessitait Internet.
- Le repli du mode Web couvre désormais **429 / 500 / 501** en plus de 400 / 404 / 422.

## [2.2.3] — 2026-11-15

### Corrigé
- **« MOTEUR MISTRAL ERREUR »** alors que la voix et les réponses locales fonctionnent :
  un modèle enregistré (`mistral-small-*` / `mistral-medium-*`) renvoie `429` sur les offres
  actuelles. Les modèles stockés sont désormais **migrés automatiquement** au démarrage.
- **Repli automatique** : si le modèle configuré est refusé (400/404/422/429), la question est
  rejouée une fois avec `ministral-8b-latest`, le réglage est mis à jour et une note est ajoutée
  à la réponse.
- Les erreurs Mistral précisent désormais le **modèle utilisé**.

## [2.2.2] — 2026-11-15

### Corrigé
- **Clé Mistral « absente » malgré la saisie** : si le Keystore de l'appareil échoue,
  la clé est désormais conservée en **repli** (et l'incident est affiché), au lieu d'être perdue.
- L'écran de réglages affiche l'état réel de la clé : `✔ enregistrée` / `✘ absente`
  avec le détail d'une éventuelle erreur Keystore.
- Message d'erreur explicite : « Réglages natifs → collez la clé → Enregistrer ».

## [2.2.1] — 2026-11-15

### Corrigé
- **Modèles Mistral par défaut** : `mistral-small-latest` renvoie `429 Rate limit exceeded`
  sur les offres actuelles. Le chat utilise désormais `ministral-8b-latest`
  (fonctionne aussi avec `ministral-14b-2512`).
- **Modèle vision** : `mistral-small-2506` n'existe plus → `ministral-14b-2512` (multimodal).
- Message d'erreur 429 explicite (limite de débit **ou** modèle hors offre).

## [2.2.0] — 2026-11-14

### Ajouté
- **Écoute interactive** : micro maintenu pendant le calcul Mistral et la lecture TTS (threads séparés)
- **Interruption vocale** : « Friday stop », « Friday attends », « Friday, j'ai une précision »,
  « Friday, explique autrement », « Friday, arrête l'écoute »
- Bouton **« Interrompre la réponse »** dans le cockpit et la notification Android
- **Clonage vocal** Mistral (Voxtral), activation enregistrée après capture
- Anti-écho : `VOICE_COMMUNICATION` + `AcousticEchoCanceler` / `NoiseSuppressor`
- Bibliothèque **Rust** `libfriday_core.so` (détection de mot-clé, JNI)

### Modifié
- Détection de fin de phrase accélérée (~2,1 s, réglable) et fenêtre de dialogue de 75 s
- Annulation des requêtes HTTP Mistral et des segments audio en cours

### Corrigé
- Une réponse annulée ne peut plus être prononcée après coup

## [2.0.2] — 2026-11-14

### Ajouté
- Publication du dépôt (sources Java/Python/Rust, scripts de build, tests)
- APK debug de référence

### Corrigé
- Analyse statique de l'APK documentée (`ANALYSE.md`)

## [2.0.0] — 2026-11-06

### Ajouté
- Cockpit HUD (WebView), mémoire personnelle chiffrée, profils de personnalité
- Mistral Chat Completions, recherche web, analyse de photos (Vision)
- Vosk hors-ligne, TTS Android, GPS et résolution de lieux
