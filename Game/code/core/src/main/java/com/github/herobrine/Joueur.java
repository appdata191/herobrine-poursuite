package com.github.herobrine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Joueur {
    private Texture texture;
    private float x, y;
    private float speed = 200f;

    // physics
    private float vy = 0f;
    private final float GRAVITY = -1200f;
    private final float JUMP_IMPULSE = 750f;
    private boolean onGround = true;

    private float elapsedTime = 0f;

    private final float playerWidth = 60f;
    private final float playerHeight = 120f;

    public Joueur(float startY) {
        this.y = startY;
    }

    public void create() {
        texture = new Texture("pngegg.png");
        x = 100;
    }

    public void update(float delta, Carte carte) {
        elapsedTime += delta;

        float prevX = x;
        float prevY = y;

        // horizontal input -> dx
        float dx = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) dx += speed * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) dx -= speed * delta;

        // tentative mouvement horizontal + résolution collisions côté
        x += dx;
        int tile = carte.getTile();
        int leftTile = (int) Math.floor(x / tile);
        int rightTile = (int) Math.floor((x + playerWidth - 1) / tile);
        int bottomTile = (int) Math.floor(y / tile);
        int topTile = (int) Math.floor((y + playerHeight - 1) / tile);

        if (dx > 0) {
            // moving right, check collision with tiles at rightTile
            for (int gy = bottomTile; gy <= topTile; gy++) {
                if (carte.isAnyBlockAt(rightTile, gy)) {
                    x = rightTile * tile - playerWidth;
                    break;
                }
            }
        } else if (dx < 0) {
            // moving left, check leftTile
            for (int gy = bottomTile; gy <= topTile; gy++) {
                if (carte.isAnyBlockAt(leftTile, gy)) {
                    x = (leftTile + 1) * tile;
                    break;
                }
            }
        }

        // jump input
        if ((Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) && onGround) {
            vy = JUMP_IMPULSE;
            onGround = false;
        }

        // gravity + vertical movement
        vy += GRAVITY * delta;
        y += vy * delta;

        // recalc tiles after vertical movement
        leftTile = (int) Math.floor(x / tile);
        rightTile = (int) Math.floor((x + playerWidth - 1) / tile);
        bottomTile = (int) Math.floor(y / tile);
        topTile = (int) Math.floor((y + playerHeight - 1) / tile);

        if (vy > 0) {
            // moving up, check head collisions on topTile
            for (int gx = leftTile; gx <= rightTile; gx++) {
                if (carte.isAnyBlockAt(gx, topTile)) {
                    y = topTile * tile - playerHeight;
                    vy = 0;
                    break;
                }
            }
        } else if (vy < 0) {
            // moving down, check feet collisions on bottomTile
            boolean landed = false;
            for (int gx = leftTile; gx <= rightTile; gx++) {
                if (carte.isTopBlockAt(gx, bottomTile) || carte.isAnyBlockAt(gx, bottomTile)) {
                    // if top block at that tile -> we land on its top surface
                    y = (bottomTile + 1) * tile;
                    vy = 0;
                    onGround = true;
                    landed = true;
                    break;
                }
            }
            if (!landed) {
                // also check ground default
                float carteGround = carte.getGroundY();
                if (y <= carteGround) {
                    y = carteGround;
                    vy = 0;
                    onGround = true;
                } else {
                    onGround = false;
                }
            }
        }

        // clamp dans les bornes de la map (espace monde)
        if (x < 0) x = 0;
        float mapW = carte.getMapWidth();
        if (x + playerWidth > mapW) x = mapW - playerWidth;
    }

    // render with camera offset
    public void render(SpriteBatch batch, float cameraX) {
        batch.draw(texture, x - cameraX, y, playerWidth, playerHeight);
    }

    public void dispose() {
        if (texture != null) texture.dispose();
    }

    // getters used by automates
    public float getVy() { return vy; }
    public float getY() { return y; }
    public float getX() { return x; }
    public float getWidth() { return playerWidth; }
    public float getHeight() { return playerHeight; }
    public float getElapsedTime() { return elapsedTime; }
}