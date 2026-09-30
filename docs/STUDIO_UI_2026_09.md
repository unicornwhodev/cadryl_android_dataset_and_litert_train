# Le studio Cadryl

La refonte native du 30 septembre utilise le lynx retenu, Barlow, Phosphor Duotone et les couleurs porcelaine, graphite et vermillon. [Assets et attributions](BRAND.md) · [Revue visuelle](../design-qa.md).

## Un point d’entrée pour les outils

**Outils** reste accessible en haut des écrans. Choisir un outil ferme le volet et ouvre le panneau correspondant ; Retour retrouve le parcours précédent. Le changement de projet revient à un accueil propre, après l’enregistrement des corrections.

| Besoin | Accès |
|---|---|
| Reprendre le travail | Studio ; l’action principale reste visible pendant le défilement |
| Voir et choisir une image | Images ; grille du lot et filtres |
| Changer ou créer un projet | Projets |
| Choisir ses images et ses tâches | Configurer ; Images source |
| Importer ou choisir un modèle | Modèles ; Catalogue, Installés, Presets, Importer |
| Vérifier les dimensions et le contrat | Contrat du modèle ; options avancées si nécessaires |
| Préparer, suivre ou reprendre l’apprentissage | Apprentissage |
| Exporter et vérifier la copie | Exporter ; Destination |
| Contrôler les cas à terminer | Qualité |
| Organiser un parcours guidé | Workflow |
| Modifier les préférences ou relancer le didacticiel | Réglages |

## L’éditeur

Le projet, la position dans le lot et le nom du fichier restent visibles. La grille permet de revenir au lot. Le canevas conserve les proportions de l’image, les gestes de zoom et les coordonnées des annotations.

- **Sélection, Boîte, Point, Masque** apparaissent selon les tâches choisies.
- **Plus** ouvre polygone, lasso, remplissage, gomme, SAM et déplacement lorsqu’ils sont applicables.
- La console rassemble classe, annuler/rétablir, annotations, zoom, état d’enregistrement, Passer et Valider. Le nombre de classe correspond au vocabulaire réel du projet.
- **Annotations** ouvre les propriétés : régions, légendes, tags, liens texte/région, VQA, comptage et qualité. Les tâches du projet déterminent les onglets disponibles.
- Les propositions à relire et **Suggérer** apparaissent lorsqu’un modèle ou des propositions sont présents. Le mode manuel ne simule aucune proposition IA.
- Les consignes du workflow sont consultables depuis les propriétés.
- En paysage, la barre compacte laisse plus de place à l’image ; les commandes complémentaires restent accessibles.

Les annotations humaines, brouillons importés et propositions IA conservent leurs statuts distincts. La validation des propositions, la publication, la suppression et la purge gardent leurs confirmations. Une copie doit être relue avant le nettoyage.

## Modèles, apprentissage et exports

Le changement d’interface conserve les services métier : inspection/import de modèle, contrats réels LiteRT, préannotation, bibliothèque et versions entraînées. L’apprentissage est facultatif ; il crée une copie et conserve l’original. Reprise, abandon explicite, contrôles et activation manuelle restent disponibles selon le modèle.

L’export conserve JSONL, COCO, YOLO, WebDataset et vision-langage, ainsi que les destinations locale/SAF et HF autorisées. Les erreurs, conflits et opérations en cours ne deviennent pas des succès visuels.

Le didacticiel est interactif et fonctionne sans backend. Sa case finale désactive les lancements automatiques ; les réglages permettent de le relancer.

## Portée de la recette

Les builds, essais Android, parcours physiques et comparaisons visuelles disposent de reçus séparés. Les anciennes releases restent liées à leurs APK et preuves d’origine ; les captures de la refonte décrivent les sources actuelles. La revue visuelle interne et les tests ne remplacent pas l’acceptation du propriétaire.
