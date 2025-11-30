package com.github.herobrine;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

// Classe abstraite représentant un automate non mortel dans le jeu.
public abstract class AutomateNonMortel {
    protected float x, y;
    protected Texture texture;
    protected Rectangle hitbox;
    protected float width, height;

    public AutomateNonMortel(float x, float y, String texturePath, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.texture = new Texture(texturePath);
        this.hitbox = new Rectangle(x, y, width, height);
    }

    public abstract void update(float delta);

    public void render(SpriteBatch batch, float cameraX) {
        batch.draw(texture, x - cameraX, y, width, height);
    }

    public void dispose() {
        if (texture != null) texture.dispose();
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public Rectangle getHitbox() { return hitbox; }

    public void setX(float x) {
        this.x = x;
        this.hitbox.x = x;
    }

    public void setY(float y) {
        this.y = y;
        this.hitbox.y = y;
    }
}
