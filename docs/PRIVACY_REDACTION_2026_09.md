# Anonymisation des preuves publiques

Le propriétaire a demandé le retrait de ses informations personnelles le
30 septembre 2026. Les chemins de compte Windows des rapports sont remplacés
par un emplacement générique. Les originaux sont conservés en privé avec leur
empreinte ; les résultats mesurés ne sont pas changés.

[Correspondance des fichiers expurgés](PRIVACY_REDACTION_2026_09.json).

Les anciennes archives de qualification concernées sont remplacées par des
copies explicitement nommées `anonymized`. Leur reçu distingue les empreintes
originales des nouvelles. Les manifestes internes anciens restent des preuves
historiques ; ils ne décrivent pas les octets des journaux expurgés. Les APK
incluses conservent exactement leurs octets et leurs signatures.

L'historique Git public est nettoyé des chemins personnels et l'adresse de
commit du propriétaire est remplacée par une adresse de projet sans identité
personnelle. La correspondance des commits est conservée en privé. Les notices
et noms des auteurs tiers restent présents pour respecter leurs licences.

Les nouveaux packages contiennent une synthèse contrôlée et les empreintes
des preuves locales. Ils ne contiennent ni numéro de téléphone personnel,
identifiant ADB, base utilisateur ni journaux bruts de la machine de build.

## Cache du fournisseur

Les branches et tags publics ont été vérifiés après réécriture. GitHub conserve
encore des vues d'anciens commits et références de PR qui exposent l'ancien email
de commit. Une demande de purge est préparée en privé ; elle n'est pas envoyée
sans autorisation de contact. Le nettoyage des sources distribuées est terminé,
mais la suppression de ces copies côté fournisseur n'est pas confirmée.

[Procédure officielle GitHub](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/removing-sensitive-data-from-a-repository).
