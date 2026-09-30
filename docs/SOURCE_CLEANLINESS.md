# Sources, licence et nettoyage — 30 septembre 2026

## Studio actuel avec lynx

L’index préparé contient 1 116 fichiers. Gitleaks 8.24.2 relève 97 correspondances,
toutes classées après vérification : empreintes de sources ou de certificats
publics, aucun secret confirmé et aucun cas non résolu. Le nouveau manifeste
de compilation est comparé aux blobs Git ; ses octets de reçu sont préservés.
Aucune clé AI Studio au format connu, information personnelle de l’éditeur ou
numéro de téléphone dans les Markdown n’est trouvé dans l’arbre public vérifié.
Aucun SDK commercial dans les sources applicatives publiques, aucun workflow
GitHub Actions, aucune APK, fixture de travail ou clé de signature ajoutée.

Les 327 fichiers compilés correspondent au build réel. Barlow et Phosphor
conservent leurs licences et empreintes épinglées ; le lynx original garde son
empreinte approuvée. Les logs bruts, corpus et captures de recette restent
dans les dossiers ignorés. Les tests de régression restent dans les sources.
[Preuves du studio](STUDIO_UI_EVIDENCE.json).

## Séparation publique précédente

Le code du projet reste sous Apache-2.0. Les composants tiers conservent leurs
propres licences et notices ; aucune licence Google n’est remplacée par Apache-2.0.

Le dépôt contient uniquement l’édition publique. Les anciens répertoires de
monétisation, le serveur de facturation, leurs configurations, conditions et
tests ont été déplacés hors de l’arbre avec vérification SHA-256.
Les imports, annotations, modèles, entraînement et exports sont conservés.
Les SDK AdMob/UMP/Billing ne sont pas des dépendances de cette compilation.
Le retrait de l’historique récent a été expressément autorisé par le titulaire.
Voir [édition publique](PUBLIC_EDITION.md) et [didacticiel](FIRST_LAUNCH_TUTORIAL.md).

## Nettoyage des preuves de travail

37 anciens logs bruts de développement sont retirés de l’arbre public courant.
[Le manifeste](SOURCE_CLEANUP_2026_09.json) conserve chaque chemin, taille et
SHA-256. Ils sont sauvegardés localement sous le chemin ignoré indiqué dans
ce reçu et restent accessibles dans le commit Git précédent indiqué. Les
liens Markdown vers ces logs renvoient à ce commit historique.

Les tests JVM, Android et Python, les petites fixtures synthétiques documentées
et les reçus publiés rc6/rc7/rc8 sont conservés. Les reçus de build et de recette
de cette campagne restent dans les dossiers de travail ignorés. Ils ne sont
pas des fichiers de produit à publier avec le code.

Aucune APK, archive de poids, donnée utilisateur, clé de signature ou jeton
n’est ajouté au commit. Le contrôle des secrets porte sur l’intégralité de
l’arbre Git préparé, en plus du diff. Les résultats exécutés sont consignés
dans le rapport de stabilisation ; ce texte ne remplace pas leur reçu.

## Contrôle historique avant la séparation publique

Gitleaks 8.24.2 ne détecte aucun secret dans le diff préparé. Le scan complet
de l’export de l’index (environ 9,35 Mo) relève 90 correspondances : 54 SHA-256
publics de fichiers dans des reçus historiques et 36 empreintes de clés publiques
de signature déjà publiées. Toutes sont classées après lecture de leur ligne
complète ; aucun secret confirmé ni cas non résolu. Aucune exclusion globale
n’est ajoutée au scanner.

Lors de cette campagne précédente, les 244 fichiers de compilation de l’index correspondent aux empreintes du
build source final. Le fichier LICENSE conserve son empreinte Apache-2.0.
Les résultats synthétiques figurent dans [les preuves](FINAL01_EVIDENCE.json).

## Contrôle du candidat public et didacticiel

Gitleaks 8.24.2 : aucun secret confirmé ni cas non résolu. Les 90 alertes
de l’index sont 54 empreintes publiques de sources et 36 empreintes publiques
de signature. Le diff complet destiné à remplacer le commit récent contient
18 alertes provenant des anciennes lignes de signature publique supprimées.
Chaque classe a été contrôlée ; aucune exclusion globale n’est ajoutée.

Les 232 fichiers de compilation de l’index correspondent au build réel.
La licence Apache-2.0 est inchangée. Aucune clé AI Studio au format connu
n’est trouvée dans cet arbre ou l’historique Git textuel vérifié. Ce contrôle
ne prouve pas une révocation chez le fournisseur.
Voir [les reçus actuels](PUBLIC_TUTORIAL_EVIDENCE.json).
