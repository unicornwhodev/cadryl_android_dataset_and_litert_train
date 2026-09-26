# Configuration et transferts — 26 septembre 2026

Cette évolution, livrée dans rc7, part des difficultés signalées sur téléphone : réglages dispersés, classes difficiles à saisir, contrats de modèles et exports refusés. Elle conserve l’identité Android, les annotations humaines et les confirmations de publication et de suppression.

## Constat sur le téléphone

La version installée est la Release 4.2.0-rc6. Un lot montre un transfert refusé avec HTTP 403 et une publication en attente de réconciliation. Dans cet état, l’ancienne interface empêche aussi de créer une archive locale. Les reçus d’inférence visibles les plus récents indiquent des exécutions réussies ou sans proposition ; ils ne permettent pas de reproduire les anciens échecs de dimensions signalés.

La lecture a été effectuée par l’interface Android. La Release ne permet pas `run-as` : sa base privée n’a pas été extraite. Les relevés restent dans le dossier local ignoré `dist/ux-20260926`. Aucun jeton, nom de dépôt privé ou corpus du téléphone ne figure dans cette note. La version du téléphone n’a pas été remplacée.

## Parcours regroupés

- **Atelier** : travail en cours, configuration, source et projets.
- **Modèles** : bibliothèque, réglages, essai sur image et apprentissage. Le contrat JSON reste accessible dans les outils avancés.
- **Export** : copie locale, publication et destination avec stockage. Les formats sont enregistrés une seule fois pour le ZIP et HF.

Les réglages courants du modèle sont séparés des paramètres CPU et des limites, repliés par défaut. Changer d’étape ou d’onglet de bibliothèque ramène au début du contenu concerné.

## Parcours guidé et compatibilité des classes

L’accueil sans images explique le parcours et affiche une action pour configurer le projet. Le guide a trois étapes : **Images**, **Objectif**, **Vérifier**. Il utilise une seule barre de navigation. Le choix du dossier local est présenté en premier ; les champs HF apparaissent lorsque cette source est choisie. La destination d’export se règle dans Export, sans être un passage obligatoire pour commencer.

Trois objectifs courants expliquent leur résultat : entourer les objets, classer les images ou les décrire. Les autres outils restent dans un panneau replié. Une description ou des questions/réponses peuvent être configurées sans imposer de classe. L’écran final résume le choix, distingue une source HF inspectée d’une source encore à vérifier et permet de choisir l’assistance automatique ou le travail manuel.

La compatibilité des classes est calculée avec le contrat actif et les outils choisis :

- Les modèles à classes fixes demandent une correspondance exacte. Les classes manquantes restent à annoter à la main. Les indices réservés RF-DETR ne sont pas proposés et l’ordre numérique du modèle reste intact.
- Les classes proposées peuvent être recherchées et sélectionnées individuellement. Les listes collées et l’ajout de toutes les classes restent possibles. Les noms contenant des virgules ou des guillemets sont conservés entre sélection, stockage et export.
- TinyCLIP compare des textes configurables ; cela ne prouve pas sa précision. Les modèles à sortie libre demandent un essai sur image. Les masques interactifs demandent un point ou une boîte et utilisent la classe configurée du masque.
- Une incompatibilité complète arrête la préannotation avant le décodage des images. Une compatibilité partielle permet l’assistance ; les nouvelles propositions des modèles à vocabulaire connu sont limitées aux classes du projet. Les annotations humaines existantes ne sont pas filtrées.

Ces vérifications portent sur le contrat et les métadonnées disponibles. Elles ne prouvent ni l’exactitude des noms fournis par l’auteur d’un fichier brut, ni la précision du modèle sur le corpus de l’utilisateur. L’essai sur image reste accessible dans les réglages du modèle.

## Prévenir les refus évitables

Les classes acceptent une liste collée avec virgules, points-virgules ou retours à la ligne. Elles se retirent individuellement et les labels du modèle peuvent être ajoutés au projet en une action. Cette opération ne réordonne pas les sorties du modèle et ne réécrit pas les annotations.

L’inspection d’une source propose une colonne contenant réellement une référence HTTPS exploitable. Les objets `src`/`url` et les listes contenant une seule image sont acceptés. Une liste avec plusieurs images et une cellule tronquée demandent toujours une décision explicite.

Les dimensions, le layout et le type d’entrée sont comparés au véritable graphe LiteRT avant l’enregistrement des réglages. Pour une forme fixe et non ambiguë, un bouton reprend les dimensions du fichier. Une forme dynamique conserve les bornes et le pas déclarés par le contrat. Un fichier brut ne permet toujours pas de deviner ses labels, sa normalisation ou ses sorties.

Avant un export COCO ou YOLO, l’interface affiche les classes manquantes et permet de les ajouter au projet. Désactiver ces projections permet de conserver le JSONL complet. Les contrôles d’intégrité, de stockage et de décisions finales restent appliqués.

## Récupération d’un envoi bloqué

Pour un lot préparé, en cours d’envoi, publié sans vérification complète ou en conflit, une archive de récupération copie le paquet déjà préparé. Elle vérifie le snapshot des annotations, le manifeste et les empreintes du reçu avant la copie. Un paquet modifié est refusé sans remplacer une archive de récupération existante.

La copie SAF est relue et comparée. Elle ne change pas le parent HF, l’état de publication ou les droits de nettoyage. Une erreur HF 403 demande toujours des droits valides sur le dépôt ; cette évolution ne peut pas accorder ces droits.

## Validation

La [campagne rc7](RC7_RELEASE.md) ajoute les builds Release signés et les tests sur leurs octets exacts. Les résultats Debug ci-dessous restent ceux du 26 septembre.

Le build final produit les deux APK et passe 84 tests JVM, 87 tests Python et 45 tests Android sur émulateur API 36 x86_64 en pages de 16 Ko. Le lint rapporte 0 erreur et 124 avertissements. Voir [le rapport de tests](../TEST_REPORT.md) pour les reçus.

Les nouveaux tests couvrent la saisie des classes, la détection de colonne, les dimensions réelles, les classes manquantes à l’export et la récupération d’un paquet figé avec refus après altération. Ils distinguent aussi les axes d’image variables d’un simple nombre variable d’images par appel, et les classes nécessaires à COCO de celles nécessaires à YOLO.

Cinq captures natives finales en français ont été inspectées : accueil, images, objectif, classes et vérification. Elles sont conservées localement dans `dist/ux-guided-20260926/screenshots-fr-final`, avec un modèle synthétique LiteRT et un projet de test. L’identifiant HF de démonstration n’a déclenché aucun accès distant. Les captures du premier passage (dimensions du modèle, export et destination) restent dans `dist/ux-20260926/screenshots-fr`. Aucun écran du corpus utilisateur n’est publié.

La qualification de cette évolution est distincte de celle de rc6. Une compilation Debug et des tests sur émulateur ne qualifient ni la Release signée, ni un téléphone ARM, ni une publication HF réelle.
