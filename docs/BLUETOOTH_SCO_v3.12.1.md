# FRIDAY v3.12.1 — Vérification du microphone Bluetooth

## Requis
- Casque Bluetooth **mains libres HFP/SCO** (la seule lecture de musique A2DP ne suffit pas).
- Dans Android > Bluetooth > casque > **Appels** activé, casque connecté.
- Dans FRIDAY > Réglages > **Privilégier le microphone du casque Bluetooth** activé.
- Android 12+ : autorisation **Appareils à proximité** accordée. Toujours autoriser le microphone.

## Essai sur appareil physique
1. Connecter le casque et vérifier qu'un appel téléphonique utilise son micro.
2. Dans FRIDAY, arrêter complètement l'écoute précédente, enregistrer les réglages, puis démarrer l'écoute.
3. Sur Android 8–11, FRIDAY doit reconnaître le profil HFP, demander SCO et attendre jusqu'à 6 s sa connexion **avant** d'ouvrir AudioRecord.
4. Dans les diagnostics FRIDAY, vérifier **« Microphone du casque Bluetooth actif (capture confirmée par Android) »**.
5. Éloigner le téléphone, parler dans le micro du casque : **« Friday, quelle heure est-il ? »**.
6. Tester trois questions en gardant le téléphone loin et vérifier la sortie TTS dans le casque.
7. Couper le Bluetooth pendant une session et vérifier le repli sur le microphone du téléphone.
8. Reconnecter le casque puis redémarrer l'écoute pour réessayer le profil HFP.

## En cas d'échec
- **« Profil Appels HFP désactivé »** : activer Appels dans les détails du casque.
- **« Liaison SCO impossible »** : le casque est connecté pour la musique mais Android n'a pas confirmé le canal microphone ; FRIDAY bascule sur le micro du téléphone.
- **« Micro du téléphone actif »** : `AudioRecord.getRoutedDevice()` ne confirme pas un micro Bluetooth. Ne pas confondre routage demandé avec routage réel.
- **« Micro Android muet »** : quitter les autres applications enregistrant l'audio, puis redémarrer l'écoute.
- Ne jamais désinstaller FRIDAY pour une mise à jour : les clés API et les préférences privées pourraient être perdues.

## Vérification en CI
Les tests d'intégration statiques vérifient la séquence HFP → SCO → AudioRecord, les permissions et le repli, mais aucun émulateur CI ne reproduit fidèlement le matériel Bluetooth HFP du téléphone. **Un test réel est obligatoire avant de considérer le défaut résolu.**
