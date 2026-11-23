# Analyse ciblée — JARVIS v3.14.18-ecoute — 23 novembre 2026

Source : deux exports **privés** du téléphone vers 17 h 28 et 18 h 03 (heure France),
versions identiques `v3.14.18-ecoute`. Les rapports bruts contiennent des
conversations personnelles, **ne pas les publier** sur un dépôt public.

## Constat dans le second export

- 420 événements techniques (journal glissant), 28 messages affichés
  dans l'historique conversationnel.
- 217 événements `VOSK_FINAL`, 8 événements `VOSK_DIRECT_QUESTION`,
  un `VOSK_WAKE`, 4 événements `MIC_DEVICE_CONFLICT`.
- Un `VOSK_FINAL` est une dictée finalisée, **pas nécessairement** une question
  volontaire ni une défaillance du microphone.
- Après une invitation à préciser la ville, « vitry sur seine ce soir »
  aboutissait à une recherche Web sans lien avec la météo.
- Une réponse locale antérieure à « météo Vitry-sur-Seine » fonctionnait.
- Une réponse du moteur local se présentait comme « Gemma 4 », en
  contradiction avec le profil FRIDAY. Problème encore ouvert.
- L'article RSS 1 a bien été sélectionné, mais le texte éditeur intégral
  n'avait pas pu être vérifié. Ne pas inventer ce contenu.

## Correctifs apportés dans cette PR

1. **Météo conversationnelle** : `WeatherFollowUpPolicy` accepte désormais
   « Vitry sur Seine ce soir », « Vitry-sur-Seine, France ce soir » après la
   question « Pour quelle ville souhaitez-vous la météo ? ». Aucun historique
   conversationnel n'est transmis aux moteurs Web : seule la nouvelle requête
   géographique explicite passe à Open-Meteo.
2. **Transcription incertaine** : après cette question météo, la variante
   « c'est sur scène france » n'est pas soumise au moteur de recherche général.
   L'assistant demande de repréciser la commune, sans la deviner.
3. **Diagnostic** : le code `VOSK_NO_WAKE` est enfin émis quand
   `FridayService` écrit « Question transcrite ... **non envoyée** ».
   L'ancien code ne détectait que « **pas envoyée** », ce qui masquait
   cette cause de rejet. Les paroles ne sont jamais conservées dans
   les événements techniques.
4. Tests Java/JVM + Python vérifiant la mémoire contextuelle météo, le
   routage normal des nouvelles questions et l'absence de journalisation
   du contenu sensible.

## Limites et essais encore nécessaires

- Ce correctif ne modifie pas l'acoustique Vosk et n'attribue pas une
  interprétation certaine à chaque `VOSK_FINAL`.
- Le problème `MIC_DEVICE_CONFLICT` (4 événements) nécessite une
  reproduction et l'inspection de l'arbitrage AudioRecord/Android ;
  il ne permet pas à lui seul de conclure que le microphone est arrêté.
- La demande « ce soir » est conservée dans la requête mais l'outil
  `JarvisWeather` affiche encore un **bulletin journalier** ; il faudra
  implémenter des données horaires pour des prévisions spécifiquement
  nocturnes. Ne pas présenter les min/max journaliers comme les
  températures de cette soirée.
- La signature et le package d'une nouvelle APK de test devront être
  vérifiés avant installation ; ne pas écraser la version fonctionnelle.
- Aucun changement du package `jarvis.myapp` destiné à Play Console,
  aucune publication et aucune fusion automatique.
