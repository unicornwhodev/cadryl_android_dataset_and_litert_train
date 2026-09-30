<img src="app/src/main/res/drawable-nodpi/ic_cadryl.png" alt="Lynx Cadryl" width="76">

# Cadryl

**Ton atelier Android pour transformer des images en datasets.**

Tu importes tes images, tu vérifies ce que propose le modèle, tu corriges et tu exportes. Cadryl rassemble ces étapes dans une app qui garde tes annotations au centre du travail. Tu peux aussi tout faire à la main, puis entraîner un modèle compatible sur les lots que tu as relus.

Un projet indépendant de **Unicorn Who Dev**, auparavant nommé *Vision Dataset Studio*.

[Release 0.0.1](https://github.com/unicornwhodev/cadryl_android_dataset_and_litert_train/releases/tag/v0.0.1) · [Premier lot](docs/GETTING_STARTED.md) · [Documentation](docs/README.md) · [English](README.en.md)

## Studio actuel dans les sources

Lynx retenu, typographie Barlow, icônes Phosphor Duotone et interface porcelaine/graphite/vermillon. Le volet **Outils** donne accès aux modèles, à l’apprentissage, aux imports et aux exports ; la console regroupe les commandes de l’éditeur. [Guide du studio](docs/STUDIO_UI_2026_09.md) · [Revue visuelle](design-qa.md) · [Preuves de la refonte](docs/STUDIO_UI_EVIDENCE.json).

La release `v0.0.1` publiée conserve son interface et ses APK d’origine. La nouvelle interface est compilée et testée dans les sources actuelles ; elle n’a pas remplacé les fichiers de cette release.

Un nouvel **AAB ARM64 signé** du studio est produit localement, avec son APK dérivée et les tests associés. [Compilation, empreintes et portée de la recette](docs/APP_BUNDLE.md).

## Version publique 0.0.1

Studio simplifié, didacticiel interactif au premier lancement, imports et exports sécurisés, icônes Cadryl et notices hors ligne. Modèles et apprentissage restent disponibles. La case finale du didacticiel désactive ses prochains lancements ; les réglages permettent de le relancer.

Les sources Apache-2.0 se compilent sans publicité, abonnement ni backend Cadryl. [Contenu de la version](docs/RELEASE_001.md) · [Preuves exactes](docs/RELEASE_001_EVIDENCE.json) · [Nettoyage des données personnelles](docs/PRIVACY_REDACTION_2026_09.md).

## Ce qu’on peut faire

- **Préparer ses images.** Dossier local ou Hugging Face, travail par petits lots, suivi des doublons et reprise du projet.
- **Annoter avec une aide.** Boîtes, points, masques : le modèle propose, tu ajustes et tu décides. Tes corrections enregistrées restent protégées.
- **Sortir un dataset utilisable.** JSONL complet, puis COCO, YOLO, WebDataset ou vision-langage selon le travail réalisé. La copie doit être relue avant le nettoyage.
- **Faire progresser une copie du modèle.** Le premier entraînement crée une version séparée. Les suivants reprennent ses derniers poids validés. L’original reste disponible.

L’apprentissage est facultatif et désactivé au départ. **Aucun poids de modèle ni dataset n’est livré dans l’APK.** Le parcours manuel fonctionne sans modèle.

## Dans l’app

<img src="docs/studio/native-editor.png" alt="Studio natif Cadryl avec le lynx retenu" width="300">

Capture native réelle sur émulateur API 36, APK Debug de la refonte, image synthétique autorisée et annotation enregistrée. [Liaison à l’APK](docs/studio/capture.json) · [Visuels et provenance](docs/VISUALS.md).

## Essayer Cadryl

Il faut **Android 9 ou plus et un téléphone ARM64**. Télécharge `vision-dataset-studio.apk` dans la release, puis commence avec quelques images dont tu peux disposer. Le nom technique du fichier et l’identifiant Android restent les mêmes pour garder la continuité du projet.

**Tu as déjà rc4 ou rc5 ?** Ces anciennes APK Debug utilisent d’autres clés. 0.0.1 ne peut pas les mettre à jour directement : garde l’installation et ses données. [Installation et signature](docs/GETTING_STARTED.md#installer-cadryl).

Pour les modèles, deux catalogues publics sont documentés : [les conversions Charlbi](https://huggingface.co/Charlbi/Lite_rt_prepared_for_android_dataset_builder) et [les modèles FireViewer](https://huggingface.co/fireviewer/litert-models). Lis les résultats de chaque variante avant de la choisir. Un modèle qui se charge n’est pas forcément précis sur tes images.

## Validation

Builds Android réels, contrôles JVM/lint/KSP, suites Android et parcours UI indépendants sur téléphone ont leurs reçus distincts. [Résultats actuels](TEST_REPORT.md). Les campagnes RC8 de services externes, corpus et endurance conservent leur portée historique. [Limites connues](KNOWN_LIMITATIONS.md).

## Mettre les mains dans le code

L’app utilise **Kotlin, Compose, Room et LiteRT**. Le [guide de développement](docs/DEVELOPMENT_RESUME.md) explique les dépendances natives, le build Windows/Linux et les tests. L’[architecture](docs/ARCHITECTURE.md) donne les repères pour trouver le bon endroit dans le code.

Le [studio actuel](docs/STUDIO_UI_2026_09.md) rassemble les outils après rc8.
Les [preuves 0.0.1](docs/RELEASE_001_EVIDENCE.json) décrivent les APK livrées ;
la validation humaine finale de l’interface reste distincte.

Un bug, une idée ou une amélioration ? [Ouvre une issue](https://github.com/unicornwhodev/cadryl_android_dataset_and_litert_train/issues) avec la version, l’appareil et les étapes pour reproduire. Les contributions sont les bienvenues ; les règles utiles tiennent dans [CONTRIBUTING.md](CONTRIBUTING.md).

Le code est sous [Apache-2.0](LICENSE). Les modèles, datasets et composants tiers gardent leurs propres conditions. [Licences et attributions](LICENSING_STATUS.md).

## Compilation locale

Aucun workflow GitHub Actions : retrait demandé par le propriétaire.
Les builds, tests et signatures sont exécutés localement avec les scripts du dépôt.
