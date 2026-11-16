# FRIDAY Memory+ v3.8.0 — Confidentialité et fonctionnement

## Objectif
Améliorer la mémoire personnelle sans créer un cloud de données privées. La mémoire fonctionne **sur l'appareil Android**, sous chiffrement AES-GCM protégé par **Android Keystore**.

## Ce qui est réellement implémenté
- Jusqu'à **200 notes de 600 caractères**, par catégorie : \`[projet]...\`, \`[travail]...\` ou catégorie « Général ».
- Recherche locale par mots-clés, déduplication, modification/suppression d'une note, date de **relecture manuelle** (pas de certification factuelle).
- Migration des anciennes 12 notes et de l'ancien historique de conversations sur les mêmes téléphones, une fois l'écriture chiffrée vérifiée. **L'ancien stockage en clair est effacé logiquement après migration**, sans pouvoir garantir un effacement physique des blocs flash anciens ou des copies préexistantes.
- Stockage chiffré strict **sans repli Base64 en clair**. Si Keystore est indisponible, l'application refuse la mémorisation plutôt que d'enregistrer du texte non chiffré.
- Historique vocal local des 40 derniers messages chiffré ; aucune synchronisation de cet historique.
- Autorisations distinctes : (1) mémoire locale activée ; (2) transmission de notes pertinentes à Mistral **désactivée par défaut**, à cocher explicitement. Sans (2), aucune note Memory+ n'est ajoutée aux requêtes réseau Mistral.
- Si (2) est activée : au maximum cinq notes liées aux mots significatifs de la question, tronquées à environ 1800 caractères au total ; aucune note sans correspondance n'est envoyée. Les notes sont traitées comme données non fiables, pas comme directives système.

## Ce que cette protection ne garantit PAS
- **Les questions posées à Mistral** et les messages de contexte volontairement envoyés utilisent l'API distante de Mistral, donc quittent nécessairement le téléphone.
- Les sauvegardes ou captures effectuées hors de FRIDAY avant l'installation de la v3.8.0 peuvent avoir conservé d'anciennes données ; une migration ne peut pas assurer un effacement physique de flash ou de tous les supports tiers.
- En cas de perte ou réinitialisation du Keystore (changement de téléphone, restauration d'ancienne sauvegarde, réinitialisation), les données chiffrées peuvent devenir illisibles. Il n'y a pas de synchronisation ou de récupération cloud automatique.
- La « relecture » d'une note signifie uniquement que l'utilisateur l'a marquée ainsi, pas qu'une source externe en a prouvé l'exactitude.
- La recherche de notes est **lexicale**, pas une mémoire vectorielle/embeddings ou une recherche sémantique avancée.

## GitHub : usage strictement limité
Le dépôt public héberge seulement le code, les instructions, les tests et les versions **sans notes, profil privé, clés API ni historique personnel**. FRIDAY Memory+ ne comporte **aucun connecteur de synchronisation GitHub**.

## Effacer ses données
Dans **Réglages > Memory+**, sélectionner une note et **Supprimer** (confirmation), ou utiliser **Effacer toutes les notes mémorisées** (confirmation). L'historique vocal dispose d'une action distincte « Effacer les échanges vocaux locaux ». La commande orale de suppression globale demande d'utiliser l'écran de confirmation.

## Tests
Les tests Java vérifient le découpage de catégories, la normalisation, le moteur de recherche lexical, les limites, la déduplication et les intentions de commande. Les tests de contrôle du code surveillent le refus des replis non chiffrés, la séparation des consentements et le chemin de migration de l'historique.

Des essais sur **un téléphone réel** sont nécessaires pour vérifier la migration de données existantes, les échecs du Keystore et l'expérience de l'utilisateur. Les tests CI ne prouvent pas à eux seuls le comportement sur l'appareil.
