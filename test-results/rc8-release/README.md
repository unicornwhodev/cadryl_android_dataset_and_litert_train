# Cadryl RC8 — preuves publiques

Campagne du 30 septembre 2026 (heure de Paris ; reçus horodatés en UTC). Les octets de build/signature restent ceux des tentatives originales. `EVIDENCE_MANIFEST.json` énumère exactement les fichiers publics et leurs SHA-256.

Huit exports texte SAF/volume ont leurs fins de ligne normalisées dans cette copie publique ; `text-normalization.json` conserve les empreintes des originaux privés et des copies. Les reçus originaux de tentative ne sont pas modifiés.

- `arm/` : APK Release durable, 53 tests de base sur Oppo ARM64/API 33/4 Ko, quatre parcours UI indépendants, audit strict 16 Ko.
- `x86/` : APK Release durable pour la distribution ; copie QA avec certificat Android Debug pour conserver les données de l’AVD API 36/16 Ko. Tous les payloads ZIP sont vérifiés identiques. 53 tests de base et quatre parcours UI indépendants.
- `debug/` et `host/` : build réel, KSP/Room, 106 tests JVM, 93 Python, lint 0 erreur/148 avertissements ; 53 tests Debug sur AVD.
- `extended/` : HF réel dans le nouveau dépôt privé autorisé, SAF réel sur Oppo, volume perdu sur AVD, ingestion de 1 000 images publiques CC0 et dix minutes d’inférence sur Honor.
- `failed-attempts.json` : issues et empreintes de tentatives conservées comme échecs/incomplètes.

Les identifiants du dépôt privé HF et ses credentials sont exclus. Les images du corpus, poids, bases utilisateur et captures d’autres applications ne sont pas publiés. La fiche du corpus et sa révision publique sont conservées. La remise au premier plan durant l’import Oppo est consignée séparément ; le temps d’import inclut le blocage antérieur.

La Release ARM finale est byte-identique à celle du benchmark Honor. Le téléphone a été libéré à la demande du propriétaire, puis les derniers contrôles ont été exécutés sur Oppo. Les tests corpus/SAF utilisent le pont Debug DocumentsUI ; il est absent de la Release. Les écritures HF sont effectuées sur Debug/AVD, avec fichiers métier identiques au build final.

**Aucune qualification ARM physique 16 Ko, de précision de modèle ou d’endurance en arrière-plan n’est revendiquée.** [Rapport et portes restantes](../../docs/RC8_QUALIFICATION_2026_09.md).


Privacy update, 30 September 2026: public host logs and historical qualification ZIPs are explicitly anonymised. Original bytes are retained privately; APK bytes and measured results are unchanged. See docs/PRIVACY_REDACTION_2026_09.md and the release-specific PRIVACY_REDACTION.json.
