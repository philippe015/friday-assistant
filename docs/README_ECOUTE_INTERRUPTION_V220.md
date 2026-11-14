# F.R.I.D.A.Y. v2.2.0 — Écoute interactive et interruption

Évolution de la v2.1.9 (voix clonée Mistral fonctionnelle sur le téléphone selon les essais utilisateur).

## Objectif
- Ne plus fermer le microphone pendant le calcul Mistral et la lecture TTS : micro et traitement sur deux threads.
- « Friday stop », « Friday attends » : arrêter uniquement l'audio, sans désactiver le micro.
- « Friday, j'ai une précision » : arrêter la réponse et garder une courte fenêtre d'écoute pour recevoir la précision.
- « Friday, explique autrement » : interrompre et lancer une nouvelle question.
- « Friday, arrête l'écoute » : arrêter volontairement le service complet.
- Bouton « Interrompre la réponse » dans le cockpit et notification Android.
- Annulation des requêtes HTTP Mistral ainsi que des segments audio de Voxtral, et blocage d'une ancienne réponse annulée.
- Réduction de l'écho via VOICE_COMMUNICATION et AcousticEchoCanceler/NoiseSuppressor, selon compatibilité matérielle.
- Détection de fin de phrase plus rapide (2,1 s par défaut, réglable) et fenêtre de suivi de 75 s.

## Limites et précautions
- L'annulation HTTP peut arriver après facturation d'une requête déjà reçue par Mistral.
- Le téléphone peut entendre sa propre voix. L'antibouclage par mot-clé + tentative d'anti-écho n'est pas une vérification d'identité vocale ; un casque améliore beaucoup la fiabilité.
- Le fonctionnement plein-duplex dépend de l'appareil et des restrictions micro d'Android. Vérifier sur Samsung réel, écran allumé puis verrouillé.
- Les questions de suivi sans « Friday » ne sont admises qu'après une activation explicite et durant la fenêtre de dialogue.
- Un GPS lent peut retarder la fin de l’ancien calcul ; il n’a toutefois pas le droit de prononcer une réponse une fois annulée.

## Tests de terrain recommandés
1. Activer l'écoute avec la voix clonée, poser « Friday, explique le système solaire pendant trente secondes ».
2. Au milieu de la phrase prononcée, dire « Friday stop » : la lecture doit s'arrêter, le micro rester actif.
3. Redemander « Friday, explique autrement » : une autre réponse doit commencer sans ancienne réponse réinjectée.
4. Dire « Friday, j'ai une précision » pendant une réponse, puis « ajoute aussi les planètes naines ».
5. Refaire avec écran verrouillé, haut-parleur puis casque, pour contrôler l'écho.
6. Tester le bouton « Interrompre » (et la notification), les questions tapées et « Friday arrête l'écoute ».

## Compilation sous Manjaro
```bash
cd ~/Téléchargements
unzip -o FRIDAY_v2_2_0_Android_Ecoute_Interruptions_Manjaro.zip
cd FRIDAY_v2_2_0_ANDROID_ECOUTE_INTERRUPTION
bash scripts/tester_sources.sh
bash scripts/compiler_manjaro.sh
# Après BUILD SUCCESSFUL
adb install -r dist/FRIDAY_v2.2.0_Android_Manjaro_Debug.apk
```

Cette archive contient les sources Android. Aucun APK compilé n'est inclus.
