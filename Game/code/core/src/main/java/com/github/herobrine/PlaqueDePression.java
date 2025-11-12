package com.github.herobrine;

public class PlaqueDePression extends AutomateNonMortel {
    private Porte porteAssociee;

    public PlaqueDePression(float x, float y, int tileSize) { // MODIFIÉ : Prend tileSize en paramètre
        // La plaque de pression fait 1 tuile de large et 1 tuile de haut
        super(x, y, "pressure_plate.png", tileSize, tileSize); // MODIFIÉ
        // La hitbox est initialisée à la taille d'une tuile (1x1)
        this.hitbox.width = tileSize;
        this.hitbox.height = tileSize;
    }

    public void setPorteAssociee(Porte porte) {
        this.porteAssociee = porte;
    }

    @Override
    public void update(float delta) {
        // La plaque de pression n'a pas de logique de mise à jour propre,
        // son action est déclenchée par la collision avec le joueur dans Carte.updateAutomates.
    }

    public void activerPorte() {
        if (porteAssociee != null && !porteAssociee.estOuverte()) {
            porteAssociee.ouvrir();
        }
    }
}
