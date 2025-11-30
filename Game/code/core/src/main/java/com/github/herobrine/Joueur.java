package com.github.herobrine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.github.herobrine.reseau.PacketPlayer;

import java.util.HashMap;
import java.util.Map;

public class Joueur {
    private Texture texture;
    private float x, y;
    private float speed = 300f;
    private int id;
    private final Map<Integer, RemotePlayerState> otherPlayers = new HashMap<>();

    // physics
    private float vy = 0f;
    private final float GRAVITY = -1200f;
    private final float JUMP_IMPULSE = 700f;
    private boolean onGround = true;

    private float elapsedTime = 0f;
    private boolean dead = false;
    private static final float MAX_STEP = 1f / 60f; // limiter le timestep pour éviter les grands sauts de physique

    // Hitbox composite
    private final float playerHeight = 120f;
    private final float bodyWidth = 60f;
    private final float feetWidth = 40f;
    private final float feetOffsetX = (bodyWidth - feetWidth) / 2;

    public Joueur(float startX, float startY) {
        this.x = startX;
        this.y = startY;
    }

    public void create() {
        texture = new Texture("pngegg.png");
    }

    public void update(float delta, Carte carte) {
        elapsedTime += delta;
        // Gérer les grands delta times en les divisant en plusieurs petites étapes pour qu'il n'y ait pas de "téléportation" à travers les murs
        int steps = Math.max(1, (int) Math.ceil(delta / MAX_STEP));
        float step = delta / steps;

        for (int i = 0; i < steps; i++) {
            // --- 1. Mouvement Horizontal ---
            float dx = 0f;
            if (Gdx.input.isKeyPressed(Input.Keys.D)) dx += speed * step;
            if (Gdx.input.isKeyPressed(Input.Keys.A)) dx -= speed * step;

            x += dx;
            int tile = carte.getTile();
            int bodyLeftTile = (int) Math.floor(x / tile);
            int bodyRightTile = (int) Math.floor((x + bodyWidth - 1) / tile);
            int bottomTile = (int) Math.floor(y / tile);
            int topTile = (int) Math.floor((y + playerHeight - 1) / tile);

            if (dx > 0) {
                for (int gy = bottomTile; gy <= topTile; gy++) {
                    if (carte.isAnyBlockAt(bodyRightTile, gy)) {
                        x = bodyRightTile * tile - bodyWidth;
                        break;
                    }
                }
            } else if (dx < 0) {
                for (int gy = bottomTile; gy <= topTile; gy++) {
                    if (carte.isAnyBlockAt(bodyLeftTile, gy)) {
                        x = (bodyLeftTile + 1) * tile;
                        break;
                    }
                }
            }

            // --- 2. Mouvement Vertical ---
            
            // Déclenchement du saut (inchangé)
            if ((Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) && onGround) {
                vy = JUMP_IMPULSE;
                onGround = false;
            }

            // Application de la gravité (inchangé)
            vy += GRAVITY * step;

            // NOUVEAU : GESTION DU SAUT DE HAUTEUR VARIABLE
            // Si le joueur relâche la touche de saut alors qu'il est encore en train de monter...
            boolean jumpKeyReleased = !Gdx.input.isKeyPressed(Input.Keys.W) && !Gdx.input.isKeyPressed(Input.Keys.SPACE);
            if (jumpKeyReleased && vy > 0) {
                // ...on "coupe" son ascension en réduisant sa vitesse verticale.
                // Multiplier par une valeur inférieure à 1 (ex: 0.5f) donne un effet plus doux.
                // Mettre à 0 est plus brutal. Essayez différentes valeurs !
                vy *= 0.95f;
            }

            // Application du mouvement vertical (inchangé)
            y += vy * step;

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
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }
    
    public void render(SpriteBatch batch, float cameraX) {
        batch.draw(texture, x - cameraX, y, bodyWidth, playerHeight);
        for (RemotePlayerState remote : otherPlayers.values()) {
            if (remote.dead) continue;
            batch.draw(texture, remote.x - cameraX, remote.y, bodyWidth, playerHeight);
        }
    }

    public void dispose() {
        if (texture != null) texture.dispose();
    }

    // Getters (inchangés)
    public float getVy() { return vy; }
    public float getY() { return y; }
    public float getX() { return x; }
    public float getWidth() { return bodyWidth; }
    public float getHeight() { return playerHeight; }
    public float getElapsedTime() { return elapsedTime; }
    public float getBodyWidth() { return bodyWidth; }
    public float getFeetWidth() { return feetWidth; }
    public float getFeetOffsetX() { return feetOffsetX; }
    public boolean isDead() { return dead; }
    public void setDead(boolean dead) { this.dead = dead; }

    public void updateRemotePlayers(Map<Integer, PacketPlayer> remoteStates) {
        otherPlayers.clear();
        if (remoteStates == null) return;
        for (PacketPlayer pkt : remoteStates.values()) {
            if (pkt == null) continue;
            RemotePlayerState state = new RemotePlayerState();
            state.id = pkt.id;
            state.x = pkt.x;
            state.y = pkt.y;
            state.dead = pkt.dead;
            otherPlayers.put(state.id, state);
        }
    }

    public boolean isAnyRemoteDead() {
        for (RemotePlayerState state : otherPlayers.values()) {
            if (state.dead) return true;
        }
        return false;
    }

    private static class RemotePlayerState {
        int id;
        float x;
        float y;
        boolean dead;
    }
}
