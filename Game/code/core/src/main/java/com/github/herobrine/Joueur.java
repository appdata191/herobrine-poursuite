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

    // MODIFIÉ : Définition d'une hitbox composite
    private final float playerHeight = 120f;
    private final float bodyWidth = 60f; // Largeur totale pour le rendu et les collisions latérales
    private final float feetWidth = 40f; // Largeur réduite pour les pieds
    // Calcul de l'offset pour centrer les pieds
    private final float feetOffsetX = (bodyWidth - feetWidth) / 2;

    public Joueur(float startX, float startY) {
        this.x = startX;
        this.y = startY;
    }

    public void create() {
        texture = new Texture("pngegg.png");
    }

    /**
     * MODIFIÉ : La physique de collision utilise maintenant une hitbox composite.
     */
    public void update(float delta, Carte carte) {
        elapsedTime += delta;

        // --- 1. Mouvement Horizontal (utilise la largeur du corps : 60px) ---
        float dx = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) dx += speed * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) dx -= speed * delta;

        x += dx;
        int tile = carte.getTile();
        int bodyLeftTile = (int) Math.floor(x / tile);
        int bodyRightTile = (int) Math.floor((x + bodyWidth - 1) / tile);
        int bottomTile = (int) Math.floor(y / tile);
        int topTile = (int) Math.floor((y + playerHeight - 1) / tile);

        if (dx > 0) { // Collision à droite
            for (int gy = bottomTile; gy <= topTile; gy++) {
                if (carte.isAnyBlockAt(bodyRightTile, gy)) {
                    x = bodyRightTile * tile - bodyWidth;
                    break;
                }
            }
        } else if (dx < 0) { // Collision à gauche
            for (int gy = bottomTile; gy <= topTile; gy++) {
                if (carte.isAnyBlockAt(bodyLeftTile, gy)) {
                    x = (bodyLeftTile + 1) * tile;
                    break;
                }
            }
        }

        // --- 2. Mouvement Vertical (utilise la largeur des pieds : 40px) ---
        if ((Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) && onGround) {
            vy = JUMP_IMPULSE;
            onGround = false;
        }

        vy += GRAVITY * delta;
        y += vy * delta;

        // Recalcul des tuiles pour les pieds
        int feetLeftTile = (int) Math.floor((x + feetOffsetX) / tile);
        int feetRightTile = (int) Math.floor((x + feetOffsetX + feetWidth - 1) / tile);
        bottomTile = (int) Math.floor(y / tile);
        topTile = (int) Math.floor((y + playerHeight - 1) / tile);

        if (vy > 0) { // Le joueur monte (collision avec la tête)
            onGround = false;
            for (int gx = feetLeftTile; gx <= feetRightTile; gx++) {
                if (carte.isAnyBlockAt(gx, topTile)) {
                    y = topTile * tile - playerHeight;
                    vy = 0;
                    break;
                }
            }
        } else { // Le joueur descend (collision avec les pieds)
            boolean landed = false;
            for (int gx = feetLeftTile; gx <= feetRightTile; gx++) {
                if (carte.isAnyBlockAt(gx, bottomTile)) {
                    y = (bottomTile + 1) * tile;
                    vy = 0;
                    onGround = true;
                    landed = true;
                    break;
                }
            }
            if (!landed) {
                onGround = false;
            }
        }

        // Clamp dans les bornes de la map
        if (x < 0) x = 0;
        float mapW = carte.getMapWidth();
        if (x + bodyWidth > mapW) x = mapW - bodyWidth;
    }

    public void render(SpriteBatch batch, float cameraX) {
        // Le rendu utilise toujours la largeur totale pour afficher le sprite entier
        batch.draw(texture, x - cameraX, y, bodyWidth, playerHeight);
    }

    public void dispose() {
        if (texture != null) texture.dispose();
    }

    // Getters mis à jour pour exposer les nouvelles dimensions
    public float getVy() { return vy; }
    public float getY() { return y; }
    public float getX() { return x; }
    public float getWidth() { return bodyWidth; } // Garde le nom pour la compatibilité, mais représente le corps
    public float getHeight() { return playerHeight; }
    public float getElapsedTime() { return elapsedTime; }

    // NOUVEAU : Getters spécifiques pour la hitbox composite
    public float getBodyWidth() { return bodyWidth; }
    public float getFeetWidth() { return feetWidth; }
    public float getFeetOffsetX() { return feetOffsetX; }
}
