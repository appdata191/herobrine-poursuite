package com.github.herobrine;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Classe abstraite pour automates mortels.
 */
public abstract class AutomateMortel {
    protected final float width;
    protected final float height;

    protected AutomateMortel(float width, float height) {
        this.width = width;
        this.height = height;
    }

    public abstract void update(float delta);
    public abstract void render(SpriteBatch batch, float cameraX);
    public abstract void dispose();

    // position en pixels (à implémenter dans les sous-classes)
    public abstract float getX();
    public abstract float getY();

    /**
     * Détection de kill : si overlap AABB => mort.
     * (Plus de logique "stomp" — tout contact tue.)
     */
    public boolean kill(Joueur joueur) {
        float px = joueur.getX();
        float py = joueur.getY();
        float pw = joueur.getWidth();
        float ph = joueur.getHeight();
        float ax = getX();
        float ay = getY();

        boolean overlapX = px < ax + width && px + pw > ax;
        boolean overlapY = py < ay + height && py + ph > ay;
        return overlapX && overlapY;
    }
}