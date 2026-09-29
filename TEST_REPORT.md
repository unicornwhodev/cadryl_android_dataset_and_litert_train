# Les tests de Cadryl

[Le projet](README.md) · [English](docs/en/VALIDATION.md)

Ce rapport distingue le candidat actuel des campagnes précédentes. Pour chaque résultat, les reçus gardent le build, les APK et l’appareil concernés.

## Préversion actuelle : 4.2.0-rc8

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

Les rapports datés peuvent utiliser l’ancien nom de l’app. Leurs reçus et empreintes restent inchangés. Aucun succès antérieur n’est transféré à rc7 sans nouveau passage.

## Ce qu’il manque encore

ARM physique 16 Ko, essais plus longs, modèles FireViewer et image réelle, sources HF complète/partielle, échanges HF autorisés, mesures de qualité et CI distante. La Release rc8 sur Honor et sa continuité sont exécutées ; la cause profonde des crashes ART reste non confirmée. [Limites](KNOWN_LIMITATIONS.md) · [Recette Android](docs/ANDROID_QUALIFICATION.md) · [Tester la Release](docs/RELEASE_TESTING.md).
