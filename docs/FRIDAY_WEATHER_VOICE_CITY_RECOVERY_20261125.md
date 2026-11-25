# FRIDAY 3.14.31 — Weather Voice City Recovery

Préversion Android du 25 novembre 2026, basée sur **3.14.30**. Objectif : corriger le problème de Vitry-sur-Seine identifié dans un rapport de conversation local, sans publier les conversations personnelles dans GitHub.

## Symptômes constatés

La demande « météo pour demain » est comprise mais Vosk transcrit les réponses suivantes avec des fragments phonétiques tels que « scène France » ou « sur scène ». L'outil météo essaie alors parfois de chercher une ville inexistante, puis une recherche générale Wikipédia traite le mot « scène » comme un sujet sans rapport avec la météo. La récupération Web a pris environ 32 secondes.

## Correctifs intégrés

1. **Réparation phonétique prudente** : uniquement lorsqu'un résultat Vosk contient explicitement *Vitry/Vitri* et *sur/du/dur* et *Seine/scène/senne/sène*, la recherche utilise « vitry-sur-seine, france ». Une seule syllabe « scène » ne suffit **jamais** à reconstruire « Vitry ».
2. **Continuité météo** : les réponses de confirmation, d'échec de géocodage ou de demande répétée de ville gardent leur contexte local. Les fragments ambigus déclenchent **une question de précision**, pas une recherche Internet générale.
3. **Période conservée** : lorsque l'utilisateur demande d'abord la météo **demain**, cette période est reprise après une réponse limitée au nom de la commune. Une nouvelle période prononcée clairement dans la réponse est prioritaire.
4. **Fenêtre vocale des précisions** : après une clarification météo prononcée, FRIDAY autorise de nouveau pendant 50 secondes une réponse de type nom de commune (tout en conservant les protections déjà installées contre l'auto-écoute TTS et les déclenchements non sollicités).
5. **Diagnostic respectueux de la vie privée** : codes `WEATHER_CITY_ASR_CLARIFY` et `WEATHER_CITY_PHONETIC_ACCEPTED`, sans enregistrer les paroles dans le journal technique.
6. **Compatibilité** : LiteRT-LM local, modèle de 2,34 Go, dialogue chiffré, météo Open-Meteo, TTS, microphone et recherche Web demeurent inchangés hors de ces corrections ciblées.

## Limite importante

Quand Vosk ne transcrit **pas du tout** « Vitry », il est impossible de déterminer avec certitude quelle ville a été demandée. JARVIS doit faire répéter ou permettre l'écriture du nom dans le dialogue. Les réglages existants permettent éventuellement un vocabulaire Vosk personnel, mais une grammaire restrictive ne doit pas être activée par défaut : elle peut dégrader la dictée libre.

## Tests de régression

Tests JVM et Python avec phrases entièrement synthétiques pour : variations Vitry-sur-Seine, refus de deviner depuis « scène France », maintien du contexte après deux clarifications, conservation de « demain », absence de fuite de conversations privées vers Open-Meteo, compatibilité des anciens tests. Compiler l'APK ARM64 et valider manuellement le fonctionnement de Vosk sur le téléphone.

## Version

DEBUG `3.14.31-weather-voice-city-preview` / code `125`, package de test stable `fr.friday.assistant.v31421matrix`. Production Google Play inchangée `3.14.18` / code `112`.

La PR est chaînée sur 3.14.30, sans fusion automatique vers la branche principale.
