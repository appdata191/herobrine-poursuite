package com.github.herobrine;

import com.badlogic.gdx.graphics.Texture;

public class Porte extends AutomateNonMortel {
    public interface DoorStateNotifier {
        void onDoorStateChanged(int doorId, boolean open);
    }

    private static DoorStateNotifier notifier;

    private boolean ouverte = false;
    private float timer = 0f;
    private final float DUREE_OUVERTURE = 3f;
    private final float TILE_SIZE;
    private final int id;

    // NOUVEAU : Les deux textures pour la porte
    private final Texture textureFermee;
    private final Texture textureOuverte;

    public Porte(float x, float y, int tileSize, int id) {
        // Le constructeur parent est appelé avec la texture par défaut (fermée)
        super(x, y, "Porte fermee.png", tileSize, 4 * tileSize);
        this.TILE_SIZE = tileSize;
        this.id = id;
        this.hitbox.width = TILE_SIZE/3;
        this.hitbox.height = 4 * TILE_SIZE;

        // NOUVEAU : Chargement des deux textures
        this.textureFermee = this.texture; // La texture initiale est déjà "Porte fermee.png"
        this.textureOuverte = new Texture("Porte ouverte.png");
    }

    @Override
    public void update(float delta) {
        if (ouverte) {
            timer += delta;
            if (timer >= DUREE_OUVERTURE) {
                fermer();
                timer = 0f;
            }
        }
    }

    public void ouvrir() {
        setDoorState(true, true);
    }

    public void fermer() {
        setDoorState(false, true);
    }

    public boolean estOuverte() {
        return ouverte;
    }

    @Override
    public void dispose() {
        // NOUVEAU : Libérer les deux textures
        if (textureFermee != null) textureFermee.dispose();
        if (textureOuverte != null) textureOuverte.dispose();
        // Le super.dispose() n'est plus nécessaire car nous gérons les textures ici
    }

    public int getId() {
        return id;
    }

    public static void setDoorStateNotifier(DoorStateNotifier doorStateNotifier) {
        notifier = doorStateNotifier;
    }

    public void applyNetworkState(boolean open) {
        setDoorState(open, false);
    }

    private void setDoorState(boolean open, boolean notifyNetwork) {
        if (this.ouverte == open) {
            if (open) {
                timer = 0f;
                if (notifyNetwork && notifier != null) {
                    notifier.onDoorStateChanged(id, true);
                }
            }
            return;
        }
        this.ouverte = open;
        if (open) {
            timer = 0f;
            this.hitbox.width = 0;
            this.hitbox.height = 0;
            this.texture = textureOuverte;
        } else {
            timer = 0f;
            this.hitbox.width = TILE_SIZE/3;
            this.hitbox.height = 4 * TILE_SIZE;
            this.texture = textureFermee;
        }
        if (notifyNetwork && notifier != null) {
            notifier.onDoorStateChanged(id, open);
        }
    }
}
