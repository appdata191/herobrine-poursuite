package com.github.herobrine;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Creeper hérite d'AutomateMortel ; mouvement géré ici (physique identique au joueur).
 */
public class Creeper extends AutomateMortel {
    private final Texture texture;
    private final float speed = 160f;
    private float x;
    private final float xMin;
    private final float xMax;
    private float y;
    private int dir = 1;

    // physics like the player
    private float vy = 0f;
    private static final float GRAVITY = -1200f;
    private boolean onGround = false;
    private final Carte carte;
    private final int tileSize;

    public Creeper(int gx1, int gx2, int gy, int tile, Carte carte) {
        super(60f, 120f);
        int sx = Math.min(gx1, gx2);
        int ex = Math.max(gx1, gx2);
        this.xMin = sx * tile;
        this.xMax = ex * tile;
        this.x = this.xMin;
        // placer les pieds du creeper sur le dessus de la tile gy
        this.y = gy * tile + tile;
        this.tileSize = tile;
        this.carte = carte;
        texture = new Texture("creeper.png");
    }

    @Override
    public void update(float delta) {
        if (delta <= 0) return;

        // horizontal movement attempt
        float dx = dir * speed * delta;
        x += dx;

        int tile = tileSize;
        int leftTile = (int) Math.floor(x / tile);
        int rightTile = (int) Math.floor((x + width - 1) / tile);
        int bottomTile = (int) Math.floor(y / tile);
        int topTile = (int) Math.floor((y + height - 1) / tile);

        if (dx > 0) {
            for (int gy = bottomTile; gy <= topTile; gy++) {
                if (carte.isAnyBlockAt(rightTile, gy)) {
                    x = rightTile * tile - width;
                    dir = -1;
                    break;
                }
            }
        } else if (dx < 0) {
            for (int gy = bottomTile; gy <= topTile; gy++) {
                if (carte.isAnyBlockAt(leftTile, gy)) {
                    x = (leftTile + 1) * tile;
                    dir = 1;
                    break;
                }
            }
        }

        // gravity + vertical movement
        vy += GRAVITY * delta;
        y += vy * delta;

        // recalc tiles after vertical move
        leftTile = (int) Math.floor(x / tile);
        rightTile = (int) Math.floor((x + width - 1) / tile);
        bottomTile = (int) Math.floor(y / tile);
        topTile = (int) Math.floor((y + height - 1) / tile);

        if (vy > 0) {
            // head collision
            for (int gx = leftTile; gx <= rightTile; gx++) {
                if (carte.isAnyBlockAt(gx, topTile)) {
                    y = topTile * tile - height;
                    vy = 0;
                    break;
                }
            }
        } else if (vy < 0) {
            // feet collision -> land on TOP blocks
            boolean landed = false;
            for (int gx = leftTile; gx <= rightTile; gx++) {
                if (carte.isTopBlockAt(gx, bottomTile) || carte.isAnyBlockAt(gx, bottomTile)) {
                    y = (bottomTile + 1) * tile;
                    vy = 0;
                    onGround = true;
                    landed = true;
                    break;
                }
            }
            if (!landed) {
                float groundY = carte.getGroundY();
                if (y <= groundY) {
                    y = groundY;
                    vy = 0;
                    onGround = true;
                } else {
                    onGround = false;
                }
            }
        }

        if (x < xMin) {
            x = xMin;
            dir = 1;
        } else if (x > xMax) {
            x = xMax;
            dir = -1;
        }
    }

    @Override
    public void render(SpriteBatch batch, float cameraX) {
        batch.draw(texture, x - cameraX, y, width, height);
    }

    @Override public float getX() { return x; }
    @Override public float getY() { return y; }

    @Override
    public void dispose() {
        if (texture != null) texture.dispose();
    }
}