# Politique de confidentialité — JARVIS (BROUILLON À VALIDER AVANT PUBLICATION)

Dernière révision : 23 novembre 2026.

**ATTENTION : Cette page est un projet de texte et ne doit pas être utilisée comme
politique finale de Google Play sans compléter les coordonnées du responsable
et vérifier le comportement exact de la version AAB soumise.**

## Responsable et contact

Application : JARVIS — Assistant IA local.  
Éditeur / responsable de traitement : **[IDENTITÉ OU RAISON SOCIALE À COMPLÉTER]**  
Contact confidentialité : **[E-MAIL DE CONTACT À COMPLÉTER]**  
Pays : France, à confirmer par le responsable.

## À quoi servent les données ?

JARVIS peut utiliser le microphone pour transcrire des questions vocales et
répondre au moyen de la synthèse vocale Android. Ces opérations reposent sur
les autorisations Android accordées par l'utilisateur. La reconnaissance
Vosk et les moteurs IA locaux compatibles peuvent fonctionner sur l'appareil,
sous réserve que les modèles nécessaires aient été importés et activés.

JARVIS peut conserver localement des échanges précédents et des préférences
dans l'espace privé de l'application ; la mémoire conversationnelle est
protégée par le stockage sécurisé du téléphone. Les informations conservées
peuvent inclure le texte de messages que l'utilisateur saisit ou prononce.

Lorsque la **recherche Web** est activée et utilisée, les termes de la question
ou de la recherche peuvent être transmis à des services Internet tiers,
par exemple des moteurs de recherche, des encyclopédies, des agrégateurs
d'actualités ou des services météo. Les fournisseurs précis et leurs
propres règles de confidentialité doivent être vérifiés dans la version publiée.

Si l'utilisateur autorise la **localisation**, JARVIS peut consulter la position
fournie par Android pour répondre à une demande de proximité. Une demande
de lieux proches peut nécessiter une transmission à un service OpenStreetMap /
Overpass suivant les réglages et l'action choisie. Les autorisations Android
peuvent être retirées dans les paramètres du téléphone.

## Diagnostics

L'application peut enregistrer des événements techniques locaux et permettre
d'exporter un rapport. Le contenu des conversations n'est inclus dans un
export que lorsque l'utilisateur choisit cette option. **Un fichier de rapport
exporté avec des conversations n'est pas chiffré** : il peut contenir des
données personnelles. L'utilisateur doit le relire avant de le partager.

L'application n'est pas censée synchroniser automatiquement les conversations
privées avec un dépôt public GitHub ; si une fonction de signalement de bug
est utilisée, le contenu envoyé doit faire l'objet d'une action volontaire et
d'un examen préalable de l'utilisateur.

## Conservation et suppression

Les préférences, modèles importés et conversations stockés sur le téléphone
restent présents tant que l'utilisateur ne les supprime pas ou n'efface pas
les données de l'application. Les événements techniques ont une durée de
rétention locale configurable dans l'application (14 jours par défaut
dans les rapports diagnostiques examinés). Les politiques de conservation
des fournisseurs Web sont distinctes et doivent être vérifiées.

## Services tiers

La version Android actuellement préparée inclut des bibliothèques Android,
de reconnaissance vocale, de génération locale et d'exécution Python/Rust.
Avant publication, un audit doit confirmer quels fournisseurs externes
peuvent recevoir des requêtes, avec quels paramètres, et quelles éventuelles
télémétries leurs bibliothèques produisent.

## Sécurité, droits et contact

Les communications réseau sont prévues en HTTPS. Les autorisations sensibles
sont contrôlées par Android. Pour toute question sur la vie privée, l'accès,
la rectification ou la suppression, utiliser l'adresse de contact ci-dessus,
après qu'elle aura été complétée.

**Conditions de validation** :
- vérifier sur appareil et par revue réseau les données réellement transmises ;
- compléter l'identité, les coordonnées et les prestataires ;
- vérifier les demandes de suppression et la conformité RGPD ;
- mettre en ligne la version définitive à une URL publique pérenne ;
- ajouter son URL dans l'application et sur la fiche Google Play ;
- aligner les réponses de la rubrique « Sécurité des données » sur ce texte.

Ce brouillon n'affirme pas que les fonctions Internet sont entièrement locales
ni qu'aucune donnée ne quitte le téléphone.
