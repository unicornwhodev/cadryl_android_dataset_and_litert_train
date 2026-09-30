# Audit du 30 septembre 2026

L'audit a contrôlé les sources, les artefacts publiés et leurs empreintes, puis
construit de nouvelles APK pour l'édition publique 0.0.1.

## Corrections

Dix WebP de launcher issus du template étaient indécodables. Ils sont retirés,
les quatre ressources vectorielles et adaptatives Cadryl sont conservées et un
test Android dessine les deux icônes à cinq densités.

La licence libjpeg-turbo renvoyait vers une notice IJG absente. Le README IJG
original est ajouté, son absence fait échouer le collecteur et les notices hors
ligne sont régénérées. Les six correspondances entre bibliothèques natives et
notices sont relues ; la revue transitive complète reste distincte.

Les anciens scripts inutilisés de packaging dépendant de CI sont retirés.
Le packageur local de release contrôle les sources Git, les APK signées, leurs
payloads, les résultats Android et la conservation de la base installée. Il
refuse d'écraser une livraison existante.

## Vie privée et périmètre

[Les preuves publiques sont expurgées](PRIVACY_REDACTION_2026_09.md). Les originaux
restent privés. Aucune clé AI Studio, clé privée de signature, base utilisateur
ou poids privé n'est intégré aux nouveaux packages.

Le dépôt public conserve Apache-2.0, tous les tests publics et les fonctions de
modèles, apprentissage et exports. Aucun workflow GitHub Actions n'est créé.

[Preuves propres à 0.0.1](RELEASE_001_EVIDENCE.json) ·
[Limites restantes](../KNOWN_LIMITATIONS.md).
