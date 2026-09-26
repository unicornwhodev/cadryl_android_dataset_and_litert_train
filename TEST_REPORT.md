# Les tests de Cadryl

[Le projet](README.md) · [English](docs/en/VALIDATION.md)

Ce rapport distingue le candidat actuel des campagnes précédentes. Pour chaque résultat, les reçus gardent le build, les APK et l’appareil concernés.

## Candidat actuel : 4.2.0-rc7

27 septembre 2026. Les véritables APK Release minifiées portent la clé durable et le code de version 12. La campagne couvre le guide en trois étapes, la compatibilité des classes, les dimensions LiteRT et la récupération d’un paquet figé. Les annotations humaines et le parent HF sont préservés.

| Vérification | Résultat |
|---|---|
| Tests JVM / Python | 84/84 / 87/87, aucun échec ni test ignoré |
| Lint Android | 0 erreur, 124 avertissements |
| Release signée ARM64 | 45/45 métier et 5/5 UI sur émulateur API 36, pages 16 Ko, ARM traduit |
| Release signée x86_64 | 45/45 métier et 5/5 UI sur le même émulateur |
| Alignement natif | 4/4 bibliothèques par APK, audit strict ELF et ZIP réussi |
| Original et copie entraînée | Original intact, Save/Restore et poursuite de l’apprentissage vérifiés |

[Note rc7](docs/RC7_RELEASE.md) · [Preuves](test-results/rc7-release/README.md) · [Synthèse](test-results/rc7-release/summary.json).

Le premier passage conserve 43/45 tests : deux repères de navigation dépendaient des noms renommés par R8. Les identifiants sont désormais explicites. Le pilote UI a aussi été adapté au doublon « Atelier » des nouvelles barres et signé avec sa clé de test existante pour conserver l’installation. Les échecs initiaux sont conservés ; les deux passages finaux complets utilisent les APK identifiées dans la synthèse.

La trace statique R8 reste partielle sur des références de framework et de test ; ses différences d’API applicatives ont été relues. Les règles sont vérifiées par les compilations et les exécutions Release réelles. Aucun crash ART n’a été observé dans les passages finaux. L’ARM est traduit par `libndk_translation` : téléphone, ARM physique 16 Ko, qualité des modèles et publication HF restent des validations distinctes.

## Évolution locale du 26 septembre : configuration guidée, classes et exports

La configuration suit trois étapes : images, objectif, vérification. La compatibilité des classes est affichée et contrôlée avant préannotation ; un choix par recherche évite de ressaisir les noms. Les dimensions LiteRT sont contrôlées avant enregistrement et un paquet HF en attente peut être récupéré en archive locale vérifiée. [Détail des changements et constat sur téléphone](docs/UX_CONFIGURATION_2026_09.md).

| Vérification de cette évolution | Résultat |
|---|---|
| Build Android réel | APK Debug et APK de tests produits, signatures et empreintes vérifiées |
| Compose / Room / Moshi / LiteRT | Compilation réussie, 101 fichiers KSP générés et schémas Room conservés dans le reçu |
| Tests JVM | 84/84, aucun échec ni test ignoré |
| Outils Python | 87/87 |
| Lint | 0 erreur, 124 avertissements |
| Android API 36, x86_64, pages 16 Ko | 45/45, aucun échec ni test ignoré ; aucun crash ART détecté |
| Revue visuelle finale | Accueil, images, objectif, choix des classes et vérification inspectés en français sur l’émulateur |

Preuves finales locales : build `dist/android/runs/20260926T214726Z-b4932f8a1ea3`, campagne `dist/ux-guided-20260926/device-core-02`, synthèse `dist/ux-guided-20260926/summary.json`. Les empreintes des APK et l’ensemble des sources compilées ont été revérifiés après les tests. Les captures natives se trouvent dans `dist/ux-guided-20260926/screenshots-fr-final` ; elles montrent uniquement un projet et un modèle synthétiques de test. L’identifiant HF de démonstration n’a été ni inspecté, ni téléchargé, ni enregistré lors de la revue visuelle.

Les tests supplémentaires couvrent les classes fixes et libres, les indices réservés, les sorties spatiales distinctes de la classification, les noms avec ponctuation, le filtrage des seules propositions automatiques et le refus avant décodage sans altérer les annotations humaines. Un test Compose parcourt le guide : classe incompatible, recherche et sélection de la classe exacte, puis enregistrement en mode manuel sans compte HF. Les modèles conservent leurs indices de classes.

Le premier passage, antérieur au guide, reste dans `dist/ux-20260926` (78 JVM, 43 Android). Sa revue native des dimensions, exports et destinations reste une preuve de ce passage, distincte des captures finales.

Les essais préliminaires restent conservés : cache Robolectric inaccessible, DNS de l’émulateur indisponible, sélecteur UI ambigu puis course avec l’ouverture du clavier. Une tentative pendant laquelle un test a changé a été refusée par le contrôle de liaison aux sources. Les passages finaux n’utilisent pas ces tentatives comme preuves de succès.

La Release présente sur le téléphone reste inchangée. Cette campagne Debug sur émulateur ne qualifie pas une mise à jour Release sur téléphone, la précision des modèles sur un corpus réel ou une publication HF réelle. Les résultats rc6 ci-dessous restent distincts.

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

Les rapports datés peuvent utiliser l’ancien nom de l’app. Leurs reçus et empreintes restent inchangés. Aucun succès antérieur n’est transféré à rc7 sans nouveau passage.

## Ce qu’il manque encore

Recette physique du candidat sur Honor et ARM 16 Ko, essais plus longs, catalogue sous le nouveau runtime, mesures de qualité et CI distante. La cause du crash ART historique reste non confirmée. [Limites](KNOWN_LIMITATIONS.md) · [Recette Android](docs/ANDROID_QUALIFICATION.md) · [Tester la Release](docs/RELEASE_TESTING.md).
