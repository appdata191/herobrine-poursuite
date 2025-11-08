package com.github.herobrine;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

public class Creeper extends AutomateMortel {
    private final Texture texture;
    private final int startGridX, endGridX;
    private final float startX, endX;
    private int direction = 1;
    private final float speed = 80f;

    public Creeper(int startGridX, int endGridX, int gridY, int tile, Carte carte) {
        super(startGridX * tile, (gridY + 1) * tile, 60f, 120f);
        this.texture = new Texture("creeper.png");
        this.startGridX = startGridX;
        this.endGridX = endGridX;
        this.startX = startGridX * tile;
        this.endX = endGridX * tile;
    }

    @Override
    public void update(float delta) {
        x += direction * speed * delta;
        if (x > endX) {
            x = endX;
            direction = -1;
        }
        if (x < startX) {
            x = startX;
            direction = 1;
        }
        // Mettre à jour la hitbox avec la nouvelle position
        hitbox.setPosition(x, y);
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

        // On vérifie si la hitbox du Creeper touche l'une ou l'autre partie du corps du joueur
        return hitbox.overlaps(playerFeet) || hitbox.overlaps(playerUpperBody);
    }

    @Override
    public void dispose() {
        if (texture != null) texture.dispose();
    }
}
