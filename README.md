# FRIDAY JARVIS 3.14.31 — Compréhension vocale améliorée (Android)

> **Version à tester mise en avant — 26 novembre 2026.** Cette préversion améliore l'interprétation des demandes vocales en français, en particulier les noms de communes lors des questions météo. Le code est proposé dans la [PR #133](https://github.com/philippe015/friday-assistant/pull/133) ; il n'est **pas fusionné dans main** et n'est **pas publié sur Google Play**.

**[Télécharger directement FRIDAY JARVIS 3.14.31 — APK Android (GitHub Release)](https://github.com/philippe015/friday-assistant/releases/download/friday-3.14.31-weather-voice-preview/FRIDAY-JARVIS-3.14.31-Voice-Weather-Preview.apk)** · [Fiche GitHub de la préversion et SHA-256](https://github.com/philippe015/friday-assistant/releases/tag/friday-3.14.31-weather-voice-preview) · [Code source PR #133](https://github.com/philippe015/friday-assistant/pull/133) · [Manuel de la version](https://github.com/philippe015/friday-assistant/blob/feature/friday-weather-voice-city-v31431-20261125/docs/FRIDAY_WEATHER_VOICE_CITY_RECOVERY_20261125.md)

**Comprend mieux certaines formulations vocales :**
- **Météo à Vitry-sur-Seine :** interprète prudemment « Vitry dur Seine » ou « Vitry sur scène » lorsque *Vitry* est bien entendu par Vosk.
- **Si la commune n'est pas reconnue :** demande de la répéter, au lieu de chercher « scène » sur Wikipédia ou de deviner une ville.
- **Dialogue plus naturel :** garde le contexte « météo pour demain » lorsqu'on précise ensuite le nom de la ville.
- **Mode local préservé :** IA LiteRT-LM sur le téléphone, historique chiffré, TTS Android, recherche Internet facultative.

**Installation Android :** utilisez **Télécharger directement l'APK** ci-dessus, puis ouvrez le fichier dans les téléchargements du téléphone. Si le téléchargement direct de la Release est indisponible, [récupérez l'artefact de compilation sur GitHub Actions](https://github.com/philippe015/friday-assistant/actions/runs/45679089884) (connexion GitHub parfois nécessaire, ZIP à extraire). **Android 8+ ARM64, APK de test signé debug (~43,6 Mo)** ; les modèles IA et Vosk ne sont pas inclus. Conservez vos données avant toute désinstallation : une signature incompatible peut empêcher la mise à jour directe.

**État de validation :** tests automatiques et compilation Android réussis ; l'amélioration de la reconnaissance acoustique sur tous les téléphones n'est pas garantie, car le moteur Vosk lui-même n'est pas remplacé. [Signaler une anomalie](https://github.com/philippe015/friday-assistant/issues) sans publier vos conversations personnelles.

---

## Versions précédentes et historique

## JARVIS v3.14.16 — contributions communautaires GitHub sans données privées

Il est désormais possible pour **chaque utilisateur Android** de signaler
un dysfonctionnement via **Cockpit → Signaler sur GitHub** ou le bouton
équivalent dans les réglages natifs.

**Fonctionnement :** JARVIS prépare un brouillon de GitHub Issue avec
le numéro de version de l'application, la version du SDK Android et
les **28 derniers codes d'événements techniques**, présentés avec des
délais relatifs. L'utilisateur voit l'intégralité de ce brouillon
avant de confirmer l'ouverture de son navigateur ; les données du
brouillon sont transmises au site GitHub **dès l'ouverture du lien**,
même si le signalement n'est pas publié. C'est une opération volontaire
distincte de la sauvegarde locale chiffrée.

L'utilisateur doit avoir un compte GitHub, se connecter au besoin,
corriger/compléter le rapport et **publier lui-même**. Aucun ticket
n'est déposé automatiquement et aucune autorisation GitHub/API token
n'est demandée à l'application. Sans clic utilisateur, les journaux
restent sur le téléphone.

**Protections de confidentialité :** le brouillon public est construit
exclusivement à partir des codes fixes `DiagnosticLogPolicy.allowedCode`
et de durées en millisecondes / temps relatifs. Il n'envoie aucun texte
de dialogue, transcription vocale, nom, secret, URL, coordonnée GPS,
horodatage absolu ni fichier JSON complet.

**Ne joignez jamais directement un export `JARVIS-rapport-conversation*.json`
dans une GitHub Issue publique.** Ces exports sont explicitement
personnels et peuvent contenir des données sensibles. Une pièce jointe
GitHub peut devenir accessible dès sa mise en ligne ; sa suppression
ne garantit pas l'effacement de toutes les copies déjà effectuées.

Le rapport JSON local de v3.14.14 reste indépendant et conserve
sa case **« Inclure la conversation »** (désactivée par défaut).
Les événements techniques et les échanges restent stockés sous
chiffrement Android Keystore, sans synchronisation automatique.

Les contributions permettent de repérer les codes d'anomalie
récurrents et de prioriser les corrections, mais aucune correction
automatique du code, aucune télémétrie permanente et aucune mise à
jour forcée ne sont mises en place.

---

## JARVIS v3.14.14 — Export conversation + journal technique (bêta)

**Objectif :** obtenir un véritable rapport permettant de comprendre
*quelle question a déclenché quelle réponse et quelle anomalie*, plutôt
qu'une liste de codes sans contexte.

### Export directement sur Android

Dans **Cockpit > Journal d'anomalies > Exporter rapport / conversation**
(ou Réglages > Sauvegarder les logs), JARVIS ouvre une confirmation avec
une case **« Inclure la conversation : questions et réponses »**.

- **Case non cochée par défaut :** rapport JSON avec seulement les
  codes techniques et les durées, comme en v3.14.13.
- **Case cochée explicitement :** le rapport ajoute `conversation[]`
  avec les questions et les réponses déjà conservées dans l'historique
  chiffré Android, les canaux `voice` / `text` et une chronologie
  `timeline[]` de références aux échanges datés et aux codes
  techniques.
- L'historique conserve **les 80 derniers messages** (et non tous les
  échanges depuis l'installation). Chaque nouveau message à compter
  de la v3.14.14 porte `time_ms` et `time_utc` dans le rapport.
  Les anciennes conversations restent accessibles mais sont marquées
  `time_unknown` au lieu d'inventer une date.
- Le message écrit est horodaté à la **soumission de la question**,
  et non à la fin de la recherche Web, même si l'écriture dans
  l'historique chiffré se produit après la réponse.
- Le journal technique `events[]` reste chiffré au repos et **ne
  reçoit jamais** de contenu de conversation. L'export ne lit les
  messages que lorsque la case est cochée.
- **Attention :** le fichier JSON choisi via Android
  `ACTION_CREATE_DOCUMENT` est non chiffré et contient le texte
  réel, potentiellement privé, des échanges ; relisez-le avant de
  l'envoyer. JARVIS n'envoie rien automatiquement sur le réseau.
- Si Android Keystore ne peut pas déchiffrer l'historique, le rapport
  incluant la conversation échoue explicitement. Pas de repli en clair.

### Structure du JSON

`schema: 2`, `events[]` (codes techniques), `conversation_included`
(booléen), `conversation[]` (seulement avec consentement : `seq`,
`role`, `channel`, `text`, `time_ms`, `time_utc` quand connu),
`timeline[]` (entrée technique ou référence à une question/réponse
horodatée ; la conversation n'est pas répétée en texte dans la
chronologie).

Les fonctionnalités vocales, LiteRT, RSS et les sauvegardes existantes
restent inchangées. Tests Java/Python/JS/Rust et compilation Android
sur GitHub Actions ; un essai sur téléphone reste nécessaire.

---

## JARVIS v3.14.11 — lecture RSS et titre différent chez l'éditeur (bêta)

Rapport Android du **22 novembre 2026 à 16 h 34** : Vosk reconnaît
« lis l'article numéro deux en entier », puis Google Actualités fournit
un lien indirect vers une brève du Figaro qu'aucun index consulté n'a
permis de confirmer chez l'éditeur.

- JARVIS valide désormais une page publique du **même domaine éditeur**
  en vérifiant le titre ou, si le titre du site est différent, un
  **passage de texte qui correspond fortement aux mots spécifiques**
  du titre RSS.
- L'inspection d'un candidat public reste bornée : trois pages HTTPS
  au plus et une section de 1 900 caractères au maximum. Les candidats
  mentionnant seulement le pays, les drones ou un autre navire sont
  rejetés par les tests de régression.
- Lorsqu'une correspondance est trouvée uniquement dans le texte,
  JARVIS précise que la lecture est **partielle**, jamais le texte
  intégral certifié du Figaro.
- Si aucune page correspondante n'est trouvable, le message indique
  clairement « pas encore indexée, introuvable ou inaccessible »,
  sans inventer de texte ni confondre un article ordinaire avec un DIRECT.
- Permissions Android, GPS, profil, IA locale et données chiffrées
  demeurent inchangés.

Cette recherche ne résout pas systématiquement les URL Google News
encodées. Certains articles ne sont pas indexés ou nécessitent un accès
abonné ou JavaScript, que JARVIS ne contournera pas. Un essai réel
sur Android reste nécessaire.

---

## JARVIS v3.14.9 — limite d'entrée LiteRT-LM et sélection RSS (bêta)

Le journal du **22 novembre 2026, 15 h 05** identifie la cause exacte :
`INVALID_ARGUMENT: Input token ids are too long` : **2 151 tokens**
envoyés pour une limite de **2 048**. Le repli CPU ne pouvait pas
résoudre ce dépassement de contexte.

- Un **budget UTF-8 local** réduit le profil, l'historique et les extraits
  avant leur envoi natif, en conservant l'identité active et la demande en cours.
  Il ne dépend pas d'une estimation erronée du nombre de caractères :
  le tokenizer LiteRT reste l'autorité finale.
- Si l'entrée dépasse encore 2 048 tokens, une seule tentative de secours
  emploie un budget plus strict sur le **même backend**. Un dépassement de
  contexte n'est plus présenté comme une panne GPU ou CPU.
- « **L'article numéro un** » et « **dix l'article numéro un** » (transcription
  Vosk de « lis l'article… ») sélectionnent le dernier lien RSS mémorisé,
  sans envoyer cette commande courte à l'IA. Aucun article n'est inventé.
- Un extrait Web ou un passage privé réduit pour respecter la capacité du
  modèle est marqué comme **partiel** plutôt que présenté comme intégral.

Les conversations et le profil restent sur le téléphone. Le modèle
`.litertlm` doit toujours être installé séparément et compatible avec
le GPU/CPU du smartphone. Les essais réels sur Android restent nécessaires.

---

## JARVIS v3.14.8 — LiteRT-LM, Vosk et état GPS (bêta)

Cette préversion cible le diagnostic du **22 novembre 2026, 12 h 38**.

- **LiteRT-LM** : en cas d'erreur JNI pendant l'inférence, le moteur natif
  défaillant est fermé. Si la panne survient sur GPU, un unique essai sur CPU
  est lancé avec un moteur neuf. Le détail de l'erreur est communiqué sans
  exposer les fichiers privés. Un fichier .litertlm GPU-only peut rester
  incompatible avec le CPU.
- **Vosk** : une transcription d'un seul mot comme « cycle » n'est pas une
  question directe. Les commandes RSS « lis l'article numéro un » à « sept »
  sont reconnues ; une phrase incertaine comme « c'est l'article et en adsl »
  déclenche une demande de reformulation locale sans solliciter Gemma.
- **GPS** : le cockpit différencie une position non encore mesurée d'une
  autorisation manquante, d'un fournisseur Android inactif et d'une
  acquisition sans résultat. Une permission « précise » ne garantit jamais
  une position à trois mètres.

L'assistant et les informations personnelles restent **sur le téléphone** ;
aucun changement de microphone, de permissions ou de fournisseur IA externe.

**Attention :** la correction améliore la récupération et le diagnostic,
sans garantir qu'un modèle LiteRT-LM incompatible fonctionnera sur ce GPU.
La validation sur un téléphone Android demeure nécessaire.

[Correctif GitHub](https://github.com/philippe015/friday-assistant/pulls)

---

## JARVIS v3.14.7 — Éditeur RSS et anti-écho (bêta)

La lecture d'articles de Google Actualités utilise désormais la métadonnée
`<source url="…">` pour reconnaître un média absent de la liste des domaines
préprogrammés. L'URL de l'article n'est acceptée qu'après validation du
domaine de l'éditeur **et** concordance du titre. Quand le texte complet
n'est pas accessible, JARVIS le signale sans inventer l'article.

Les transcriptions provisoires Vosk ressemblant à la propre voix de
JARVIS sont masquées dans la télémétrie ; le filtre final anti-écho et
le bouton STOP restent en place.

**Après installation, redemander un bulletin d'actualité RSS avant de dire
« Jarvis, lis l'article 1 en entier » : les anciens articles enregistrés
ne possèdent pas la nouvelle métadonnée d'éditeur.**
La lecture reste limitée aux pages publiques accessibles, sans contournement
des paywalls, comptes ni interfaces JavaScript.

[Pull request et vérifications v3.14.7](https://github.com/philippe015/friday-assistant/pull/96)

---

## J.A.R.V.I.S. v3.14.6 — profil permanent et lecture Web approfondie (bêta)

Cette branche de développement ajoute l'analyse HTTPS publique jusqu'à 36 000
caractères lorsqu'une page est accessible. Le texte est traité intégralement
par segments puis synthétisé **avec le profil actif stocké sur Android**.
« Jarvis, analyse cette page https://… » et « Jarvis, lis cette page en entier »
utilisent la lecture approfondie ; une analyse explicite de plusieurs pages Web
peut couvrir jusqu'à deux pages accessibles. Le moteur IA reste local.

**Statut :** version source en validation CI ; ne pas remplacer aveuglément une
installation déjà fonctionnelle. Tests sur téléphone nécessaires.

[Guide d'utilisation et limites](docs/JARVIS_V3146_PROFIL_WEB_PROFOND.md)

---

## FRIDAY/JARVIS v3.14.1 (PRÉVERSION) — RSS mondiaux traduits localement en français

**Nouveau :** 17 flux et éditions RSS internationales, un choix ciblé selon la région demandée, et traduction en français des titres/extraits avec le **modèle Gemma 4 E2B GPU local** (aucun service de traduction cloud). Sources originales, dates RSS et liens conservés. Réglage désactivable dans **Internet Jarvis** et bouton **RSS Monde FR** dans le cockpit.

**Version fonctionnelle validée à conserver :** [FRIDAY/Jarvis Intelligence+ v3.14.0-test](https://github.com/philippe015/friday-assistant/releases/tag/v3.14.0-test). La préversion RSS 3.14.1 utilise une édition Android séparée : **ne pas désinstaller la version 3.14.0**, ses fichiers de modèle et son historique restent privés à cette version.

[Documentation RSS mondiaux : langues, sources, confidentialité et installation](docs/JARVIS_RSS_MONDE_FR_V3141.md)

---

## FRIDAY v3.11.0 — Gemma 4 .litertlm pris en charge sur Android

FRIDAY prend en charge deux moteurs de langage fonctionnant sur le téléphone :
- **MediaPipe** pour `gemma3-270m-it-q8.task` ([fichier compatible](https://huggingface.co/litert-community/gemma-3-270m-it/blob/main/gemma3-270m-it-q8.task)).
- **LiteRT-LM** pour `gemma-4-E2B-it-gpu.litertlm` ([fichier compatible](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/blob/main/gemma-4-E2B-it-gpu.litertlm)).

Télécharger le modèle depuis Android, puis ouvrir **Réglages → Jarvis local → Importer un modèle IA local**. FRIDAY choisit le moteur d'après le format et garde le fichier dans son stockage privé. Gemma 4 GPU pèse environ 2,34 Go et peut échouer sans assez de RAM ou de pilotes GPU/OpenCL. Le modèle MediaPipe de 304 Mo est plus adapté pour un premier essai.

Un test de génération sur le téléphone reste indispensable. La reconnaissance vocale, Memory+, Mistral Web et STOP sont conservés.

[Guide détaillé](docs/JARVIS_ANDROID_OFFLINE.md)

## FRIDAY v3.10.0 — Jarvis entièrement sur le téléphone, sans ordinateur

Cette version intègre un **véritable moteur de langage local Android** avec MediaPipe GenAI. Les fonctions vocales et les notes personnelles restent sur le téléphone : une fois un modèle `.task` compatible importé depuis les téléchargements Android, Jarvis local peut générer des réponses sans Wi-Fi, sans connexion au PC et sans API Mistral. L'APK ne contient pas lui-même les poids du modèle, qui peuvent faire plusieurs centaines de Mo.

**Installation depuis le téléphone** : ouvrir la page des [modèles LiteRT Community Gemma 3 270M](https://huggingface.co/litert-community/gemma-3-270m-it/tree/main) ; se connecter et accepter les conditions du modèle si nécessaire ; choisir le fichier `gemma3-270m-it-q8.task` (pas `*-web.task` ni `.litertlm`). Dans FRIDAY, aller dans **Réglages → Jarvis local sur Android → Importer un modèle IA local**, puis activer le moteur et lancer **Tester Jarvis IA locale sans Internet**. Dire ensuite « Friday, Jarvis, explique-moi ... ». La deuxième case permet de passer toutes les questions générales à Jarvis local, sans Mistral.

La mémoire Memory+ chiffrée est accessible en contexte local sur le téléphone, sans synchronisation. Un outil de calcul local est fourni ; il n'y a pas d'exécution arbitraire de programmes. Mistral reste disponible dans son mode habituel pour les actualités et la recherche Web lorsqu'Internet est actif. La connexion OpenJarvis-PC v3.9.0 est toujours facultative mais **n'est pas nécessaire**.

**Limite vérifiable** : cette version intègre le moteur MediaPipe Android et non le programme OpenJarvis Linux complet. Les capacités réelles du modèle dépendent de son fichier, de la RAM, du chipset et de la version d'Android. La compilation et les tests CI ne remplacent pas un essai sur appareil.

[Guide complet : Jarvis IA hors ligne sur Android](docs/JARVIS_ANDROID_OFFLINE.md)

## Nouveauté FRIDAY v3.9.0 — Connecter OpenJarvis sur Manjaro

FRIDAY peut désormais **communiquer avec OpenJarvis**, un agent externe installé sur un PC Manjaro ou Linux, sans installer son environnement Python/Ollama dans l'APK Android. La liaison utilise son API `POST /v1/chat/completions` derrière un serveur **HTTPS authentifié**. OpenJarvis est **facultatif et désactivé par défaut**, Mistral reste le moteur ordinaire avec accès Web et actualités vocales.

Ouvrez Réglages → OPENJARVIS, activez la connexion, entrez l'URL HTTPS, le modèle réellement présent sur le PC et le jeton de serveur `OPENJARVIS_API_KEY`. Le jeton est conservé chiffré sur le téléphone. Appuyez sur « Tester mon serveur OpenJarvis », puis dites : **« Friday, Jarvis, explique-moi ... »**. Cochez l'option de routage général uniquement si vous consentez à envoyer les questions générales à ce PC.

**Aucune mémoire chiffrée, aucun historique privé ni position GPS n'est joint automatiquement à ces appels.** En cas d'échec, l'application indique le problème au lieu d'envoyer silencieusement la question à Mistral. Les performances du modèle et les éventuels outils d'OpenJarvis dépendent de la configuration et des permissions du serveur.

Voir le [guide complet](docs/OPENJARVIS_ANDROID_BRIDGE.md), l'[API officielle OpenJarvis](https://open-jarvis.github.io/OpenJarvis/deployment/api-server/) et la [licence Apache 2.0](https://github.com/open-jarvis/OpenJarvis/blob/main/LICENSE). **Le projet OpenJarvis complet n'est pas embarqué** dans l'APK : cette version ajoute une passerelle vérifiable.

## FRIDAY v3.8.9 — Dialogue continu et diagnostic complet

Un défaut de la v3.8.8 bloquait la reconnaissance vocale **pendant les recherches Web**, même quand le haut-parleur était silencieux. La v3.8.9 garde désormais le microphone et Vosk actifs en attendant Mistral. Dites **« Friday, ... »** pour envoyer une nouvelle question, même si une recherche précédente n'a pas fini. Pendant la synthèse vocale, seul STOP / précision reste actif, puis la dictée se rouvre après l'anti-écho.

En cas de microphone rendu muet par Android, FRIDAY évite les réinitialisations répétées de Vosk et tente un autre mode de capture. Une requête Mistral silencieuse trop longue est annulée après 150 secondes. Une erreur réseau peut être annoncée à voix haute si TTS fonctionne.

**Diagnostic :** ouvrir Réglages → **Diagnostic complet de l'écoute · micro / Vosk / Mistral / voix**. La fenêtre affiche l'état du service, du micro Android, de Vosk/STOP, du moteur vocal, du mode Continu/Une question et du test Web. Le bouton de copie exclut la clé Mistral, les coordonnées GPS et les conversations. La dernière erreur détaillée reste consultable séparément.

**Pour vérifier :** en mode Continu (case « Mode une question » décochée), poser « Friday, actualités des sept dernières heures », puis « Friday, quelle heure est-il ? » pendant que le réseau est encore en attente. Après une réponse parlée, patienter 2,5 secondes et poser trois autres questions commençant par Friday. Si cela échoue, transmettre le diagnostic copié et préciser si la question apparaît en texte ou si seul le son manque.

Voir `docs/AUDIT_V389_VOICE_CONTINUITY.md` pour le détail du diagnostic, des corrections et des limites. La version conserve Memory+ chiffrée sur le téléphone, les permissions GPS, le Web de Mistral et STOP. **La stabilité réelle doit être vérifiée sur appareil.**

## FRIDAY v3.8.8 — Réponse vocale et questions suivantes

Après une réponse longue, FRIDAY doit continuer à écouter. Le circuit Web Mistral utilise maintenant directement la synthèse Android lorsqu'une réponse complète est fournie : la file streaming ne doit plus conserver une requête active après un échec audio. La lecture réellement progressive conserve une file bornée, contrôlée pour ne pas attendre un lecteur TTS mort. En cas d'erreur TTS, le service laisse le texte disponible, annonce le problème et tente de réinitialiser la voix sans couper le microphone.

**Mode Continu** : laisser la case « Mode une question » décochée. Le microphone reste actif ; après 2,5 secondes anti-écho, dites « Friday, ... » pour chaque nouvelle question, surtout au-delà des 75 secondes de suivi sans mot-clé (durée réglable). **Mode une question** : si cette case est cochée, le micro s'arrête volontairement après la réponse.

**Diagnostic sur téléphone** : demander « Friday, donne les actualités des sept dernières heures », attendre la fin, puis dire « Friday, quelle heure est-il ? » et « Friday, parle-moi de la météo de demain avec les sources ». Si FRIDAY n'émet plus de son mais affiche du texte, tester « Répondre à voix haute » et le volume média, puis « Tester la voix ». Si le micro est bloqué, toucher « Interrompre la réponse et écouter » ou redémarrer l'écoute native. Vérifier les erreurs TTS affichées par l'application.

Le Web « Toujours » de v3.8.6, la source des actualités v3.8.7, les autorisations GPS, STOP et Memory+ chiffrée locale sont conservés ; aucune synchronisation automatique des données personnelles vers GitHub.

## FRIDAY v3.8.7 — « Les actualités des 7 dernières heures »

Sur le téléphone Android, FRIDAY traite une question vocale sur des nouvelles publiées pendant les **N dernières heures (1 à 48)** comme une demande de recherche récente. Le système prépare une période exacte avec l'heure du téléphone puis envoie la requête par **Mistral Conversations + web_search**. La réponse doit préciser les sources et, si disponible, les heures de publication.

Si Mistral ne retourne aucune source exploitable, FRIDAY effectue **au plus une relance**. Il ne doit pas reprendre la phrase générique du modèle « je n'ai pas accès au Web » sans expliquer que le test Web a pu réussir séparément. Aucun résultat non sourcé n'est présenté comme une actualité vérifiée.

Exemple vocal : **« Friday, donne-moi les actualités des sept dernières heures et cite les sources avec leurs dates de publication. »**

Le moteur `web_search` reste piloté par Mistral. Un test de recherche réussi confirme l'outil sur cette requête de test uniquement ; il ne garantit ni couverture exhaustive ni précision à l'heure près des actualités. Les requêtes peuvent être facturées. Pas d'accès aux contenus bloqués ni de synchronisation automatique des données privées. Le contrôle de sécurité STOP/anti-écho et la mémoire chiffrée restent en place.

## FRIDAY v3.8.6 — Accès Internet activé par défaut

FRIDAY propose désormais automatiquement **Recherche Web activée + Mode Toujours** après installation ou migration depuis les anciennes versions. Chaque **question non locale envoyée à Mistral** demande l'outil officiel `web_search` via l'API Conversations. S'il ne s'exécute pas au premier essai, l'application effectue au plus **une deuxième tentative** ciblée avec `mistral-small-latest`. Le résultat indique clairement si des sources ont réellement été obtenues.

La clé API Mistral déjà enregistrée est conservée. Les droits du compte, les quotas, la connexion Wi-Fi/données et les refus de certains sites restent applicables. L'option Toujours peut consommer davantage de crédits Mistral. FRIDAY n'effectue **pas de navigation permanente en arrière-plan** : Internet est utilisé lorsqu'une demande est envoyée. Une réponse sur l'heure ou une commande locale peut rester hors ligne.

Le diagnostic du bouton Test Web est conservé séparément de l'état de la dernière recherche, pour éviter de confondre « clé capable de rechercher » avec « recherche effectivement exécutée ». Dans Réglages, il reste possible de revenir au mode Automatique en décochant Toujours et d'enregistrer.

Memory+ reste chiffrée uniquement sur le téléphone, sans synchronisation GitHub. Les autorisations de partage GPS sont inchangées et la commande STOP/anti-écho de la v3.8.4 est conservée.

## FRIDAY v3.8.5 — Tester réellement l'accès Internet

Ouvrir **Réglages → Moteur IA Mistral** puis appuyer sur **« Activer et tester la recherche Web Mistral »**. La fenêtre indique immédiatement « Test en cours », puis affiche le résultat explicite. Une copie du dernier diagnostic reste visible **juste sous le bouton** et dans la télémétrie Web du cockpit.

Le mode **Automatique** choisit le Web seulement quand la demande le nécessite. En activant **« Mode Toujours »**, chaque question transmise à Mistral demande `web_search` via son API `/v1/conversations`, au prix de requêtes éventuellement facturées ; les réponses prennent alors plus de temps et le streaming token par token n'est pas disponible pour cette recherche. Le modèle décide toujours si l'outil est exécuté. Les réponses locales, l'heure et certaines commandes ne font pas systématiquement appel à l'API.

**Diagnostic :** 401 = clé invalide ; 403 = outil non autorisé par le compte ; 429 = quota ou limites ; 400/404/422 = modèle ou API incompatible ; délai = problème de réseau. Le téléchargement d'une page publique en HTTPS est distinct de l'autorisation d'utiliser le moteur `web_search` de Mistral. Ne communiquez jamais votre clé API dans un rapport.

La mémoire personnelle reste chiffrée sur le téléphone, sans sauvegarde automatique vers GitHub, et la commande STOP / anti-écho v3.8.4 reste disponible.

## FRIDAY v3.8.4 — Anti-écho TTS Android

FRIDAY ne doit plus traiter comme question la voix émise par son propre haut-parleur. Pendant toute réponse (Mistral, streaming ou lecture TTS), le Vosk général est temporairement suspendu. Un décodeur Vosk indépendant conserve la reconnaissance des commandes « STOP », « Arrête » et « J'ai une précision » pendant la réponse, dans les limites acoustiques du téléphone.

Après la fin de la voix, la dictée normale reprend après **2,5 secondes** pour éviter de retranscrire l'écho résiduel. Après un STOP volontaire, ce délai est de **900 ms**. Les nouvelles questions vocales doivent donc être posées après cette courte attente ; le bouton d'interruption et la saisie écrite restent disponibles.

La suppression de l'écho n'est pas une garantie de reconnaissance des commandes STOP sur tous les haut-parleurs. Un casque réduit les retours acoustiques. La mémoire Memory+ reste chiffrée sur le téléphone, sans synchronisation GitHub ; l'accès Web Mistral v3.8.3 est conservé.

## FRIDAY Web v3.8.3 — Recherche Internet mieux diagnostiquée

Dans **Réglages > Recherche Web**, activer **« Activer et tester la recherche Web Mistral »**. La recherche passe par l'API Mistral **Conversations** et son outil officiel `web_search` (pas Chat Completions). Le bouton ne confirme un succès que si l'API fournit une URL source.

Si le modèle de dialogue choisi n'exécute pas l'outil, ou s'il est rejeté avec HTTP 400/404/422, FRIDAY essaie **une fois** `mistral-small-latest` pour la recherche, sans changer le modèle préféré pour les conversations. En cas d'erreur HTTP **401**, vérifier la clé Mistral ; **403**, les droits d'accès à l'outil ; **429**, le quota/limite. Changer de modèle ne débloque pas des droits Web manquants. L'accès à certaines fonctions de Mistral peut être facturé.

Pour lire une page publique directement, saisir une adresse `https://` puis choisir **« Analyser cette page Web avec Mistral »**. Aucun microphone n'est nécessaire. Les refus des sites, dont les 403, ne sont pas contournés. Si possible, FRIDAY recherche à la place des sources publiques alternatives et les distingue explicitement de la page qu'il n'a pas pu lire. Les paramètres de l'URL refusée ne sont pas transmis à cette recherche.

Le profil, les données GPS et les notes privées restent soumis aux permissions existantes ; Memory+ demeure chiffrée sur le téléphone et jamais synchronisée automatiquement vers GitHub.

## FRIDAY Web v3.8.2 — Dépannage des pages inaccessibles

Dans **Réglages > Recherche Web**, la lecture d'une page HTTPS publique ne nécessite plus de démarrer le microphone. Saisissez `https://www.example.com/` et cliquez sur **Analyser cette page Web avec Mistral**. FRIDAY corrige également `https//` et `https:/` en `https://` quand un domaine est fourni.

Un message **HTTP 403** indique que **le site refuse l'accès automatisé**, et non nécessairement que la clé Mistral est invalide. FRIDAY n'affirme pas avoir lu une page bloquée et ne contourne pas ses restrictions. Si besoin, poser une **nouvelle demande de recherche Web sur le sujet** avec des sources accessibles via l'outil Mistral, ou consulter le site dans son navigateur.

La récupération d'une page publique est distincte de l'outil `web_search` de Mistral ; testez les deux fonctionnalités séparément dans Réglages. Memory+ reste locale et chiffrée ; aucune donnée personnelle n'est synchronisée vers GitHub.

## FRIDAY v3.8.1 — Web réellement vérifié

La recherche Web utilise **Mistral Conversations / web_search** (pas Chat Completions). Dans Réglages, « Activer et tester la recherche Web Mistral » réalise un véritable essai avec la clé API enregistrée ; un échec est visible dans l'interface. Une recherche demandée explicitement ne doit pas être présentée comme accomplie si l'outil n'a pas été appelé.

Pour **analyser une page précise**, saisir une URL HTTPS publique dans le champ Web des Réglages et cliquer « Analyser cette page Web avec Mistral » après avoir démarré l'écoute. FRIDAY lit le texte public de la page localement (sans Google API), puis envoie à Mistral un extrait borné pour répondre ; il cite l'URL effectivement consultée. Les sites nécessitant une connexion, le rendu JavaScript, des pages privées et les contenus non textuels ne sont pas garantis.

La mémoire personnelle reste chiffrée uniquement sur Android. Le partage des notes Memory+ avec Mistral est une permission **distincte**, désactivée par défaut ; aucune synchronisation vers GitHub.

## FRIDAY Memory+ v3.8.0

**La nouvelle mémoire est privée par défaut.** Elle conserve jusqu'à 200 notes catégorisées (600 caractères chacune) sur le téléphone, chiffrées via Android Keystore. Recherche, modification, suppression et marquage de relecture se font dans les Réglages, hors ligne.

La mémoire locale et son partage avec Mistral ont **deux autorisations indépendantes** : l'envoi des notes pertinentes à Mistral est désactivé par défaut. Aucune note, clé ou conversation n'est automatiquement synchronisée vers GitHub. L'historique vocal des 40 derniers échanges est lui aussi stocké sous chiffrement avec migration de l'ancienne version.

Une commande « cherche dans ma mémoire ... » fonctionne localement, tandis que « retiens que ... » mémorise uniquement à la demande. Les suppressions globales exigent une confirmation dans les Réglages. La mémoire ne remplace pas la consultation des sources : elle peut contenir des informations anciennes ou inexactes.

[Documentation de confidentialité Memory+](docs/PRIVACY_MEMORY_PLUS_V380.md)

## v3.7.3 — Répétition de STOP et diagnostic microphone

- Les répétitions « stop stop », « stop stop stop » et « arrête arrête » sont traitées comme une seule commande d'interruption pendant la parole, sans exiger « Friday ».
- La télémétrie d'interruption affiche le **niveau sonore PCM capté** (0–100), indicatif et local. Un niveau non nul peut aussi provenir de l'écho : il ne prouve pas que le mot « stop » a été reconnu.
- Si le bouton « Interrompre la réponse » fonctionne mais pas la commande orale, vérifier le réglage d'interruption, l'accès microphone Android et essayer un casque pour réduire l'écho.

## Correctif d'interruption v3.7.2

**Deux décodeurs vocaux** sont désormais utilisés pendant une réponse : le Vosk libre pour les questions et un décodeur Vosk limité aux commandes « stop », « arrête », « attends » et « j'ai une précision ». Le second reste inactif hors réponse pour limiter la consommation. Son état apparaît dans le cockpit sous **Interruption vocale**. Le bouton Interrompre fonctionne même si Android coupe le microphone.

Il s'agit d'une amélioration de reconnaissance nécessitant **des tests sur téléphone**, pas d'une garantie matérielle que la voix sera captée au haut-parleur.

# 🤖 F.R.I.D.A.Y. — Assistant personnel Android

**F.R.I.D.A.Y.** est un assistant vocal et conversationnel **application Android native** : cockpit **HUD** holographique (WebView), backend **Python embarqué**, détection de mot-clé en **Rust**, reconnaissance vocale **hors-ligne (Vosk)** et IA **Mistral**.

> Sources **v3.7.0** — application en développement actif.
> APK **release signé** publié par la CI (voir [Releases](../../releases/latest)).

---

## Feuille de route des prochaines versions

Le plan de développement validé est disponible dans **[docs/ROADMAP_MISTRAL.md](docs/ROADMAP_MISTRAL.md)** :

- **[v3.5.0 — Fiabilité Mistral, GPS et Web](https://github.com/philippe015/friday-assistant/issues/29)** : diagnostics, reprise réseau annulable, tests du profil actif, consentement GPS.
- **[v3.6.0 — Streaming et TTS progressif](https://github.com/philippe015/friday-assistant/issues/30)** : démarrage vocal accéléré, file de phrases et interruptions fiables.
- **[v3.7.0 — Reconnaissance vocale avancée](https://github.com/philippe015/friday-assistant/issues/31)** : vocabulaire Vosk, options Voxtral et consommation batterie.

Ces fonctions sont **prévues**, pas encore livrées. L'IA reste **Mistral uniquement** ; le profil actif définit la personnalité et le GPS ne peut être transmis à Mistral qu'avec l'accord ponctuel de l'utilisateur.

---

## Correctifs d'interruption v3.7.1

- « Stop », « Arrête », « Silence », « Pause » ou « Attends » peuvent interrompre une réponse **sans répéter le mot Friday**, uniquement pendant que FRIDAY répond.
- « J'ai une précision », avec ou sans « Friday », coupe la réponse et garde le micro ouvert pour que l'utilisateur précise sa demande.
- Le TTS Android est interrompu **avant** d'attendre la déconnexion réseau Mistral ; son démarrage est synchronisé avec l'ordre d'arrêt.
- Dans le mode « une question », le micro reste ouvert pendant la réponse pour permettre les interruptions puis s'arrête après la fin de la réponse. La compréhension des commandes dépend toutefois de la capture audio et de l'anti-écho réellement disponibles sur l'appareil.

## Nouveautés v3.5–v3.7
- v3.5.0 : fiabilité réseau et modèles Mistral.
- v3.6.0 : streaming SSE et TTS progressif.
- v3.7.0 : Vosk spécialisé facultatif et écoute une question.

## ✨ Fonctionnalités (v3.4.1)

### 🎙️ Voix
- **Écoute interactive** : le microphone ne se coupe plus pendant le calcul Mistral ni pendant la lecture TTS (micro et traitement sur deux threads)
- **Interruption par la voix** :
  - « Friday stop » / « Friday attends » → coupe **l'audio seul**, le micro reste actif
  - « Friday, j'ai une précision » → coupe la réponse et ouvre une courte fenêtre d'écoute
  - « Friday, explique autrement » → interrompt et relance une nouvelle réponse
  - « Friday, arrête l'écoute » → arrête complètement le service
- **Mot-clé « Friday »** détecté par une bibliothèque **Rust** (`native/`, JNI)
- **Vosk** pour la reconnaissance française **hors-ligne**
- **TTS Android** pour la restitution
- Anti-écho : `VOICE_COMMUNICATION` + `AcousticEchoCanceler` / `NoiseSuppressor` (selon matériel)

### 🧠 Intelligence
- **Mistral Chat Completions** (`https://api.mistral.ai/v1/chat/completions`)
- **Recherche Web intelligente** : Mistral Conversations et l'outil intégré `web_search` pour les questions factuelles, les relances et la consultation des liens publics. Les sources sont affichées uniquement lorsqu'elles sont réellement fournies par l'API. Les appels Web peuvent être facturés.
- **Réglage Web explicite** : case Recherche Web et bouton pour l'activer et tester réellement l'accès. En cas d'échec, l'application le signale.
- **Mémoire personnelle locale chiffrée** sur demande
- **10 profils de personnalité** (16 000 caractères), chiffrés via **Android Keystore**
- **Cerveau local Python** : heure, date, jour, année — **fonctionne sans réseau**

### Correctifs v3.4.1
- **Identité du profil actif** : FRIDAY répond au nom choisi ; « Mistral » désigne le moteur de génération, pas l'assistant.
- **GPS Mistral avec consentement** : l'activation Android du GPS est indépendante du partage de coordonnées à Mistral. Activez le partage ponctuel depuis le cockpit et redémarrez l'écoute si le GPS a été activé après le démarrage du service.
- **Transparence** : indicateur « GPS vers Mistral » et dernier envoi API confirmé, sans coordonnées persistantes.
- **Tests à faire sur appareil** : identité, coordonnées, recherche d'informations locales via Mistral Web.

### Fonctionnement amélioré (v3.4.0)
- **Personnalité active** : le prompt sauvegardé du profil choisi est appliqué à chaque réponse Mistral, que la recherche Web soit active ou non ; le cockpit affiche le nom de ce profil.
- **Diagnostic Web** : état réel de l'option et du test de l'outil, sans confondre « activé » et « testé ».
- **GPS** : questions géolocalisées avec permission Android et diagnostic de la précision ; accord distinct pour un partage ponctuel à Mistral.
- **Interruption et précision** : « Friday, attends, j'ai une précision » stoppe la réponse mais conserve le sujet pour la question suivante.

### 🖥️ Interface
- Cockpit HUD holographique (HTML/CSS/JS local dans un WebView)
- Journal vocal local, sessions, diagnostics intégrés (Internet, clé, micro)
- Bouton **« Interrompre la réponse »** dans le cockpit **et** dans la notification
- Écoute possible **écran verrouillé** (service de premier plan)

## 🏗️ Architecture

```
app/src/main/java/fr/friday/assistant/   (24 classes Java)
├── MainActivity.java          (58 Ko)  activité principale + WebView
├── FridayService.java         (39 Ko)  service de premier plan (micro, TTS, HTTP)
├── NativeAI.java              orchestration DeepSeek (annulation, tokens)
├── VoiceSampleRecorder.java            capture de l'échantillon vocal
├── BargeInPolicy.java                  règles stop / précision / autrement
├── FridayLocation.java                 GPS + précision
├── AndroidPlaceResolver.java           résolution de lieux
├── NearbyIntent.java / LocationIntent.java
├── PersonalMemory.java / MemoryIntent.java
├── PersonalityProfiles.java            profils chiffrés
├── SecurePrefs.java                    stockage chiffré (Keystore)
├── ModelInstaller.java                 installation du modèle Vosk
├── RustWake.java                       pont JNI → mot-clé Rust
├── WebSearch.java                     recherche web et relances
├── FridayVersionGuard.java / FridayVersionPolicy.java
└── ...

app/src/main/python/friday_logic.py     logique locale (heure / date)
native/src/lib.rs + Cargo.toml          détection de mot-clé (Rust, JNI)
app/src/main/assets/index.html          cockpit HUD
tests/                                  tests Java + Python
scripts/                                build, diagnostic, installation
```

### Chaîne d'exécution

```
Micro ─► Vosk / Rust (mot-clé) ─► FridayService ─► NativeAI ─► API DeepSeek
                                        │                        │
                                        ▼                        ▼
                                   TTS Android  ◄──── réponse ──┘
                                        │
WebView (cockpit HUD) ◄── état/événements ── FridayService
```

## 📥 Installation

1. Télécharger l'APK depuis **[Releases](../../releases/latest)**
2. Autoriser l'installation de sources inconnues
3. Lancer l'app → **Réglages natifs** → bouton **« Se connecter / créer ma clé »** (page DeepSeek) → coller la clé → **Enregistrer**

**Pré-requis** : Android **8.0+** (API 26), appareil **arm64-v8a**.

> ⚠️ APK **debug** (`debuggable=true`, signature debug) : usage personnel / test.


**Depuis la v3.3.0 :** utiliser exclusivement une clé API Mistral. Les anciennes clés DeepSeek sont supprimées des préférences ; elles ne sont jamais envoyées à Mistral. Activez la recherche Web dans les réglages pour utiliser `web_search` avec Conversations. Cette fonction dépend de l'offre API et peut être facturée.

## 🛠️ Compiler depuis les sources

Voir **[BUILD.md](BUILD.md)**. En résumé :

| Outil | Version |
|---|---|
| JDK | 17 ou 21 |
| Python | **3.14** (Chaquopy `buildPython`) |
| Rust + cargo-ndk | stable |
| Android SDK | 35 + build-tools 35.0.0 |
| Android NDK | 27.2 ou 28.2 |
| Gradle | 8.9+ (AGP 8.7.3) |

```bash
bash tests/../scripts/tester_sources.sh   # tests Java + Python
bash scripts/compiler_manjaro.sh          # APK dans dist/
```

## 🔐 Sécurité & vie privée

| Point | État |
|---|---|
| Clés API | **chiffrées** via `SecurePrefs` / Android Keystore |
| Profils de personnalité | chiffrés |
| `usesCleartextTraffic` | `false` ✅ |
| `allowBackup` | `false` ✅ |
| Clés en dur dans le code | **aucune** ✅ |
| Micro / GPS | activés **volontairement**, service visible (notification) |
| Partage de position | **facultatif** et ponctuel |

## ⚠️ Limites connues

- Le téléphone peut entendre sa propre voix (l'anti-écho n'est pas une validation d'identité vocale) — un casque améliore nettement la fiabilité
- L'annulation d'une requête HTTP peut survenir **après** facturation côté fournisseur
- Le plein-duplex dépend des restrictions micro d'Android selon l'appareil
- **arm64-v8a uniquement** (pas d'émulateur x86)
- Le build exige **Python 3.14** et le **NDK** ; il n'est pas réalisable sur toutes les machines

## 🗺️ Feuille de route

- [x] APK **v2.2.0** compilé (debug) et publié
- [x] APK **release signé** par la CI (keystore en secret GitHub, `debuggable=false`)
- [ ] Workflow GitHub Actions (build reproductible)
- [ ] Historique persistant chiffré des conversations
- [ ] Thèmes du cockpit
- [ ] Variante `armeabi-v7a`

## 🛠️ Développement

| Ressource | Rôle |
|---|---|
| [CONTRIBUTING.md](CONTRIBUTING.md) | flux de contribution, conventions, processus de release |
| [CHANGELOG.md](CHANGELOG.md) | historique des versions |
| [BUILD.md](BUILD.md) | compilation (Linux x86_64, aarch64, CI) |
| [Tests](tests/) | tests Java, Python et Rust |

**Intégration continue** :

| Workflow | Déclencheur | Action |
|---|---|---|
| [`Tests`](.github/workflows/ci.yml) | pull request, push | tests Java / Python / Rust / syntaxe JS |
| [`Build APK`](.github/workflows/build.yml) | push, tag `v*` | APK debug + APK **release signé**, publication sur tag |

Dependabot surveille **Gradle**, **Cargo** et les **GitHub Actions**.

## 🔐 Signature de release

Les APK de release sont signés automatiquement par GitHub Actions avec un keystore dédié
(secrets : `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`).

```
Certificat : CN=Philippe, OU=FRIDAY, O=philippe015, L=Paris, C=FR
SHA-256    : 81becec7f794bb647cd5abb1db9d84f12d51c5c9b00f609bde52804c2930106e
```

> ⚠️ Sauvegardez le keystore : sans lui, aucune mise à jour de l'app ne pourra être signée avec la même identité.

## 📄 Licence

MIT — voir [LICENSE](LICENSE).

## 📚 Documentation complémentaire

- [ANALYSE.md](ANALYSE.md) — analyse statique de l'APK v2.0.2 (composants, permissions, libs, sécurité)
- [docs/README_FR.md](docs/README_FR.md) — documentation d'origine de l'auteur
- [docs/README_ECOUTE_INTERRUPTION_V220.md](docs/README_ECOUTE_INTERRUPTION_V220.md) — spécification de l'écoute interactive
- [docs/README_VOIX_CLONE_V219.md](docs/README_VOIX_CLONE_V219.md) — clonage vocal
- [docs/README_INTELLIGENCE_V216.md](docs/README_INTELLIGENCE_V216.md) — mémoire et analyse approfondie
