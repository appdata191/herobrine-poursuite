package com.github.herobrine;

import com.badlogic.gdx.graphics.Texture;

public class Porte extends AutomateNonMortel {
    private boolean ouverte = false;
    private float timer = 0f;
    private final float DUREE_OUVERTURE = 3f;
    private final float TILE_SIZE;

    // NOUVEAU : Les deux textures pour la porte
    private final Texture textureFermee;
    private final Texture textureOuverte;

    public Porte(float x, float y, int tileSize) {
        // Le constructeur parent est appelé avec la texture par défaut (fermée)
        super(x, y, "Porte fermee.png", tileSize, 4 * tileSize);
        this.TILE_SIZE = tileSize;
        this.hitbox.width = TILE_SIZE/3;
        this.hitbox.height = 4 * TILE_SIZE;

        // NOUVEAU : Chargement des deux textures
        this.textureFermee = this.texture; // La texture initiale est déjà "Porte fermee.png"
        this.textureOuverte = new Texture("Porte ouverte.png");
    }

    @Override
    public void update(float delta) {
        if (ouverte) {
            timer += delta;
            if (timer >= DUREE_OUVERTURE) {
                fermer();
                timer = 0f;
            }
        }
    }

    public void ouvrir() {
        if (!ouverte) {
            ouverte = true;
            this.hitbox.width = 0;
            this.hitbox.height = 0;
            // NOUVEAU : Changer la texture pour la porte ouverte
            this.texture = textureOuverte;
        }
    }

    public void fermer() {
        if (ouverte) {
            ouverte = false;
            this.hitbox.width = TILE_SIZE/3;
            this.hitbox.height = 4 * TILE_SIZE;
            // NOUVEAU : Changer la texture pour la porte fermée
            this.texture = textureFermee;
        }
    }

    public boolean estOuverte() {
        return ouverte;
    }

    @Override
    public void dispose() {
        // NOUVEAU : Libérer les deux textures
        if (textureFermee != null) textureFermee.dispose();
        if (textureOuverte != null) textureOuverte.dispose();
        // Le super.dispose() n'est plus nécessaire car nous gérons les textures ici
    }
}
