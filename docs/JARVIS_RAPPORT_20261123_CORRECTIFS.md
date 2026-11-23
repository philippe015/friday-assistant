# JARVIS Android · Correctifs du rapport 23/11/2026

Source diagnostique : rapport privé de l'utilisateur (non copié dans le dépôt).  
Base : JARVIS v3.14.18 Kotlin Preview.  
Nouvelle édition de test : identifiant Android `fr.friday.assistant.v31418logfix`, distinct des APK antérieurs.

## Anomalies reproduites et corrections

- **Actualités de la nuit :** une dictée a confondu la demande de bulletin avec une recherche du mot « envie ». La catégorie « informations de la nuit » déclenche maintenant un bulletin de titres datés sur la fenêtre glissante de 24 h. Les amorces reconnues « j'ai envie d'un mode... » / « j'ai envie donne... » ne doivent pas devenir des sujets de recherche. La fenêtre de 24 h ne garantit pas une sélection uniquement entre le coucher et l'aube.
- **Article n° 1 :** « dit l'article entier numéro un » est une commande d'article numéroté. Elle vise le dernier bulletin RSS conservé sur le téléphone ; les droits d'accès au contenu intégral de l'éditeur demeurent limités.
- **Relance coupée :** « de en entier » ne déclenche plus une recherche Wikipédia sur les entiers naturels ; JARVIS demande un numéro d'article.
- **Identité de l'assistant :** une question comme « Comment tu t'appelles ? » reçoit une réponse calculée localement depuis le profil actif. Elle ne déclenche plus de recherches absurdes sur les communes contenant « Appelles ». L'application ne prétend pas utiliser Mistral en mode IA locale uniquement.
- **Recherche des services proches :** l'échec Overpass reçoit désormais un code de diagnostic selon la famille d'erreur (délai, DNS, TLS, réseau ou autre) : aucune coordonnée ni URL brute n'est enregistrée.

## Tests

- Tests unitaires Java de lecture RSS et actualités récentes.
- Tests Kotlin JVM : clarification d'une relance tronquée.
- Tests Python de câblage de l'identité locale, du flux RSS et des catégories d'erreurs GPS.
- Compilation Android debug dans GitHub Actions, puis essai réel nécessaire sur Android.

## Confidentialité et compatibilité

Le rapport JSON original contient des dialogues en clair et ne doit pas être envoyé à un dépôt public.
L'APK debug se signe avec une clé de test ; il utilise volontairement un identifiant
Android différent pour s'installer à côté de la version précédente sans écraser
ses préférences, son historique, ses modèles ni ses clés Android Keystore.

La validation CI n'implique pas que les résultats Web seront accessibles sur tous les réseaux.
La recherche OpenStreetMap est sur consentement séparé. Aucune correction du moteur IA
ne garantit la transcription parfaite des mots prononcés par l'utilisateur.
