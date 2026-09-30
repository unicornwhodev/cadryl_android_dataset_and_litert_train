# Ce qui reste à améliorer

[Le projet](README.md) · [English](docs/en/KNOWN_LIMITATIONS.md)

Cadryl **0.0.1** est la release publique. Les résultats RC8 ci-dessous sont historiques. Le [rapport du 30 septembre](docs/RC8_QUALIFICATION_2026_09.md) lie chaque contrôle aux APK et appareils réellement testés.

## Appareils et stabilité

Les 53 tests Release passent sur Oppo ARM64/API 33/4 Ko, Honor ARM64/API 36/4 Ko et AVD x86/API 36/16 Ko. Quatre parcours UI indépendants passent sur Oppo et AVD. **L’ARM physique 16 Ko reste ouvert** : les deux téléphones connectés utilisent des pages de 4 Ko. L’alignement natif et l’émulateur ne remplacent pas ce contrôle.

Dix minutes d’inférence CPU RepViT M1 sur photo réelle passent sur le Honor avec l’APK publiée : 11 094 inférences, médiane 53,50 ms/P95 54,14 ms, PSS échantillonné 80 321–158 855 KiB. Cela ne qualifie pas d’autres modèles, la précision ou l’endurance en arrière-plan. Les restrictions constructeur, redémarrages et longues sessions d’entraînement restent à tester. La recette des 1 000 photos a nécessité une remise au premier plan sur Oppo.

Les anciennes traces de crashes ART/API 35 sont conservées sans cause profonde confirmée. Aucun crash ART n’est relevé avant/après les suites finales API 36. Les tests interrompus ne deviennent pas des succès.

## Modèles et qualité

Les résultats du catalogue sont partiels et doivent être repris avec le candidat actuel. Les six variantes FireViewer restent à tester sur image réelle avec rc8. La campagne publique Charlbi du 22 septembre compte **13 variantes réussies sur 25**, dont huit entraînables, un délai dépassé et onze non exécutées. Ce sont des essais d’exécution, pas une mesure de précision.

Les conversions entraînables fournies ajustent des têtes ou adaptations de sortie avec un encodeur figé. Le test synthétique des couches internes est une autre preuve. Il manque encore des mesures sur un corpus indépendant : précision, erreurs, oubli après entraînement, RAM et latence sur ARM.

Un bundle incomplet ne peut pas être remplacé par un simple fichier `.tflite`. Certains modèles demandent plusieurs graphes et des fichiers de prétraitement ou de tokenisation.

La vérification des classes utilise le contrat actif et son vocabulaire déclaré. Elle ne mesure pas la précision et ne peut pas valider des labels inconnus dans un fichier brut. Les sorties libres demandent toujours un essai sur image.

## Données et échanges

rc7 améliore les diagnostics HTTP, les réglages et la récupération locale d’un paquet HF bloqué. Cette version ne résout pas un refus 403 dû aux permissions du compte, du jeton ou du dépôt. Les anciennes erreurs de dimensions signalées sur téléphone n’ont pas été reproduites avec les reçus récents disponibles. [Constat et portée](docs/UX_CONFIGURATION_2026_09.md).

rc8 passe les échanges HF réels dans un nouveau dépôt privé explicitement autorisé : publication/readback, conflit de parent et réconciliation après réponse perdue. Ces essais utilisent Debug/AVD avec les mêmes fichiers métier que le candidat final ; ils ne prouvent pas une publication depuis Release sur téléphone. La révocation SAF réelle passe sur Oppo en Debug, et la perte du volume amovible dédié passe sur AVD. Le quota, la coupure réseau et la purge interrompue de la campagne antérieure gardent leur portée.

L’ingestion native de 1 000 photos publiques CC0 passe sur Oppo, conformément au seuil réduit par le propriétaire. La qualité des modèles et le parcours Viewer HF réel complet/partiel restent ouverts. Tous les fournisseurs de stockage, gros transferts multipart, serveurs d’agent et intégrations cloud ne sont pas couverts.

Le registre anti-doublons couvre les fichiers ou pixels identiques dans un même projet. Il ne garantit pas la détection de toutes les images recadrées ou recompressées. **Un export dataset n’est pas une sauvegarde complète du projet.** Les migrations Room gardent l’identité de l’app ; elles ne récupèrent pas les données d’une autre application ni d’une installation désinstallée.

## Distribution

Le dépôt public contient uniquement l’édition sans publicité ni abonnement.
Les composants commerciaux sont hors de cet arbre. Les renseignements
légaux fournis ne constituent pas une attestation de conformité.
La revue intégrale UI/accessibilité/parcours d’erreur et la validation humaine
sont des conditions de la future RC finale ; des captures seules ne les clôturent pas.

rc8 utilise la clé durable. Les anciennes rc4/rc5 Debug portent d’autres certificats et ne peuvent pas être mises à jour directement. Garde leurs données. La clé actuelle et sa copie ont été vérifiées sur deux disques du même PC ; une sauvegarde hors machine reste à faire.

Les workflows GitHub Actions ont été retirés à la demande du propriétaire. La recette reste locale. Les inventaires actuels comptent 103 artefacts du runtime public ; la revue des notices natives transitives reste ouverte. [Signature](docs/SIGNING.md) · [Licences](LICENSING_STATUS.md) · [Priorités](docs/ROADMAP.md).

La préparation suivante vérifie l’absence des SDK AdMob/UMP/Billing dans les
APK publics. La revue des captures à 200 % relève des
textes et libellés coupés, notamment en paysage : l’UI reste à reprendre avant
RC final0.1.

La publication 0.0.1 suit la décision du propriétaire ; les contrôles matériels non exécutés ne sont pas déclarés réussis. [Preuves actuelles](docs/RELEASE_001_EVIDENCE.json).
