package com.github.herobrine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Menu intermédiaire pour l'éditeur de carte.
 * Propose de créer une nouvelle carte ou d'en modifier une existante.
 *
 * updateInput() retourne :
 *   0 = rien
 *   1 = Nouvelle Carte
 *   2 = Modifier Carte
 *   3 = Retour (ESC)
 */
public class EditorStartMenu {
    public static final int RESULT_NONE = 0;
    public static final int RESULT_NEW_MAP = 1;
    public static final int RESULT_MODIFY_MAP = 2;
    public static final int RESULT_BACK = 3;

    private boolean active = false;
    private int selection = 0; // 0 = Nouvelle, 1 = Modifier
    private final Texture background;
    private final float EXTRA_HEIGHT;

    public EditorStartMenu(float backgroundExtraHeight) {
        this.EXTRA_HEIGHT = backgroundExtraHeight;
        this.background = new Texture("Fond_simple.png");
    }

    public boolean isActive() { return active; }
    public void activate() { active = true; selection = 0; }
    public void deactivate() { active = false; }

    public int updateInput() {
        if (!active) return RESULT_NONE;

        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            selection = Math.min(1, selection + 1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            selection = Math.max(0, selection - 1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            return selection == 0 ? RESULT_NEW_MAP : RESULT_MODIFY_MAP;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            return RESULT_BACK;
        }
        return RESULT_NONE;
    }

    public void render(SpriteBatch batch, BitmapFont font) {
        if (!active) return;

        int sw = Gdx.graphics.getWidth();
        int sh = Gdx.graphics.getHeight();

        // Fond
        float bgW = background.getWidth();
        float bgH = background.getHeight();
        if (bgH <= 0) bgH = 1;
        float destHeight = sh + EXTRA_HEIGHT;
        float destWidth = (bgW / bgH) * destHeight;
        if (destWidth < 1f) destWidth = sw;

        for (float bx = 0; bx < sw; bx += destWidth) {
            batch.draw(background, bx, 0f, destWidth, destHeight);
        }

        // Boîte de menu
        float boxW = 420f;
        float boxH = 200f;
        float boxX = sw / 2f - boxW / 2f;
        float boxY = sh / 2f - boxH / 2f;

        font.getData().setScale(1.8f);
        font.draw(batch, "Editeur de Carte", boxX + 20, boxY + boxH - 25);
        font.getData().setScale(1f);

        String s0 = (selection == 0 ? "> Nouvelle Carte" : "  Nouvelle Carte");
        String s1 = (selection == 1 ? "> Modifier une Carte" : "  Modifier une Carte");

        font.draw(batch, s0, boxX + 40, boxY + boxH - 80);
        font.draw(batch, s1, boxX + 40, boxY + boxH - 120);
        font.draw(batch, "ESC pour revenir", boxX + 20, boxY + 30);
    }

    public void dispose() {
        if (background != null) background.dispose();
    }
}
