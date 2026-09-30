# La suite pour Cadryl

[Documentation](README.md) · [English](en/ROADMAP.md)

La priorité est de rendre le parcours actuel fiable sur téléphone. Les nouvelles fonctions viendront après les retours sur cette base.

## Suite de la release 0.0.1

- Conserver la qualification des APK exactes et les contrôles de préservation lors des prochaines mises à jour.
- Faire accepter l’interface finale, après les parcours erreurs, clavier, accessibilité et reprise sur appareil.
- Compléter la couverture matérielle lorsque les appareils correspondants sont disponibles, sans bloquer cette livraison.
- Refaire les essais longs et observer les restrictions d’arrière-plan. Conserver les traces si ART replante ; la cause historique reste à expliquer.

## Ensuite : données et reproductibilité

Compléter les interruptions et les gros transferts avec des destinations de test autorisées. Vérifier une ancienne base issue d’un usage réel, avec une copie de sauvegarde. Reproduire le build sur un autre poste avec les scripts locaux.

## Puis : mesurer les modèles

Reprendre le catalogue avec le nouveau runtime, terminer les variantes manquantes et les bundles, puis mesurer la qualité sur un corpus indépendant. La campagne Charlbi compte aujourd’hui 13/25 réussites, un délai dépassé et onze variantes non exécutées ; les huit entraînements réussis ne prouvent pas un gain de précision.

## Pour une distribution durable

Garder la même clé, ajouter une sauvegarde hors machine, terminer la revue des notices natives et suivre la taille de l’APK. Les modèles restent téléchargeables séparément. Les [limites actuelles](../KNOWN_LIMITATIONS.md) et les [résultats](../TEST_REPORT.md) servent de repères avant chaque release.
