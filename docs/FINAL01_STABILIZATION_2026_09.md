# Stabilisation suivant rc8 — 30 septembre 2026

Travail de préparation demandé par le propriétaire. La release `v4.2.0-rc8`
publiée reste immuable. Aucun nouveau candidat final n’est publié par cette note.

Cette note décrit la campagne précédente, antérieure à la séparation complète
de l’édition publique. Les mentions commerciales sont historiques ; leur code
a été retiré de cet arbre. Voir [l’état public actuel](PUBLIC_EDITION.md).

## Modifications

- Imports de dossiers et manifestes dans une transaction Room : erreur ou
  interruption conserve l’index précédent. Le téléchargement HF précède la transaction.
- Lecture UTF-8 stricte, budget réel en octets, conservation des retours intégrés.
- ZIP local et de récupération relus entrée par entrée, taille et SHA-256,
  avant remplacement atomique. Une erreur conserve le fichier précédent.
- Suppression de deux dépendances directes inutilisées, sept couleurs de template,
  53 clés de traduction sans référence et deux méthodes de stockage inutilisées.
  Les outils de purge réellement utilisés et leurs protections restent en place.
- Bannière AdMob liée à l’état d’export, choix d’âge neutre, UMP, une offre Play
  mensuelle de 3,99 EUR, validation serveur locale et restauration préparées.
  Mode par défaut `disabled`, aucun achat ou annonce de production déclaré validé.
- Confidentialité et conditions FR/EN, licences hors ligne et manifestes
  d’empreintes. Inventaires de 103 dépendances par défaut et 144 dans le build
  optionnel ; source de javax.inject
  pour sa notice manquante et notices natives associées à six binaires vérifiés.
- Édition source Apache-2.0 sans SDK publicitaire ni facturation par défaut,
  code optionnel séparé et vérification des DEX/manifeste dans chaque APK construit.
- 37 logs historiques archivés avec leurs empreintes ; tests préparés et reçus
  publiés conservés. Voir [nettoyage](SOURCE_CLEANLINESS.md).

## Preuves et portée

Les builds écrivent des reçus sous `dist/android/runs/` et `dist/release-tests/runs/`.
Les campagnes locales sont conservées sous `test-results/final01-*`. Ces dossiers
de travail sont ignorés par Git ; ils ne sont pas les assets de la release rc8.
Le rapport final de préparation lie les empreintes APK, suites et dispositifs.

Avant la reprise UI, les contrôles exécutés passent 122 tests JVM, 102 tests Python des outils
et 10 tests synthétiques du serveur local. Lint : zéro erreur et 85 avertissements dans l’édition source, 86 dans
le build optionnel de test (122 tests JVM également réussis) ;
Room/KSP : 101 fichiers générés. Les suites métier passent 56/56 sur l’AVD
x86_64/API 36/pages 16 Ko en Debug et sur l’Oppo ARM64/API 33/pages 4 Ko
en Release minifiée. Le pilote UI indépendant passe quatre scénarios sur Oppo.
La matrice de l’AVD capture 144 cas par orientation, soit 288 captures
avec 16 écrans, deux documents, FR/EN, clair/sombre et texte à 100/200 %.
La revue visuelle relève encore des textes coupés à 200 % en paysage ;
les captures et les suites réussies ne constituent pas une certification UI.
Chaque suite conserve son propre couple APK/test ; un changement ultérieur
ne reçoit pas automatiquement ces résultats.

La [reprise UI suivante](UI_REWORK_2026_09.md) est compilée séparément : 122 JVM,
104 tests Python des outils, lint source 0 erreur / 86 avertissements, 101 KSP,
58/58 Android sur l’AVD et l’Oppo Release. Les deux APK Debug finales sont
identiques octet pour octet au couple exercé sur l’AVD ; les règles de signatures
de recette ajoutées affectent R8 Release. Le build optionnel avec SDK de test
compile les nouveaux composants (122 JVM, lint 0 erreur / 87 avertissements).
Les annonces et achats réels restent des essais distincts.
Les [preuves publiques de préparation](FINAL01_EVIDENCE.json) lient chaque
campagne au couple d’APK correspondant sans contenu de projet ni identifiant privé.
Les matrices finales produisent 288 captures par dispositif (576 en tout) ;
le pilote UI indépendant de l’Oppo réussit quatre parcours par clics réels.
La dernière comparaison des 1 000 images publiques garde l’empreinte du relevé
ci-dessous. Les documents de préparation ne certifient pas l’interface finale.

Les premiers essais et erreurs de build/pilote/R8 sont conservés. Une APK issue
d’un essai incomplet ne reçoit pas une qualification. Le pilote UI identifie
chaque route et attend les transitions Compose/WindowManager avant de capturer.

Une empreinte logique de toutes les tables installées est comparée avant/après
mise à jour sur Oppo, sans exporter le contenu des projets. Le corpus source
public de 1 000 images fait l’objet d’empreintes indépendantes.
Les 1 000 empreintes correspondent exactement au relevé effectué avant les
mises à jour. SHA-256 du relevé trié en UTF-8/LF :
`f2d7d76272e6060ceed815f33cbd01d1240f77550603bf99eb66a66a12a56fa4`.

## Conditions encore ouvertes

- Identité légale complète et adresse professionnelle. Renseignements reçus :
  Unicorn Who Dev, France, unicornwhodev@gmail.com, audience tout public.
- AdMob : compte déclaré en attente de validation, app et bannière créées,
  identifiants publics configurés. Diffusion, domaine/app-ads.txt et messages
  de confidentialité à qualifier ;
  produit et base plan Play, serveur autorisé et achats réels qualifiés.
- Informations légales et déclarations Play correspondant à la configuration
  effective, dont Families, Data safety et droits du consommateur.
- Fermeture de la provenance native pour les en-têtes et binaires non reconstruits.
- Revue UI de tous les états métier, clavier, TalkBack, tablette et acceptation
  humaine ; les matrices capturées ne valent pas une certification intégrale.
- Téléphone ARM avec pages de 16 Ko et limites de qualification déjà déclarées
  dans [KNOWN_LIMITATIONS](../KNOWN_LIMITATIONS.md).

Voir [plan final](FINAL_RC_0_1_PLAN.md) et [édition publique](PUBLIC_EDITION.md).
