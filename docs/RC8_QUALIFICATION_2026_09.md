# Cadryl 4.2.0-rc8 — qualification du 30 septembre 2026

**Préversion : P0 réussi et P1 partiellement qualifié.** Les correctifs de pagination, de restauration initiale et de frontière R8 sont compilés et exécutés. Les preuves publiques sont dans [rc8-release](../test-results/rc8-release/README.md), avec manifeste SHA-256. La release et son tag désignent le commit dont le paquet vérifie les octets compilés ; le commit de base inscrit dans chaque reçu de build reste intact.

Identité `com.unicornwhodev.visiondatasetstudio`, version `4.2.0-rc8`, code 13, Room 4. Certificat durable : `51ef3e4abf953c62c8427deaecf4349aefffc2270c593229315722457f126895`.

## Résultats exécutés

| Contrôle | Résultat et portée | Preuve |
|---|---|---|
| Build Android réel | Debug + tests, deux Release minifiées + tests ; 101 fichiers KSP | [Build Debug](../test-results/rc8-release/debug/build/status.json), [ARM](../test-results/rc8-release/arm/build/status.json), [x86](../test-results/rc8-release/x86/build/status.json) |
| JVM / Python / lint | 106/106 ; 93/93 ; 0 erreur et 148 avertissements | [Hôte](../test-results/rc8-release/host-checks.json) |
| Debug AVD API 36/16 Ko | 53/53 ; original du modèle conservé et entraînement repris | [Debug](../test-results/rc8-release/debug/core/status.json) |
| Release ARM physique | 53/53 sur Oppo CPH2343, API 33, pages 4 Ko ; même APK 53/53 sur Honor API 36/4 Ko avant sa libération | [Oppo](../test-results/rc8-release/arm/core/status.json) |
| Release x86 native | 53/53 sur AVD API 36/16 Ko ; copie QA avec certificat de l’installation existante et payload identique | [x86](../test-results/rc8-release/x86/core/status.json) |
| UI indépendante | 4/4 sur Oppo et 4/4 sur AVD ; XML frais pour chaque action ; préférence restaurée | [Oppo UI](../test-results/rc8-release/arm/ui/status.json), [AVD UI](../test-results/rc8-release/x86/ui/status.json) |
| Alignement ELF / ZIP | Les quatre bibliothèques de chaque APK passent le contrôle strict 16 Ko | [ARM](../test-results/rc8-release/arm/strict-alignment.json), [x86](../test-results/rc8-release/x86/strict-alignment.json) |
| HF réel autorisé | Nouveau dépôt privé de recette, publication/readback, un gagnant pour deux réservations concurrentes, reprise sans seconde publication | [Publication](../test-results/rc8-release/extended/hf-live.json) |
| Réponse HF perdue | Processus réellement tué après acceptation serveur ; réconciliation et relecture sans commit dupliqué | [Réponse perdue](../test-results/rc8-release/extended/hf-lost-response.json) |
| Conflit HF réel | Ancien parent refusé par Hub ; aucun changement silencieux du parent ; candidat local conservé | [Conflit](../test-results/rc8-release/extended/hf-conflict.json) |
| SAF réel sur Oppo | Copie relue, droit persistant réellement révoqué, redémarrage du processus, purge refusée ; annotation/image/archive privées conservées | [SAF](../test-results/rc8-release/extended/saf-revocation.json) |
| Volume perdu | Volume amovible dédié de l’AVD démonté après copie relue ; purge refusée, données conservées ; volume remonté | [Volume](../test-results/rc8-release/extended/volume-loss.json) |
| Corpus libre 1 000 images | Oppo : vrai DocumentsUI, index local, dix lots, 1 000 copies vérifiées, dimensions décodées, aucun doublon pixel, curseur 1 000 relu après réouverture de Room | [Corpus](../test-results/rc8-release/extended/corpus-1000.json) |
| Inférence ARM prolongée | Honor : 600 113 ms, 11 094 inférences RepViT M1 sur photo publique, CPU 2 threads ; médiane 53,50 ms, P95 54,14 ms | [Inférence](../test-results/rc8-release/extended/prolonged-inference.json) |

La Release ARM publiée a le SHA-256 `f4f87ce602b71de297b3fd91d4dbfcb0707744b51c06c743e4878e6793b441e1` ; la Release x86 publiée `e5b9e18c4887e95c3d664be90c2b478ad11da7c1b2396abd0874e95c42295163`. Les tests Release durables ont le SHA `a1358970a9544e047834be9d66241dc86218d610484b36200f64566e81067b61`. Les reçus de signature lient ces octets aux builds. La copie x86 QA porte le SHA `538d0b29e08bd17afd04fcfdfe31ff4c9c24d819057323e90c5ed56567fde7f8` ; seule sa signature diffère de l’APK de distribution.

## Portée des essais supplémentaires

