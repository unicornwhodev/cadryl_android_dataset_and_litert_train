# Cadryl rc7 — preuves de livraison

27 septembre 2026. Les APK Release ARM64 et x86_64 sont minifiées, non débogables et signées avec la clé durable. Chacune passe **45/45 tests métier et 5/5 scénarios UI**, sans test ignoré ni crash ART observé, sur l’émulateur API 36 en pages de 16 Ko. L’ARM64 utilise `libndk_translation` : aucune qualification physique ARM n’en est déduite.

`summary.json` relie les résultats aux APK. Le packageur vérifie les 213 fichiers compilés contre Git, les signatures, les résultats et l’audit natif. Les derniers builds incluent les sources définitives du pilote UI ; la paire ARM est identique octet par octet à celle des suites métier et UI déjà réussies. Les reçus de ces suites restent inchangés.

Les résultats hôte sont 84/84 JVM, 87/87 Python, zéro erreur lint et 124 avertissements. Les bibliothèques natives passent l’audit strict ELF et ZIP. Le reçu de continuité vérifie la préservation de l’original et la reprise des derniers poids appris.

## Essais initiaux conservés

- `initial-arm/core` : 43/45. Deux sélecteurs de navigation dépendaient de noms de classes renommés par R8. Les destinations utilisent désormais des identifiants explicites.
- `initial-arm/ui` : conflit de signature du pilote de test. Sa clé Debug existante a été réutilisée, sans désinstallation.
- `initial-arm/ui-existing-key` : 2/5. Le libellé « Atelier » apparaît dans deux barres ; le pilote attend désormais le raccourci des projets pour reconnaître l’accueil.
- `initial-ui/ui` : 4/5. Le texte enfant d’un bouton désactivé reste marqué actif par l’accessibilité Android. Le test vérifie maintenant le nœud qui porte l’action. Le bouton est bien désactivé sans source.

La trace R8 reste partielle sur des références de framework et de bibliothèques de test. Les différences d’API applicatives ont été relues avant reconstruction ; les compilations et les suites Release réelles constituent les vérifications exécutées.

## Périmètre public

Les captures françaises dans `visuals` sont natives, non retouchées, prises sur la Release x86_64 avec un projet de démonstration vide. `binding.json` vérifie le hash de l’APK installée. La capture Images montre le contrôle empêchant de continuer sans source.

Les fichiers `package.txt` sont des extraits identifiés du dump brut. Les identifiants synthétiques inutiles à la filiation sont omis du reçu de continuité. Les fixtures ont été préparées et relues avec root uniquement sur l’émulateur dédié ; les tests applicatifs tournent avec ADB en UID shell 2000. Aucun poids, jeton, dépôt privé ou corpus utilisateur n’est inclus.

Le téléphone, les échanges HF réels, les interruptions externes, la précision des modèles et la CI gardent leurs qualifications distinctes. [Note de version](../../docs/RC7_RELEASE.md).


Privacy update, 30 September 2026: public host logs and historical qualification ZIPs are explicitly anonymised. Original bytes are retained privately; APK bytes and measured results are unchanged. See docs/PRIVACY_REDACTION_2026_09.md and the release-specific PRIVACY_REDACTION.json.
