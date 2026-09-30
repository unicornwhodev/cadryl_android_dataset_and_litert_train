# Les visuels de Cadryl

[Documentation](README.md) · [Identité visuelle](BRAND.md)

Les images décoratives et les captures de l’application sont identifiées séparément. Les captures Android restent intactes ; elles montrent le logiciel tel qu’il a tourné.

## Identité actuelle dans les sources

Le propriétaire a fourni et retenu le lynx orange. Le [PNG original](../app/src/main/res/drawable-nodpi/ic_cadryl.png) est conservé sans redessin. Barlow et Phosphor Duotone sont livrés localement avec leurs licences et leurs empreintes ; [BRAND.md](BRAND.md) décrit les sources épinglées.

Les captures de la refonte décrivent une véritable interface Compose avec un projet de recette et une image synthétique autorisée. Le [rapport visuel](../design-qa.md) précise les états, dimensions, différences et corrections. Une image de référence est une maquette ; elle ne constitue pas une preuve d’exécution Android. Les éventuels recadrages documentaires enlèvent uniquement les barres système et gardent un reçu distinct du PNG brut.

Les anciens tags publiés conservent leur APK, leur apparence et leurs preuves. Le studio actuel est décrit dans [le guide de la refonte](STUDIO_UI_2026_09.md).

## Identité historique rc6

La [bannière Cadryl](brand/cadryl-hero.png) a été générée avec l’outil intégré Image Gen le 24 septembre 2026. Elle donne la direction graphique : cadre ouvert cyan, angle lilas, fond encre et nom court. Le [prompt](brand/imagegen-prompt.txt) et le [reçu de provenance](brand/provenance.json) accompagnent le fichier.

Le [symbole SVG](brand/cadryl-mark.svg), le [logo](brand/cadryl-lockup.svg) et les VectorDrawable Android ont ensuite été dessinés pour garder une géométrie nette à petite taille. La bannière est une illustration de marque, pas une capture, une image de dataset ou un résultat d’inférence.

## Captures historiques de l’application

Les captures [accueil rc7](../test-results/rc7-release/visuals/home.png) et [Images rc7](../test-results/rc7-release/visuals/images.png) ont été prises le 27 septembre sur la Release x86_64 signée, en français, puis inspectées sans retouche. Elles utilisent un projet de démonstration vide sur l’émulateur API 36/16 Ko. Le [reçu](../test-results/rc7-release/visuals/binding.json) relie l’APK installée et les fichiers à leurs empreintes. Les captures rc6 ci-dessous restent historiques.

| Visuel | Ce qu’il montre |
|---|---|
| [Accueil rc6](../test-results/rc6-release/arm/home.png) | APK Cadryl Release ARM64 signée, émulateur API 36/16 Ko, ARM traduit |
| [Démarrage rc6](../test-results/rc6-release/visuals/splash.png) | Image extraite de l’enregistrement du lancement Android, sans retouche |
| [Icône et nom Android](../test-results/rc6-release/visuals/app-info.png) | Fiche système de l’application installée |
| [Éditeur historique](ui-refined/refined-editor-persisted.png) | Image synthétique et annotation conservée, build du 19 septembre, avant Cadryl |

Les reçus de [rc6](../test-results/rc6-release/README.md) lient les nouvelles captures à l’APK testée. Elles viennent d’un émulateur, pas d’un téléphone ARM. La capture historique de l’éditeur garde son ancien nom et sa [recette d’origine](UI_REFINEMENT.md).

## Schémas et graphiques

Les diagrammes `visuals/*.dot` décrivent les composants et le cycle du lot. Ils se régénèrent avec Graphviz. Le schéma [des versions de modèle](visuals/current/model-lineage.svg) décrit le comportement ; ce n’est pas une mesure.

Les graphiques datés conservent leurs données d’origine. `visuals/current/qualification-20260922.*` représente la campagne Charlbi du 22 septembre : 13/25 variantes réussies, un délai dépassé et onze non exécutées, dont huit succès entraînables. Les anciens graphiques `visuals/benchmarks/` utilisent les [reçus listés ici](visuals/android-evidence.json). Les durées de tests ne sont pas des latences d’inférence et les pertes synthétiques ne mesurent pas la précision métier.

Les scripts reproductibles sont dans [tools/docs](../tools/docs/). L’[ancienne bannière](visuals/banner.png) garde son [prompt](visuals/banner-prompt.txt) pour les publications précédentes.

## English

Current sources use the exact owner-supplied lynx, bundled Barlow fonts and licensed Phosphor Duotone icons. Native redesign captures and their state differences are described in design-qa.md. The older generated banner, SVG marks, RC6/RC7 screenshots and dated charts are historical and retain their original APK bindings. No artwork or screenshot establishes model accuracy or final human UI acceptance.
