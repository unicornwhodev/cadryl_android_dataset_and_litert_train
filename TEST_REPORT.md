# Les tests de Cadryl

## Bundle signé du studio — 30 septembre 2026

118 JVM et 114 Python réussis, lint zéro erreur / 88 avertissements, 101 KSP.
La nouvelle APK Debug passe 63/63 sur l’émulateur API 36. Les recettes HF
privée réelle (publication, conflit, réponse perdue) et SAF réelle (copie,
révocation, refus de purge) passent sur cette APK. L’AAB ARM64 signé, son APK
dérivée et l’APK instrumentée sont produits et vérifiés. La recette physique
de ces artefacts attend le téléphone ; les captures et résultats physiques
ci-dessous concernent leurs candidats d’origine.

[Recette et limites](docs/APP_BUNDLE.md) · [Artefacts exacts](docs/APP_BUNDLE_EVIDENCE.json).

[Le projet](README.md) · [English](docs/en/VALIDATION.md)

Ce rapport distingue le candidat actuel des campagnes précédentes. Pour chaque résultat, les reçus gardent le build, les APK et l’appareil concernés.

## Studio avec lynx — sources du 30 septembre 2026

Refonte porcelaine/graphite/vermillon, lynx original, Barlow et Phosphor Duotone. Les outils d’annotation, modèles, apprentissage et exports sont conservés. La reprise d’un lot sauvegardé initialise désormais le résultat de benchmark avant les coroutines du constructeur ; un nouveau test exerce huit reprises sur Room.

Builds réels : 118 JVM réussis, 114 Python réussis, lint 0 erreur / 88 avertissements, 101 fichiers KSP. Les mêmes APK Debug passent 63/63 sur l’émulateur dédié ; la Release ARM64 minifiée et signée passe 63/63 sur Oppo. Le pilote indépendant passe 5/5. Les deux matrices physiques conservent 288 captures. Les tables installées sont identiques avant/après remplacement ; aucun contenu utilisateur n’est exporté.

Les premiers essais ont retenu un défaut de démarrage et des accès retirés par R8 dans le nouveau test ; ils ne sont pas qualifiés. Les règles de conservation ciblent les méthodes utilisées par la recette, sans désactiver l’optimisation. Le fournisseur de preuves résout l’UID du vrai targetPackage depuis le manifeste de l’APK de test.

[APK et reçus exacts](docs/STUDIO_UI_EVIDENCE.json) · [Revue visuelle native](design-qa.md). Ces preuves concernent la refonte dans les sources ; les anciennes releases publiées gardent leurs APK et leurs reçus. L’acceptation humaine finale reste ouverte.

Les builds sont ensuite reproduits avec les fichiers LF canoniques du dépôt.
Les APK applicative et de test restent identiques octet par octet aux APK
qualifiées, en Debug et en Release. Les 327 entrées compilées correspondent aux
blobs Git préparés pour le commit. [Manifeste compilé](docs/studio/source-manifest.json).

## Release publique 0.0.1 — 30 septembre 2026

117 tests JVM, 113 tests Python, lint zéro erreur / 83 avertissements, 101 fichiers KSP. Les nouvelles APK passent 62/62 sur émulateur en Debug et sur téléphone ARM64 en Release minifiée signée. Le pilote UI indépendant passe 4/4 ; les matrices portrait/paysage produisent 288 captures. Toutes les tables installées sont conservées lors de la mise à jour, sans exporter leur contenu. Les SDK commerciaux sont absents des DEX et manifestes publics.

Le premier essai Release a relevé un défaut du nouveau test d’icônes (61/62). Le test a été corrigé pour lire les ressources installées sans référencer une classe R supprimée par R8 ; la suite complète repasse 62/62. Cet échec est conservé en privé et ne reçoit aucune qualification.

[APK et reçus exacts](docs/RELEASE_001_EVIDENCE.json) · [Anonymisation](docs/PRIVACY_REDACTION_2026_09.md). Les sections suivantes sont historiques. Les services externes et essais prolongés RC8 ne sont pas présentés comme réexécutés sur 0.0.1. La revue humaine de l’interface reste séparée.

## Édition publique et didacticiel — 30 septembre 2026

Le dépôt ne contient plus l’implémentation commerciale. Les tests et fichiers
correspondants sont conservés hors de cet arbre avec leurs empreintes. Les
fonctions métier, modèles, apprentissage et exports sont conservés.

Les APK réelles Debug et Release ARM64 minifiée sont construites. Les 117 tests
JVM publics passent, sans suppression d’un test public pour obtenir ce résultat ;
les cinq tests de politique commerciale sont sortis avec leur implémentation.
Lint : zéro erreur, 85 avertissements ; Room/KSP : 101 fichiers générés.
Les 104 tests Python passent. Les DEX et manifestes des deux APK ne contiennent
pas les SDK publicitaires/facturation ni leurs permissions.

