# Audit technique F.R.I.D.A.Y. v3.8.9 — dialogue vocal sur plusieurs tours

## Périmètre vérifié

Source de référence : branche principale au commit `c042456f29083fb95fbfd1d5a70d0f81ddf8ce53` (v3.8.8), avec corrections regroupées en v3.8.9. Audit des modules Android `FridayService`, `NativeAI`, `MainActivity`, `TtsEchoGate`, `VoiceStopSpotter`, `WebSearch`, `RecentNewsPolicy`, `SecurePrefs`, `PrivateConversationHistory`, `ModelInstaller`, du manifeste, du pipeline de tests Java/Python/Rust/JS et de la publication de l'APK.

### Défauts vérifiés dans le code

| Priorité | Défaut | Mécanisme | Correction |
|---|---|---|---|
| Critique | Reconnaissance coupée pendant Mistral | `TtsEchoGate.suppressGeneralAsr(currentTurn != null,...)` bloquait Vosk même durant une recherche Web sans TTS | Utiliser `speakingNow` pour bloquer seulement le haut-parleur actif ; une nouvelle question commençant par « Friday » peut interrompre la recherche |
| Haute | Double décodage Vosk pendant l'appel réseau | `stopRecognizer` fonctionnait pendant tout `currentTurn` même sans synthèse sonore | Démarrer le décodeur spécialisé STOP seulement pendant la lecture, Vosk général restant libre durant les appels réseau |
| Haute | Surconsommation Vosk lorsque le micro est muet | `silencedByPolicy` provoquait `recognizer.close(); createRecognizer()` sur chaque trame sans son | Réinitialisation unique à l'entrée et à la sortie du mode muet, deux reconstructions maximum, recours à `VOICE_RECOGNITION` après un échec de `VOICE_COMMUNICATION` |
| Haute | Tour réseau sans garde supérieure | Aucune durée totale de réponse active ; plusieurs appels Web et des temporisations peuvent empêcher de clôturer le tour | Annulation du tour au bout de 150 s *uniquement avant la parole*, retour à l'écoute, requête HTTP annulable |
| Moyenne | L'erreur API est silencieuse | Le `catch` final envoyait un événement texte mais ne parlait pas | Courte annonce TTS locale de panne de Mistral quand le moteur de voix est disponible ; erreur toujours visible dans le diagnostic |
| Moyenne | Impossible de distinguer les causes | L'utilisateur ne voyait pas nettement quel composant était en attente ou muet | Fenêtre « Diagnostic complet de l'écoute » affichant moteur, Vosk, micro Android, TTS, mode conversation et statut Web, copie sans secrets |
| Confidentialité | URL complète dans la télémétrie | `web_last_status` pouvait contenir des paramètres privés d'URL non chiffrés | Conserver uniquement le nom de domaine dans la télémétrie de connexion |

Les erreurs d'écho TTS, les requêtes Web sans sources, la commande STOP, la mémoire locale et la protection Keystore sont conservées sans assouplissement des permissions.

### Limites de cette validation

Les tests de source confirment la logique, les tests Java le fonctionnement déterministe de l'anti-écho et de l'expiration réseau, et les builds vérifient la compilation. Ils **ne peuvent pas mesurer** le comportement réel d'Android `AudioRecord`, des politiques de concurrence microphone, de Vosk dans un environnement bruyant, du volume média ou de la voix TTS sur le téléphone de l'utilisateur. Selon la documentation Android, `AudioRecordingConfiguration.isClientSilenced()` reflète notamment une priorité d'enregistrement concurrente. Voir : https://developer.android.com/media/platform/sharing-audio-input

### Scénario de recette sur téléphone

1. Installer la v3.8.9 par-dessus la v3.8.8, sans désinstaller ni effacer les données chiffrées.
2. Vérifier que « Mode une question » est désactivé ; lancer volontairement l'écoute native.
3. Dire « Friday, donne-moi les actualités des sept dernières heures ».
4. **Avant** la fin du réseau Mistral, tester « Friday, quelle heure est-il ? » : la nouvelle question doit être reçue même si le premier appel était encore en attente, sans que l'application soit bloquée par l'anti-écho.
5. Attendre la fin d'une réponse vocale, patienter 2,5 s puis poser trois autres questions avec « Friday ».
6. Pendant la synthèse, vérifier « STOP », puis la reprise du micro.
7. Dans Réglages, ouvrir « Diagnostic complet de l'écoute » ; vérifier que l'état est cohérent et copier le rapport **sans clé, GPS ni conversations** en cas d'échec.
8. Vérifier une question écrite et un test Web pour distinguer le réseau, le micro et la synthèse.

### Points nécessitant des traces d'appareil si le défaut persiste

- L'application a-t-elle bien le droit d'utiliser le micro ? Android indique-t-il qu'un autre client audio le rend muet ?
- La question apparaît-elle dans l'historique local ? Si non : capture / Vosk / activation vocale. Si oui sans réponse : réseau Mistral ; si réponse texte sans son : moteur TTS / sortie média.
- Le diagnostic affiche-t-il « recherche en cours », « TTS/anti-écho », « micro prêt », « micro Android muet », « délai dépassé » ?
- Les alternatives de source audio (`VOICE_COMMUNICATION`, `VOICE_RECOGNITION`) fonctionnent-elles avec le matériel et sa version Android ?

Aucune garantie « zéro bug » n'est annoncée sans test physique de longue durée.
