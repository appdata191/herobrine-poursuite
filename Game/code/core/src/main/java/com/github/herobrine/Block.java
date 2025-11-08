package com.github.herobrine;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Block {
    public enum Type { TOP, BOTTOM }

    public final int gx;
    public final int gy;
    private final Type type;
    private final Texture texture; // texture à utiliser pour ce bloc

    public Block(int gx, int gy, Type type, Texture texture) {
        this.gx = gx;
        this.gy = gy;
        this.type = type;
        this.texture = texture;
    }

    public boolean isTop() { return type == Type.TOP; }
    public boolean isBottom() { return type == Type.BOTTOM; }

    // dessine le bloc à la position tile gx,gy en pixels (tile taille TILE)
    public void render(SpriteBatch batch, int TILE, float cameraX) {
        float drawX = gx * TILE - cameraX;
        float drawY = gy * TILE;
        batch.draw(texture, drawX, drawY, TILE, TILE);
    }

    @Override
    public String toString() {
        return "Block{" + "gx=" + gx + ", gy=" + gy + ", type=" + type + '}';
    }
}