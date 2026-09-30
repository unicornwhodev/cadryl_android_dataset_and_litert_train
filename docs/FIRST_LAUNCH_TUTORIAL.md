# Didacticiel interactif

Le guide apparaît dans l’accueil lors du premier lancement après cette mise à jour.
Quatre exemples présentent source, annotation, modèle et export. Ils ne créent
aucun projet, ne modifient aucune annotation et ne font aucune requête externe.
L’accès aux fonctions du studio reste disponible pendant le guide.

À la dernière étape, « Ne plus afficher au démarrage » enregistre le choix
sur l’appareil quand « Terminer » est touché. Sans cette case, le guide revient
au prochain lancement. « Plus tard » masque uniquement la session courante.
En cas d’échec d’enregistrement, le guide reste ouvert et affiche une erreur.
Les réglages permettent de rejouer le guide et de changer le choix à sa fin.

`FirstLaunchTutorialTest` couvre les deux choix, le prochain lancement,
la relance depuis les réglages et le report. Les tests restaurent la préférence
initiale et utilisent l’activité réelle ; ils ne réinitialisent pas la base.
