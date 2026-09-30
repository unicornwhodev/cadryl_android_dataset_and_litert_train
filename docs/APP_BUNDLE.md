# Bundle Android signé — 30 septembre 2026

L’édition publique reste `0.0.1`, code 14, identité
`com.unicornwhodev.visiondatasetstudio`. Le studio avec lynx est inclus dans
l’AAB ARM64 construit localement. Les [empreintes](APP_BUNDLE_EVIDENCE.json)
identifient ce nouvel artefact ; les assets de la release GitHub précédente
gardent leurs preuves d’origine.

## Construire depuis les sources

Préparer JDK 21, SDK 36 et les dépendances natives indiquées dans
[ANDROID_QUALIFICATION.md](ANDROID_QUALIFICATION.md). Fournir le CLI
[bundletool officiel](https://github.com/google/bundletool/releases) et une clé
de signature extérieure au dépôt. La clé de distribution doit correspondre au
certificat public consigné dans `config/release-signing.json`.

```powershell
./tools/sign_with_local_keystore.ps1 -KeyDirectory <dossier-externe> `
  -Kind play-bundle -ArtifactDirectory dist/app-bundle `
  -Bundletool <bundletool.jar>
```

Le reçu conserve la compilation Gradle réelle, le manifeste des sources, la
vérification de signature et `bundletool validate`. L’APK applicative est dérivée
de cet AAB, alignée puis signée ; les empreintes de son contenu restent
identiques avant/après signature. L’APK instrumentée et ses mappings R8 sont
produits dans la même tentative.

Les mots de passe sont transmis aux outils par variables d’environnement. Les
clés, APK, AAB, corpus et journaux restent hors des sources Git. Les tests
automatisés du produit restent dans le dépôt.

## Portée du contrôle actuel

- 118 tests JVM et 114 tests Python réussis ; lint zéro erreur, 88 avertissements ; 101 fichiers KSP.
- Suite Android Debug : 63/63 sur l’émulateur dédié API 36.
- HF privé autorisé : publication et relecture, conflit de parent et reprise après réponse perdue réussis avec des données synthétiques.
- SAF : copie réelle via DocumentsUI, révocation du droit et refus de purge avec conservation des annotations réussis sur l’émulateur.
- AAB signé et APK dérivée non débogable construits ; audit DEX/manifeste sans SDK de publicité, de paiement ni permission publicitaire.

Les tests physiques du studio précédent sont conservés dans
[STUDIO_UI_EVIDENCE.json](STUDIO_UI_EVIDENCE.json). Le téléphone s’est
déconnecté avant l’installation de cette nouvelle APK dérivée : sa recette
physique et son endurance restent à exécuter. Aucun ancien résultat n’est
attribué à ces nouveaux octets.

Le pilote de pannes mesure désormais les empreintes des APK installées. Pour
répéter la publication synthétique HF, il conserve le namespace de recette
précédent et en crée un nouveau. Une destination déjà occupée reste protégée ;
les préférences de publication utilisateur et les preuves précédentes sont conservées.
