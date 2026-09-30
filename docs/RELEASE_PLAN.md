# Livrer Cadryl 0.0.1

[Version et contenu](RELEASE_001.md) · [Preuves](RELEASE_001_EVIDENCE.json).

La release publique utilise versionCode 14 et la signature durable. Le packageur `tools/package_public_release.py` exige les sources Git exactes, les nouvelles paires APK Debug/Release, les tests Android, les parcours UI indépendants et la conservation de la base installée. Les preuves brutes contenant des chemins personnels restent privées.

L’APK ARM64 signée, le ZIP de qualification anonymisé, les trois archives Maven et leurs empreintes sont disponibles dans la release GitHub. Le package OCI conserve les mêmes fichiers pour les utilisateurs autorisés ; il ne s’agit pas d’un conteneur exécutable.

Les scripts de RC historiques conservent leurs contrats propres. Ne pas réutiliser une ancienne preuve pour qualifier une nouvelle APK. Aucun workflow GitHub Actions. Après upload, retélécharger et comparer chaque fichier. Les anciennes archives expurgées ont une notice de retrait et de nouvelles empreintes.

La publication suit la décision du propriétaire du 30 septembre ; les limites mesurées et les essais non exécutés restent documentés.