Les suites Android passent 61/61 sur AVD API 36/x86_64/pages 16 Ko et sur Oppo
API 33/ARM64/pages 4 Ko avec Release signée. Trois tests utilisent l’activité
réelle pour vérifier le didacticiel : case cochée, case non cochée, prochain
lancement, relance dans les réglages et report limité à la session.
Les préférences initiales sont restaurées ; aucune base utilisateur n’est effacée.

Les matrices capturent 144 cas par orientation et par dispositif (576 au total).
Le pilote indépendant passe 4/4 sur Oppo. Toutes les tables installées sont
identiques avant/après mise à jour ; les 1 000 images sources publiques restent
identiques au relevé initial. Ces résultats ne certifient pas tous les états UI.

[Reçus et APK exactes](docs/PUBLIC_TUTORIAL_EVIDENCE.json) ·
[Comportement du guide](docs/FIRST_LAUNCH_TUTORIAL.md).
La release rc8 reste inchangée ; ARM physique 16 Ko et acceptation UI finale
restent ouverts. Les campagnes suivantes sont historiques, avant cette séparation.

## Interface simplifiée après rc8 — 30 septembre 2026

Une action principale sur l’accueil, moins d’onglets redondants, aides et détails
dans la page, options du modèle repliables sans perte de saisie. Les 16 routes,
modèles, import, inférence, apprentissage et export restent disponibles.
[Changements UI](docs/UI_REWORK_2026_09.md).

Le build source sans pub ni abonnement passe 122 tests JVM, lint 0 erreur /
86 avertissements et 101 fichiers KSP. Les outils passent 104 tests Python ;
le serveur local passe 10 tests synthétiques. Le build optionnel avec les SDK
de test est aussi compilé (122 JVM, lint 0 erreur / 87 avertissements) ; annonces
et achats réels ne sont pas qualifiés par ce build.

Les suites Android passent 58/58 sur l’AVD x86_64 / API 36 / pages 16 Ko et
58/58 sur l’Oppo ARM64 / API 33 / pages 4 Ko avec la véritable Release minifiée
signée. Les nouvelles assertions vérifient l’accès à l’apprentissage et la
conservation d’une saisie non enregistrée quand le panneau du modèle est replié.
La comparaison logique des tables avant/après installation sur Oppo est identique.
Les matrices capturent 144 cas par orientation sur chacun des deux dispositifs
(576 captures au total). Le pilote UI indépendant passe 4/4 sur l’Oppo ; les
1 000 empreintes des images sources publiques correspondent au relevé initial.
Les échecs intermédiaires restent conservés et ne reçoivent aucune qualification.

Les APK, reçus et campagnes exactes sont liés dans [les preuves](docs/FINAL01_EVIDENCE.json).
La future RC finale et l’acceptation humaine de son UI restent ouvertes.

## Stabilisation avant la reprise UI — 30 septembre 2026

Les correctifs d’import transactionnel, de lecture UTF-8 et de vérification des ZIP, le nettoyage des ressources et la préparation légale/publicitaire sont compilés réellement : 122 tests JVM réussis, lint 0 erreur/85 avertissements et 101 fichiers KSP dans le build sans monétisation. Le build optionnel de test passe aussi 122 tests JVM, lint 0 erreur/86 avertissements. Les outils passent 102 tests Python ; le serveur de validation local passe 10 tests synthétiques distincts.

La suite métier passe 56/56 sur l’AVD x86_64/API 36/pages 16 Ko en Debug et 56/56 sur l’Oppo CPH2343/API 33/pages 4 Ko en Release minifiée signée. Le pilote UI indépendant passe 4/4 sur cette Release Oppo. Les tables installées sont identiques avant/après mise à jour ; les empreintes des 1 000 images sources publiques restent identiques.

L’AVD sans monétisation capture 144 cas par orientation, soit 288 captures : 16 écrans et deux documents, FR/EN, clair/sombre, texte 100/200 %. Les textes coupés à 200 % et la couverture des autres états restent à traiter avant la validation humaine. Les captures ne constituent pas une certification UI.

Ces résultats ne modifient pas la release rc8 publiée. La revue UI intégrale, les annonces et achats réels, l’identité légale complète et la provenance native complète restent des contrôles séparés. [Travaux et portée](docs/FINAL01_STABILIZATION_2026_09.md) · [Conditions du candidat final](docs/FINAL_RC_0_1_PLAN.md).

## Préversion historique : 4.2.0-rc8

