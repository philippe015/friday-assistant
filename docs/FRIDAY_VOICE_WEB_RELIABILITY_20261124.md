# FRIDAY 3.14.28 — Voice & Web Reliability+

Date : 24 novembre 2026. Préversion développée à partir de 3.14.27.

## Correctifs basés sur le diagnostic vocal de l'application 3.14.26

- Le test sur dix questions de LiteRT-LM (modèle 2,34 Go) est stable,
  mais trois questions vocales ont révélé un blocage Vosk et des
  recherches Web hors sujet.
- **Filtre de transcription vocale** : une très longue sortie Vosk sans
  requête identifiable au début déclenche un message de clarification,
  un code diagnostic `VOSK_LONG_UNRELIABLE_REJECTED` et aucune recherche.
  Le filtre n'est appliqué **qu'à la voix**, jamais à une question écrite.
  Les vraies dictées longues formulées comme un ordre clair restent possibles.
  Des erreurs Vosk courtes ne seront pas toutes détectées par cette règle
  conservatrice.
- **Programmes TV / films ce soir (France)** : l'IA interroge un seul
  guide public : `https://www.programme-tv.net/`, et ne produit que des
  horaires dont la **date en France**, la chaîne, le titre et l'heure
  sont explicitement lisibles dans la page. Une diffusion ne peut pas
  être garantie : la grille peut changer. Les requêtes concernant un
  autre pays ne sont pas redirigées silencieusement sur la TNT française.
- La lecture est du HTML HTTPS avec une taille plafonnée et sans JS.
  Un blocage, une grille non datée, un contenu inutilisable ou un refus
  du site donne une réponse transparente avec lien du guide,
  **jamais un horaire ou film inventé**.
- Ce chemin court ne lance pas les recherches génériques Bing,
  DuckDuckGo, Wikipédia, Wikidata, etc. et évite une partie des
  délais de 35-42 secondes constatés pour les demandes TV.
  **Il n'y a pas encore de garantie d'un temps maximal absolu** :
  DNS, réseau mobile et génération locale peuvent prendre plus longtemps.

## Confidentialité

Les données privées, la mémoire conversationnelle, le modèle IA,
les commandes GPS, les diagnostics complets et l'audio ne sont pas
transmis au guide TV. Seule une URL publique, identique pour tous
les utilisateurs, est consultée. Le code du diagnostic Vosk est
enregistré sans exporter la transcription privée sur GitHub.

## Tests

- Scénarios synthétiques d'une longue transcription incohérente,
  dictée avec commande explicite, demande TV en français et
  reconnaissance de films uniquement.
- Extraction stricte d'une grille TNT synthétique datée,
  rejet d'un programme périmé et de l'absence de grille.
- Garde-fous Web, modèle local, continuité des conversations,
  tests Java/Python et compilation Gradle ARM64.

## Version

DEBUG 3.14.28-voice-web-reliability-preview, versionCode 122,
applicationId `fr.friday.assistant.v31421matrix`.

Release Google Play reste `3.14.18` / code `112`, non modifiée.
Branche PR chaînée sur 3.14.27 ; aucune publication automatique
sur Google Play ni fusion dans main.
