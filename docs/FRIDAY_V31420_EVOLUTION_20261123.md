# FRIDAY — évolution conversationnelle 23 novembre 2026

Statut : **branche de développement / pas une version stable Google Play**.

Base : `fix/jarvis-v31419-ecoute-weather-diagnostics-20261123` (les tests
des rapports privés du 23 novembre). L'objectif est de faire évoluer la
version JARVIS/FRIDAY Android déjà en place **sans activer de modèle
distant** ni envoyer de conversations personnelles sur GitHub.

## 1. Véritable météo horaire « ce soir »

Avant : `JarvisWeather` répondait par les minimums et maximums d'une journée
entière, même lorsqu'on demandait les températures de la soirée.

Maintenant :
- Open-Meteo est appelé avec `hourly=temperature_2m,precipitation_probability,weather_code`.
- `WeatherEveningPolicy` sélectionne **18 h à 23 h** dans le fuseau de la
  ville géocodée et, pour la soirée en cours, exclut les heures déjà passées.
- Les réponses « demain soir » choisissent le lendemain et non un mot
  `soir` mal interprété comme une partie du nom de la commune.
- FRIDAY présente la plage d'heures réellement vérifiées, une fourchette
  de températures et la probabilité maximale de pluie lorsqu'elle existe.
- Si la soirée est déjà terminée, le fuseau est invalide ou les données
  horaires manquent, FRIDAY l'indique. Il n'invente pas une prévision.
- Seul le nom de la commune fourni par l'utilisateur est envoyé au
  géocodeur ; aucune coordonnée GPS ni historique privé n'est transmis
  à partir du téléphone.

## 2. FRIDAY garde son identité malgré le nom du modèle

Certains modèles locaux se présentaient spontanément comme « Gemma 4 »
alors que l'utilisateur s'adresse à FRIDAY.

`AssistantResponseIdentityGuard` corrige uniquement une auto-présentation
du moteur **au début** d'une réponse libre, par exemple « Je suis Gemma 4. »
vers le nom de l'assistant configuré.

Une question explicite sur le moteur (« Quel modèle es-tu ? ») n'est jamais
masquée : son nom doit rester accessible, notamment dans les diagnostics.
Les citations d'articles et les explications sur les modèles ne sont pas
modifiées. Le garde-fou fonctionne pour les backends locaux LiteRT-LM
et MediaPipe.

## 3. Réponse vocale naturelle après « Pour quelle ville ? »

Après cette question précise de FRIDAY, une courte fenêtre de **50 secondes**
est ouverte quand la réponse vocale est terminée, afin d'accepter une ville
clairement prononcée sans redire « Jarvis ». Elle est conditionnée par :
- service vocal actif et aucun blocage anti-écho en cours ;
- réponse précédente demandant explicitement une ville météo ;
- ville à plusieurs composants, composée avec tirets ou pays explicite,
  ou locution volontaire « pour Paris » ;
- durée strictement limitée ; le mode une seule question ne l'active pas ;
- toutes les autres questions continuent à respecter le mot d'activation
  ou la fenêtre normale du cockpit.

Les phrases floues comme « bonne soirée » ne sont pas réinterprétées
comme des villes. L'interprétation suit ensuite la politique météo locale
avec conservation du contexte chiffré Android.

## 4. Tests automatisés et risques

`tests/test_friday_v31420_evening_persona.py` compile et exécute des
tests JVM sur l'extraction des heures 18–23, les données déjà passées,
l'identité de l'assistant, la demande explicite du modèle et la
reconnaissance prudente des villes parlées.

Restent à vérifier **sur téléphone** :
- qualité de reconnaissance Vosk et absence de faux déclenchements réels ;
- conflits `MIC_DEVICE_CONFLICT` dans le flux AudioRecord Android ;
- GPS/Overpass selon disponibilité réseau ;
- compatibilité des modèles LiteRT-LM/MediaPipe et du service vocal en
  arrière-plan, selon les permissions Android.

Ces changements restent une PR. Ils ne modifient ni l'APK installé, ni
les clés chiffrées de l'utilisateur, ni l'application Google Play
`jarvis.myapp`, ni la signature de publication.
