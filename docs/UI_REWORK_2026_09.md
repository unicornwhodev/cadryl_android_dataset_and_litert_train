# Reprise de l’interface Cadryl — 30 septembre 2026

Le propriétaire refuse l’interface précédente et demande une interface aboutie,
moderne, dynamique et intuitive. La priorité porte sur l’usage à taille normale.
Les contrôles d’accessibilité restent distincts ; ils ne remplacent pas le jugement
sur la qualité du produit. Aucun retrait du train, des modèles ou des outils métier.

## Constat sur la Release avant reprise

Captures physiques fraîches : `test-results/final01-oss-oppo-ui-portrait-reconnect-20260930/`
et `test-results/final01-oss-oppo-ui-landscape-20260930/`, APK
`36744cd4c4abb932e7e5747384d22041bf40f5865adc080e8afedf7e1b1780f5`.
Ces dossiers ignorés restent des preuves locales, non des données à publier.

1. Atelier : rendu et commande accessibles, mais aperçu disproportionné,
   action de configuration incohérente avec une image déjà éditable, outils secondaires peu visibles.
2. Lot : filtres fonctionnels, mais faible séparation entre outils et contenu.
3. Modèles : import, catalogue et réglages accessibles ; densité et métadonnées
   nuisent au repérage. L’apprentissage doit rester explicitement accessible.
4. Export : confirmations conservées ; formats présentés comme petits réglages techniques.
5. Réglages et documents : accessibles ; hiérarchie visuelle et espacement à reprendre.

Captures seules : clavier, TalkBack, gestuelle complète, tous les états réseau et
la validation humaine de la qualité finale restent à contrôler.

## Première structure — historique

- Atelier : une prochaine action, avancement réel du lot, aperçu, outils du projet.
- Lot : recherche, filtres distincts, images et statut de chaque cas.
- Modèles : catalogue, import, contrats et entraînement conservés.
- Export : formats descriptifs, archive locale et publication, preuves et confirmations.
- Composants communs : espaces réguliers, cartes de section, typographie,
  surfaces cyan/violet dans cette première itération, sélection animée et navigation cohérente.

## Périmètre fonctionnel conservé

Les 16 routes du studio restent disponibles : atelier, configuration guidée,
lot, éditeur, modèles, apprentissage, similarité, workflow, export, qualité,
préférences, projets, source, destination, contrat modèle et outils avancés.
Les services métier, protections des corrections humaines, copie relue avant
purge et confirmations de publication/suppression restent la base de qualification.

La reprise doit être compilée, exercée et vue sur appareil. Les suites de la
Release précédente ne qualifient pas automatiquement les nouvelles APK.
La validation visuelle du propriétaire reste la condition de la future RC finale.

## Simplification demandée après le premier rendu

- Suppression des onglets redondants sur l’atelier et des compteurs répétés.
- Une commande suivante selon le lot : configurer, préparer, annoter ou ouvrir.
- Aperçu plus compact, navigation principale stable, modèles et entraînement accessibles.
- Aides de section et détails du catalogue dépliés dans la page.
- Choix de formats descriptifs sans fenêtre d’aide supplémentaire.
- Options du modèle repliables ; la saisie non enregistrée reste en mémoire
  quand ce panneau est replié. Un test Compose contrôle ce comportement.
- Contrôle UTF-8 des libellés ajouté aux tests des outils.

Les confirmations de publication, de suppression, de remplacement de propositions
et de purge restent présentes. L’apprentissage, les modèles, les contrats et
les fonctions avancées restent disponibles. La simplification ne vaut pas une
certification de tous les états métier ni l’acceptation finale du propriétaire.

## Recette de cette simplification

Le build source du 30 septembre `20260930T041742Z-120348f06359` passe
122 tests JVM, zéro erreur lint (86 avertissements) et génère 101 fichiers KSP.
La suite Android passe 58/58 sur l’AVD API 36 / pages 16 Ko ; les deux matrices
produisent 288 captures. Les nouveaux tests ouvrent l’apprentissage depuis
l’accueil et vérifient qu’une option du modèle non enregistrée survit au repli.
Le test existant de sauvegarde réelle des réglages ouvre maintenant le panneau
« Model options » avant de modifier le prompt et de relire sa valeur dans Room.

Les échecs initiaux restent conservés : champ replié non ouvert dans l’ancien
parcours du test, puis fenêtre sans focus et arrêt ColorOS lors d’un essai sur
Oppo avec le volet système ouvert. La recette minifiée a aussi révélé une
recherche dans un élément de liste non composé et des API Compose supprimées
par R8 dans le nouveau test. Le défilement utilise la liste réelle ; les API
exactes du test sont conservées sans désactiver l’optimisation.
Le verrou Windows d’un JAR Gradle a nécessité l’arrêt du daemon dédié au projet.
Aucun de ces essais incomplets n’est qualifié. La reprise Release réussit 58/58
sur l’Oppo et conserve toutes les tables lors du remplacement de l’APK.
Les matrices Oppo réussissent 144 cas par orientation et le pilote indépendant
passe 4/4 : accueil, navigation import/export/qualité, projet après redémarrage,
préférence enregistrée puis restaurée. Les 1 000 images sources publiques
conservent leurs empreintes. Ces scénarios et captures ne remplacent pas la
recette de tous les états ni l’acceptation visuelle du propriétaire.
Les reçus du candidat et la portée de chaque reprise sont liés dans
[les preuves de préparation](FINAL01_EVIDENCE.json).

## Nouvelle direction du studio

Le propriétaire a retenu le lynx fourni et demandé une interface plus sobre,
avec une identité propre. La nouvelle UI native utilise un fond porcelaine,
une console graphite, des accents vermillon, Barlow et une seule famille
d’icônes Phosphor Duotone. Les sources et licences des assets sont conservées
dans `third_party/design/`. Le logo est repris sans modification de ses pixels.

- Outils regroupe images, modèles, apprentissage, export, qualité et réglages.
- L’action principale de l’accueil reste visible pendant le défilement.
- L’éditeur conserve sélection, boîte, point et masque à portée immédiate ;
  Plus ouvre polygone, lasso, remplissage, gomme, SAM et déplacement.
- Classe, annuler/rétablir, zoom, enregistrement, report et validation restent
  dans la console. Les propositions IA affichent un accès à leur relecture.
- Les propriétés donnent accès aux régions, légendes, tags, liens texte/région,
  VQA, comptage et qualité selon les tâches du projet. Les consignes du workflow
  sont aussi accessibles dans ce panneau.
- Le paysage utilise une barre compacte. Le changement de projet conserve
  un retour à l’accueil et les confirmations métier restent présentes.

Les imports, exports, modèles et apprentissage restent les services existants.
La publication, la suppression et la purge gardent leurs confirmations et
leurs protections de données. L’édition publique garde son identité Android
et se compile sans SDK publicitaire, abonnement ni backend.

Cette évolution des sources a sa propre recette. Les résultats historiques
ci-dessus et les APK déjà publiées ne qualifient pas automatiquement la refonte.
Les essais intermédiaires ayant échoué sont conservés ; ils ne sont pas promus
en succès. La revue humaine du propriétaire reste distincte des tests.

[Parcours du studio actuel](STUDIO_UI_2026_09.md) · [Comparaison visuelle](../design-qa.md).
