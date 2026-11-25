# FRIDAY 3.14.30 — Voice Intelligence & Stability+

Version de test Android du 25 novembre 2026. Suite de FRIDAY 3.14.29, sans publication Google Play automatique.

## Anomalies traitées, d'après le diagnostic 3.14.29

1. **Demande de télévision le soir** : le moteur reconnaît désormais « Qu'est-ce qu'il y a à la télévision ce soir ? », « Qu'est-ce qui passe à la télé ce soir ? » et leurs transcriptions précédées de mots parasites. Les demandes passent à l'outil TV public et daté existant, non à la recherche générale de termes incohérents. Le guide est limité à la grille française **du soir**, pas à celle du lendemain.
2. **Météo** : le parseur public `WeatherQueryPolicy.city` distingue correctement `Joinville le Pont, France` de `Joinville le Pont en, France` lorsque l'utilisateur dit « météo sur ... en France ». La ville seule, les formes avec virgule et les demandes sans pays restent possibles. Seul le nom de ville choisi est envoyé au géocodeur, pas l'historique chiffré.
3. **LiteRT-LM** : le verrou partagé de l'inférence attend maintenant par périodes de 750 ms pendant **18 secondes maximum**, avec annulation vérifiée entre périodes, plutôt que d'échouer immédiatement après 3 secondes. Cette attente réduit les collisions transitoires avec un benchmark ou une autre génération ; elle ne rend pas possibles deux inférences simultanées, et ne résout pas une inférence native bloquée. Aucun appel IA distant n'est ajouté.
4. **Microphone** : l'ancien comportement Android de détection `isClientSilenced()`, reprise automatique et protection anti-écho est conservé. L'application enregistre en plus le code local `MIC_POLICY_RESTORED` lorsqu'elle retrouve le microphone. Si Android impose un silence pour une autre application, FRIDAY ne peut pas contourner cette politique ni identifier arbitrairement la cause.
5. **Actualités** : les résultats écrits continuent d'inclure la liste complète des titres, dates RSS et liens. Le TTS lit désormais **au plus deux titres**, avec une invitation courte à demander un article plus complet. Cette amélioration ne modifie pas la politique de vérification des dates, les sources ni le moteur IA local.

## Confidentialité et compatibilité

Tests GitHub synthétiques, sans copier les journaux personnels, transcriptions ni adresses privées dans le dépôt. Les historiques restent chiffrés localement ; seuls des sujets publics explicitement demandés peuvent servir de recherche. Aucun changement du modèle LiteRT-LM 2,34 Go.

Préversion DEBUG : `3.14.30-voice-intelligence-stability-preview`, code **124**, package `fr.friday.assistant.v31421matrix` pour conserver les données entre préversions. Production Google Play toujours `3.14.18`, code **112**, non modifiée.

## Tests sur le téléphone

- « Jarvis, qu'est-ce qu'il y a à la télévision ce soir ? » : résultat du guide TV, réponse vocale brève.
- « Jarvis, donne-moi la météo sur Joinville-le-Pont en France » : le nom reconnu ne doit plus contenir « en, France ».
- « Jarvis, les actualités des dix dernières heures » : dates et liens à l'écran, deux titres prononcés au maximum.
- Pendant un benchmark local, poser une question ordinaire puis contrôler la présence ou non d'une erreur `LITERT_INFERENCE_FAILED`.
- Contrôler si les codes `MIC_DEVICE_CONFLICT` et `MIC_POLICY_RESTORED` apparaissent dans le nouveau diagnostic.

Les performances réelles dépendent toujours du modèle, du réseau, du moteur TTS, d'Android et des éventuels accès simultanés au microphone.
