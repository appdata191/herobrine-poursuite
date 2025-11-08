package com.github.herobrine;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Pics stationnaires : largeur 60, hauteur 20.
 * Ils tuent toujours au contact (côté, dessus ou dessous).
 */
public class Pics extends AutomateMortel {
    private final Texture texture;
    private final float x;
    private final float y; // bottom position in pixels

    public Pics(int gx, int gy, int tile) {
        super(60f, 30f);
        this.x = gx * tile;
        // placer la base (y) au sommet du bloc (gy*tile + tile)
        this.y = gy * tile + tile;
        texture = new Texture("Pics.png");
    }

    @Override
    public void update(float delta) {
        // stationary
    }

    @Override
    public void render(SpriteBatch batch, float cameraX) {
        batch.draw(texture, x - cameraX, y, width, height);
    }

    @Override
    public float getX() { return x; }

    @Override
    public float getY() { return y; }

    @Override
    public boolean kill(Joueur joueur) {
        // override : kill on any overlap (no stomp immunity)
        float px = joueur.getX();
        float py = joueur.getY();
        float pw = joueur.getWidth();
        float ph = joueur.getHeight();
        boolean overlapX = px < x + width && px + pw > x;
        boolean overlapY = py < y + height && py + ph > y;
        return overlapX && overlapY;
    }

    @Override
    public void dispose() {
        if (texture != null) texture.dispose();
    }
}