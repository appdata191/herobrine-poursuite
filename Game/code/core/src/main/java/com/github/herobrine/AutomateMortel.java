package com.github.herobrine;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

// Classe abstraite représentant un automate mortel dans le jeu.
public abstract class AutomateMortel {
    protected float x, y, width, height;
    protected Rectangle hitbox;

    public AutomateMortel(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.hitbox = new Rectangle(x, y, width, height);
    }

    public abstract void update(float delta);
    public abstract void render(SpriteBatch batch, float cameraX);
    public abstract void dispose();

    // La méthode est correctement surchargée par les classes filles.
    // Son implémentation ici n'a pas d'importance, mais elle doit exister.
    public abstract boolean kill(Joueur joueur);
}
