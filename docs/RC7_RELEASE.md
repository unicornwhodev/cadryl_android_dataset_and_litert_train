# Cadryl 4.2.0-rc7

27 septembre 2026 · préversion · Android 9+ ARM64 · code 12.

[Téléchargements](https://github.com/unicornwhodev/vision-dataset-studio/releases/tag/v4.2.0-rc7) · [Premiers pas](GETTING_STARTED.md) · [Preuves](../test-results/rc7-release/README.md)

## Ce qui change

- La configuration suit **Images → Objectif → Vérifier**. L’annotation manuelle est accessible sans modèle. Les options avancées sont repliées et les écrans associés sont regroupés.
- Les classes du modèle se recherchent et se sélectionnent. La compatibilité tient compte des tâches et du type de vocabulaire ; les classes manquantes sont expliquées avant la préannotation.
- Les dimensions, axes et types d’entrée sont vérifiés contre le graphe LiteRT réel. Le vocabulaire des fichiers bruts et la précision restent à vérifier par l’utilisateur.
- L’inspection accepte davantage de colonnes image valides. Les erreurs HF proposent une action adaptée. Les classes nécessaires à COCO ou YOLO sont vérifiées avant export.
- Un paquet HF bloqué peut être récupéré en copie locale lorsque ses fichiers et son reçu sont intacts. Les annotations humaines, le parent HF et les conditions de nettoyage restent protégés.

[Description complète](UX_CONFIGURATION_2026_09.md).

## Validation de ces APK

| Vérification | Résultat |
|---|---|
| Tests JVM / Python | 84/84 / 87/87, aucun échec ni test ignoré |
| Lint Android | 0 erreur, 124 avertissements |
| Release signée ARM64 | 45/45 métier et 5/5 UI sur émulateur API 36, pages 16 Ko, ARM traduit |
| Release signée x86_64 | 45/45 métier et 5/5 UI sur le même émulateur |
| Alignement natif | 4/4 bibliothèques par APK, audit strict ELF et ZIP réussi |
| Original et copie entraînée | Original intact, Save/Restore et poursuite de l’apprentissage vérifiés |

Les tests utilisent les octets signés publiés. Les sources compilées sont comparées au commit Git par le packageur. Les reçus initiaux en échec sont conservés avec la cause et les nouveaux passages. Le pilote UI indépendant couvre aussi le nouveau guide sans requête HF.

## Installation et packages

Installe `vision-dataset-studio.apk` et vérifie son empreinte dans `SHA256SUMS`. La même clé durable permet une mise à jour depuis rc6. Les anciennes rc4/rc5 Debug ont une autre signature : conserver leurs données et leur installation. Aucun modèle ni corpus n’est embarqué.

Le ZIP de qualification contient les paires app/tests ARM64 et x86_64, le pilote UI, la documentation et les preuves. Les trois ZIP Maven fournissent Flex, Graphics Path et LiteRT. Le package GHCR reste privé et contient des artefacts ; il n’est pas exécutable.

## Limites

L’ARM64 a été exécuté par traduction sur un émulateur x86_64 API 36/16 Ko. La recette complète de rc7 sur téléphone, ARM physique 16 Ko, les essais prolongés, la qualité des modèles et la CI distante restent à faire. Les droits HF ne peuvent pas être accordés par l’app. La cause ART historique et la revue des notices natives restent ouvertes. [Limites connues](../KNOWN_LIMITATIONS.md).

## English summary

rc7 adds three-step setup, grouped navigation, searchable compatible classes, checks against real LiteRT input dimensions and verified local recovery of pending HF exports. Existing human annotations and transfer safeguards remain protected.

Both durable-signed, minified Release APKs pass 45 core tests and five independent UI scenarios on the API 36 / 16 KB emulator. JVM: 84/84; Python: 87/87; lint: zero errors, 124 warnings; strict native alignment: four libraries per APK. ARM64 uses translation, so this remains a prerelease with phone, physical ARM 16 KB, model quality and remote CI work pending.