30 septembre 2026 : 106 JVM, 93 Python, lint 0 erreur/148 avertissements, 101 fichiers KSP. Suites Android 53/53 en Debug/AVD 16 Ko et Release/AVD 16 Ko, Honor et Oppo ARM64/4 Ko. UI indépendante 4/4 sur Oppo et AVD.

Les recettes supplémentaires réussissent : publication HF privée autorisée, conflit et réponse perdue ; révocation SAF réelle sur Oppo et perte de volume sur AVD ; import natif de 1 000 photos CC0 sur Oppo ; 11 094 inférences en dix minutes sur Honor. Ces essais ont chacun leur APK, dispositif et portée. [Rapport détaillé](docs/RC8_QUALIFICATION_2026_09.md) · [Preuves publiques](test-results/rc8-release/README.md).

P0 est réussi. P1 reste partiel : aucun téléphone ARM en pages de 16 Ko n’est disponible ; la qualité des modèles, le parcours Viewer réel complet/partiel, les essais en arrière-plan, la CI et les notices natives restent ouverts. Aucun émulateur n’est présenté comme un téléphone ARM 16 Ko.

## Version précédente : 4.2.0-rc6

| Vérification | Résultat |
|---|---|
| Tests JVM | 70 réussis, aucun échec ni test ignoré |
| Outils Python | 87 réussis |
| Lint Android | 0 erreur, 96 avertissements |
| Release signée ARM64 | 40/40 métier et 4/4 UI sur émulateur API 36, pages 16 Ko, ARM traduit |
| Release signée x86_64 | 40/40 métier et 4/4 UI sur le même émulateur |
| Alignement natif | 4/4 bibliothèques par APK, audit strict ELF et ZIP vert |
| Original et copie entraînée | Original intact, Save/Restore et poursuite du lot suivant vérifiés sur données synthétiques |

[Note rc6](docs/RC6_RELEASE.md) · [Preuves](test-results/rc6-release/README.md) · [Synthèse des preuves](test-results/rc6-release/summary.json) · [État lisible par les outils](QUALIFICATION_STATUS.json).

L’ARM64 tourne ici via `libndk_translation` sur un hôte x86_64. **Ces résultats ne sont pas une recette sur téléphone.** Les suites utilisent les véritables APK Release minifiées et signées ; les scénarios HF, pannes externes et catalogue restent des campagnes distinctes.

Les premiers essais de pilotes ont rencontré deux courses liées au clavier : 39/40 métier x86_64, puis 3/4 UI ARM64. Les attentes ont été corrigées et les échecs restent conservés. La campagne finale utilise les APK avec l’identité Cadryl.

## Les campagnes précédentes

| Campagne | Ce qu’elle apporte |
|---|---|
| [Correctifs natifs](docs/NATIVE_FIX_2026_09.md) | LiteRT sourcé, correction CPUinfo, audit strict et nouvel environnement ART |
| [Investigation native](docs/NATIVE_FOLLOWUP_2026_09.md) | Défaut de l’ancien AAR 1.4.2 et limites de provenance |
| [Suite Release et Honor](docs/RELEASE_CLOSURE_2026_09.md) | 40 tests métier et pilote UI sur les candidats précédents |
| [Arrière-plan](docs/RELEASE_HARDENING_2026_09.md) | Service, arrêt/reprise et douze minutes sur Honor, sur un ancien candidat |
| [Données et interruptions](docs/P1_QUALIFICATION_2026_09.md) | Pannes SAF/stockage/HF, restauration, migration et signature durable |
| [Windows et rc5](docs/WINDOWS_QUALIFICATION_2026_09.md) | Build reproductible, premier correctif Flex et conservation du modèle original |
| [Catalogue LiteRT](docs/LITERT_QUALIFICATION.md) | Résultats par variante et limites de couverture |
| [Audit fonctionnel](docs/FUNCTIONAL_AUDIT_2026_09.md) | Parcours et modèles du 22 septembre |

Les rapports datés peuvent utiliser l’ancien nom de l’app. Leurs résultats restent historiques ; les expurgations de données personnelles ont leurs propres reçus et empreintes. Aucun succès antérieur n’est transféré à rc7 sans nouveau passage.

## Ce qu’il manque encore

ARM physique 16 Ko, essais plus longs, modèles FireViewer et image réelle, sources HF complète/partielle, échanges HF autorisés, mesures de qualité et CI distante. La Release rc8 sur Honor et sa continuité sont exécutées ; la cause profonde des crashes ART reste non confirmée. [Limites](KNOWN_LIMITATIONS.md) · [Recette Android](docs/ANDROID_QUALIFICATION.md) · [Tester la Release](docs/RELEASE_TESTING.md).