Les échanges HF réels sont exécutés avec les services Android de production sur Debug/AVD 16 Ko, dans le seul nouveau dépôt privé explicitement autorisé. Son identifiant et les credentials restent hors des preuves publiques. Les fichiers de production `app/src/main` sont identiques entre ce build et le build final : [liaison vérifiée](../test-results/rc8-release/extended/hf-source-binding.json). Cela ne constitue pas une publication HF depuis la Release minifiée sur téléphone. La phase volontairement tuée après acceptation serveur n’est jamais comptée comme un test terminé ; la réconciliation et le conflit réussissent séparément.

Les essais SAF et corpus physiques utilisent le pont DocumentsUI absent de la Release, dans une APK Debug signée avec la clé durable. La Release remplace ensuite cette APK sans désinstallation ni réinitialisation. Les données de recette restent conservées. Le quota tmpfs réel, la coupure réseau par règles limitées à l’UID et la purge interrompue/reprise de la première campagne RC8 restent des résultats distincts ; ils ne sont pas renommés en essais du binaire final.

Le propriétaire a réduit explicitement le seuil de corpus de 10 000 à **1 000** le 30 septembre. Le corpus public [KoalaAI/StockImages-CC0](https://huggingface.co/datasets/KoalaAI/StockImages-CC0) est figé à `206f3575579f1187548c6f47042ae9174c0a51fc` ; sa fiche déclare CC0-1.0. Deux images non décodables ont été exclues lors de la préparation hôte ; 1 000 fichiers décodables, soit 205 731 482 octets, sont effectivement importés sur Android. Les fichiers source restent inchangés. Cette recette couvre l’ingestion, l’index et le curseur ; elle n’évalue pas 1 000 annotations humaines, la précision des modèles ou tous les formats d’export. Le temps de 600 370 ms inclut un blocage en arrière-plan puis la remise au premier plan : ce n’est pas un benchmark de débit.

Le benchmark de dix minutes utilise exactement l’APK ARM publiée et une photo publique, avec cinq inférences de chauffe. Le PSS échantillonné varie de 80 321 à 158 855 KiB ; aucun pic absolu ni absence générale de fuite mémoire n’est revendiqué. Les inputs/SHA restent intacts. L’application est gardée visible, sans exemption de gel pour ce benchmark. Il ne qualifie ni la précision, ni les six variantes FireViewer, ni l’endurance en arrière-plan.

Les suites de base gardent l’app visible sur les téléphones ; sur Honor une exemption temporaire a été utilisée pour les processus de recette. Le contrôle UI indépendant n’utilise aucune instrumentation ni exemption. Aucun crash ART n’est relevé avant/après les suites finales. Les anciennes traces ART restent sans cause profonde démontrée.

## Correctifs et échecs conservés

- Les réponses Viewer absentes/nulles/sans `rows`, pages trop longues, indices négatifs/dupliqués et annotations tronquées sont refusés. Le curseur compte les lignes réellement parcourues ; les annotations humaines restent protégées.
- L’éditeur et les opérations projet attendent la restauration initiale : la course de lot reproduite lors du passage 52/53 est corrigée. Huit tests de pagination et cinq tests du parseur HTTP sont ajoutés sans suppression des tests précédents.
- R8 conserve les signatures précises exercées par les tests, dont l’accès au catalogue FireViewer pour le benchmark. La minification reste active.
- Les premiers passages ART/API 35, un accesseur R8 manquant, les corpus AVD interrompus et les pilotes UI incomplets restent des échecs. Le premier conflit HF utilisait un cas sans reçu de réconciliation ; le pilote exige maintenant la réutilisation du cas réussi.
- La préparation du premier essai de volume avait été interrompue par le lancement trop précoce de la phase suivante ; le pilote attend maintenant la fin JUnit de chacune des trois phases. La campagne finale passe les trois phases.
- Sur Oppo, un libellé de navigation visible avait des bounds nuls dans l’arbre d’accessibilité. Le pilote utilise désormais son parent cliquable visible dans le même XML frais. La capture réelle confirme que les contrôles sont visibles ; aucun clic n’est dérivé de coordonnées supposées.

Les [reçus des tentatives incomplètes](../test-results/rc8-release/failed-attempts.json) conservent leur issue et l’empreinte du reçu privé original. Les builds échoués restent dans leurs dossiers de tentative, sans APK de substitution.

## Portes encore ouvertes

- **ARM physique 16 Ko** : le Honor et l’Oppo ont tous deux des pages de 4 096 octets. L’AVD 16 Ko et l’audit ELF/ZIP ne remplacent pas un téléphone ARM 16 Ko.
- Sources Viewer HF réelles complète/partielle de bout en bout, qualité des six variantes FireViewer, profils bruts et familles d’adapters sur corpus indépendant.
- Précision/oubli après entraînement, endurance prolongée de plusieurs modèles, redémarrages et restrictions constructeur en arrière-plan.
- CI distante et revue des notices natives transitives.

La RC8 est distribuée comme **préversion** avec ces limites. Aucun reçu ne prétend une qualification complète de production.
