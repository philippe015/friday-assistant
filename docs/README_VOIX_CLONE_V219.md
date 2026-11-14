# F.R.I.D.A.Y. v2.1.9 — Correction activation voix clonée

## Cause et résolution

La version v2.1.8 cochait « Utiliser la voix clonée » après une capture réussie mais ne persistait pas `clone_enabled=1` avant un enregistrement manuel. La capture et l’activation sont désormais une seule séquence : validation du consentement, capture de 12 s, contrôle de présence du WAV, activation `clone_consent`, `clone_enabled` et `voice_enabled` dans les préférences privées Android. L’indicateur du cockpit et le libellé des réglages sont rafraîchis immédiatement.

## Sur le Samsung

1. Installer v2.1.9 par mise à jour de la version v2.1.8 si la signature est identique ; sinon ne pas supprimer les données avant sauvegarde.
2. Ouvrir Réglages, vérifier la clé Mistral et arrêter l’écoute native (ainsi que toute autre édition susceptible d’utiliser le micro).
3. Cocher la confirmation du droit d’utilisation de la voix.
4. Appuyer sur « Enregistrer mon échantillon vocal (12 s) ». Parler pendant les 12 secondes.
5. Vérifier « Échantillon : ENREGISTRÉ | Voix clonée : ACTIVÉE » ; **aucune pression supplémentaire sur « Enregistrer les réglages » n’est nécessaire**. Si l'échantillon provient de la v2.1.8, cochez le consentement et appuyez sur « Activer mon échantillon déjà enregistré », sans nouvelle capture.
6. Appuyer sur « Tester ma voix clonée », avec écoute arrêtée. Démarrer ensuite l'écoute pour parler avec F.R.I.D.A.Y.

Si le test échoue, le statut doit afficher la raison. Une erreur HTTP 401/403 concerne la clé ou les droits API ; 429 peut correspondre à des limites ou crédits ; 400/404/422 peuvent indiquer une demande ou un modèle refusé. La disponibilité du TTS dépend du compte Mistral.

La génération vocale est **en ligne et peut coûter des crédits**, sans deuxième API. L’échantillon WAV local est envoyé à Mistral à chaque génération par `ref_audio`. Le bouton Supprimer l’échantillon désactive immédiatement la voix clonée. La voix Android reste le repli lors des échecs.

## Compiler sur Manjaro

```bash
cd ~/Téléchargements
unzip -o FRIDAY_v2_1_9_Android_Clonage_Auto_Manjaro.zip
cd FRIDAY_v2_1_9_ANDROID_VOIX_CLONE_AUTO
bash scripts/tester_sources.sh
bash scripts/compiler_manjaro.sh
adb install -r dist/FRIDAY_v2.1.9_Android_Manjaro_Debug.apk
```

## Limites de validation

Tests de logique Java/Python/JavaScript/Rust fournis. La compilation complète Android et la génération vocale réelle sont à valider sur le Samsung. Cloner uniquement sa propre voix ou une voix utilisée avec accord explicite.
