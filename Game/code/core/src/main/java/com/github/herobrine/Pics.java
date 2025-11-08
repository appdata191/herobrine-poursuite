package com.github.herobrine;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

public class Pics extends AutomateMortel {
    private final Texture texture;

    public Pics(int gridX, int gridY, int tile) {
        super(gridX * tile, (gridY + 1) * tile, 60f, 20f);
        this.texture = new Texture("Pics.png");
    }

    @Override
    public void update(float delta) {
        // Les pics sont statiques, pas de mise à jour nécessaire
    }

    @Override
    public void render(SpriteBatch batch, float cameraX) {
        batch.draw(texture, x - cameraX, y, width, height);
    }

    /**
     * CORRIGÉ : La logique de hitbox composite est maintenant appliquée ici.
     */
    @Override
    public boolean kill(Joueur joueur) {
        // Création des rectangles pour la hitbox composite du joueur
        float feetHeight = joueur.getHeight() / 4;
        float upperBodyHeight = joueur.getHeight() - feetHeight;

        Rectangle playerFeet = new Rectangle(
            joueur.getX() + joueur.getFeetOffsetX(),
            joueur.getY(),
            joueur.getFeetWidth(),
            feetHeight
        );

        Rectangle playerUpperBody = new Rectangle(
            joueur.getX(),
            joueur.getY() + feetHeight,
            joueur.getBodyWidth(),
            upperBodyHeight
        );

        // On vérifie si la hitbox des Pics touche l'une ou l'autre partie du corps du joueur
        return hitbox.overlaps(playerFeet) || hitbox.overlaps(playerUpperBody);
    }

    @Override
    public void dispose() {
        if (texture != null) texture.dispose();
    }
}
