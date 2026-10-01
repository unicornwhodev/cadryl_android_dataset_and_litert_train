# Reproductibilité de l’apprentissage — 1 octobre 2026

## Correctif

L’exécution de l’apprentissage utilise un thread CPU, également à la reprise
d’un ancien checkpoint. Ce choix est enregistré dans le reçu de l’essai.
Le contrat original d’inférence reste conservé. La comparaison des sorties
ne reçoit aucune tolérance supplémentaire : formes, valeurs finies, empreintes
et conservation de l’original restent contrôlées. La qualification exige toutes
les étapes prévues, un résultat fini, un checkpoint relu et une inférence
utilisable. La variation de perte est informative ; aucun gain de précision
n’est imposé.

Sur téléphone ARM64/API 33, deux sessions restaurées depuis le même checkpoint
donnent des sorties identiques sur les douze images de contrôle en mode série.
L’écart maximal est nul. Avec deux threads, le contrôle passait sur onze images
sur douze ; des écarts apparaissaient aussi entre deux inférences sans
rechargement. Le défaut observé venait donc de la non-reproductibilité de
l’inférence multithreadée dans ce périmètre, et non d’une preuve de poids perdus.

## Contrôles exécutés

- Build Android réel : APK Debug et APK instrumentée, 101 fichiers KSP.
- 126 tests JVM, lint zéro erreur et 88 avertissements.
- Cinq tests ciblés sur téléphone, dans une identité QA isolée : apprentissage
  des couches visuelles, persistance et rechargement, WorkManager avec
  interruption/reprise et génération suivante, configuration guidée, affichage
  d’un échec persistant et conservation d’un budget modifié.
- Sur la fixture synthétique du parcours WorkManager, la perte descend de
  0,689188 à 0,003020 ; le modèle original reste inchangé. Ce résultat ne mesure
  pas la précision d’un modèle métier sur des photographies réelles.

Le rafraîchissement de l’état d’apprentissage résiste à une erreur de lecture
et ignore un résultat appartenant à un projet ou lot précédemment sélectionné.
Le guide reprend un budget disque modifié depuis une autre page.

## Portée

Un nouveau parcours natif sur corpus autorisé, dans l’identité QA isolée,
conserve 77 annotations sur 49 images exploitables. L’export local est relu,
les trois cycles terminent 117 étapes (39 images d’apprentissage et 10 de
contrôle), puis les poids sont activés manuellement. L’interface utilise le
checkpoint ; un processus relancé passe l’inférence sur les dix images de
contrôle avec des sorties finies. Le modèle original garde son empreinte.
La recette reste au premier plan. Un fichier à repère EXIF ambigu est exclu
avant l’export ; aucun gain de précision n’est revendiqué.
[Artefact signé et périmètre des preuves](TRAINING_CHECKPOINT_FIX_EVIDENCE.json).

La sauvegarde complète de cette recette conserve les 77 annotations,
le reçu d’export et le checkpoint actif. L’app relit les 382 fichiers de
l’archive ; une relecture indépendante retrouve les huit tables, les mêmes
annotations et les octets du checkpoint. Aucune image de corpus n’est conservée
sur le PC pendant cette vérification.

Les APK publiques reconstruites ne sont pas encore installées sur téléphone.
Les tests physiques de l’identité QA ne qualifient pas leurs octets ni toute
l’interface. Une sérialisation reproductible ne garantit pas qu’un entraînement
améliore un modèle. Une exécution incomplète, un checkpoint altéré ou des sorties
non finies restent refusés. L’activation est manuelle ; les annotations et le
modèle initial sont conservés.
