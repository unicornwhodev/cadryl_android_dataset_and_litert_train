# Cadryl — revue visuelle native

## Findings

Aucun écart P0/P1/P2 actionnable dans l’état comparé. Le lynx fourni et retenu est intégré sans approximation. La revue porte sur l’éditeur natif à taille de texte normale ; l’acceptation humaine de l’ensemble du produit reste ouverte.

## Source, état et dimensions

- Vérité visuelle : `dist/ui-redesign/design-source-lynx.png`, planche de référence de l’éditeur complet, issue de la direction retenue ; le propriétaire a approuvé le lynx.
- Implémentation : `dist/ui-redesign/13-public-editor-final.png` et `dist/ui-redesign/14-public-editor-reference-viewport.png`. [Capture native disponible](docs/studio/native-editor.png) · [liaison à l’APK](docs/studio/capture.json).
- Référence : 819 × 1920 px, interprétée à 2×, soit 409,5 × 960 dp. Viewport Android de comparaison : 1075 × 2715 px à densité 2,625 ; contenu de l’app 409,52 × 960 dp après exclusion des barres système. Capture standard : 1080 × 2400 px. La taille temporaire de l’émulateur a été rétablie.
- Recadrage d’analyse : `[0,132,1075,2652]`, puis réduction uniforme vers 819 × 1920 px. Le [reçu local](dist/ui-redesign/comparison-public-final/normalization.json) conserve les empreintes. Les PNG Android bruts restent intacts.
- État : français, clair, texte 100 %, Boîte sélectionnée, classe Sac, annotation humaine enregistrée, zoom 100 %. Le projet comporte une image synthétique autorisée et aucun modèle IA actif. La référence comporte 32 images et deux propositions IA. Les commandes précédent/suivant désactivées et l’absence de bandeau IA reflètent ces données réelles. Après réouverture, l’historique annuler/rétablir peut être vide.

## Comparaisons ouvertes ensemble

La référence et le rendu sont placés côte à côte dans chaque entrée :

- Vue complète : `dist/ui-redesign/comparison-public-final/full-comparison.png`.
- Marque et navigation : `dist/ui-redesign/comparison-public-final/header-detail.png`.
- Classe, historique, propriétés, zoom et sauvegarde : `dist/ui-redesign/comparison-public-final/console-controls-detail.png`.
- Actions : `dist/ui-redesign/comparison-public-final/actions-detail.png`.

## Les cinq surfaces de fidélité

| Surface | Évaluation |
|---|---|
| Typographie | Barlow embarquée, quatre graisses réelles. Mot-symbole 30 sp semi-gras ; titres 20/26 sp, corps 15/23 sp, commandes 14/20 sp. La planche ne fournit pas sa police originale ; Barlow est le choix explicite de la refonte. Les vues rapprochées montrent une hiérarchie lisible et des libellés sans chevauchement à 100 %. Les noms dynamiques longs utilisent une ellipse avec accès aux détails. |
| Espacement et rythme | Alignement du header, règle fine, repères, cinq outils et console regroupée vérifiés. Les commandes conservent une cible de 48 dp. La console est plus compacte en mode manuel ; l’image conserve ses proportions et ses coordonnées. La photo carrée laisse des marges dans le viewport haut : aucune déformation pour imiter la maquette. La capture standard confirme l’utilisation du canevas. |
| Couleurs | Porcelaine F5F2EC, graphite 252D2C, annotation vermillon D85836. Action B94222 avec texte blanc : contraste 5,43:1. Texte F3F1E9 sur console : 12,46:1 ; texte secondaire BBC5BE : 7,95:1. Le bouton est assombri pour le contraste ; les propositions IA gardent un état distinct. Les aplats sobres sont une adaptation assumée de la direction. |
| Images, logo et icônes | Bitmap du lynx original transparent, SHA-256 `3ac82a491945cdb3a828b6000214d99911dfa27b101e41d0bc957d4e22ba1121` ; aucun C de substitution. La capture confirme sa netteté et son fond transparent. Phosphor Duotone officiel, sources et licences épinglées, même famille pour les outils et panneaux. La photo de recette diffère de la photo de référence et reste aspect-fit. Les carrés/poignées et le libellé Barlow des boîtes sont rendus par le canevas natif. |
| Texte du produit | Outils, Sélection, Boîte, Point, Masque, Classe, Enregistré, Passer et Valider restent courts et autonomes. Le nombre de classe et les compteurs viennent du projet réel. Le bouton des propriétés devient icône + compteur à faible largeur, avec libellé d’accessibilité complet. Aucun résultat d’inférence fictif ni succès d’export fabriqué. Les parcours FR/EN sont exercés séparément. |

## Historique des corrections

1. **P2 — annotations ochre, poignées rondes, libellé plat.** Rétablissement du vermillon, poignées carrées et étiquette Barlow. Captures 06/07, puis `13-public-editor-final.png` et `14-public-editor-reference-viewport.png` : correction vérifiée après modification.
2. **P2 — badge de classe et cadres de commandes absents.** Ajout de l’ordinal réel, cadres annuler/rétablir et groupe de zoom ; propriétés et commandes préservées. Vues rapprochées de la console recapturées : correction vérifiée.
3. **P2 — header incomplet et lynx trop petit.** Lynx original 44 dp, règle et repères rétablis ; recapture et comparaison du header : correction vérifiée.
4. **P1 — action principale de l’accueil cachée sous le contenu.** Action persistante dans le Scaffold ; les assertions existantes et le pilote physique vérifient son accès.
5. **P1 — consignes de workflow inaccessibles depuis les propriétés.** Commande dans l’en-tête du panneau ; test fonctionnel conservé et réussi.

Les réparations de compilation ou de l’API de recette ne sont pas comptées comme itérations visuelles. Le défaut de reprise au démarrage dispose d’un test Android distinct sur huit créations répétées du ViewModel et un vrai projet Room.

## Interactions et portée

Les tests vérifient annotation/préservation, import/export, modèles, apprentissage, accès aux consignes, navigation, didacticiel et reprise. La suite exacte passe 63/63 en Debug et en Release physique ; le pilote indépendant passe 5/5. Les matrices portrait/paysage couvrent les routes, langues et thèmes et conservent les captures et sémantiques. Leur nombre ne prouve pas une revue humaine exhaustive ni la précision des modèles. [Empreintes et preuves](docs/STUDIO_UI_EVIDENCE.json).

## Follow-up polish

- **P3 — coin du badge ordinal.** Le parent du bouton arrondit légèrement le coin gauche du badge. La forme et le nombre restent visibles ; affiner ce clip si une reproduction plus stricte est souhaitée.
- **P3 — pluriels des petits compteurs.** Certains messages de configuration emploient encore « 1 images » ou « image(s) ». Ils ne changent pas les compteurs ni l’accès aux actions.

## Implementation checklist

- [x] Lynx original et licences embarqués.
- [x] Détails du header et de la console corrigés puis recapturés.
- [x] Comparaison complète et trois comparaisons ciblées ouvertes côte à côte.
- [x] Fonctions métier et confirmations conservées ; builds et essais distincts reçus.
- [ ] Acceptation humaine de l’ensemble de l’interface.

final result: passed
