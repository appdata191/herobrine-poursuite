package com.github.herobrine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Menu de lancement : fond identique à la carte ("Fond_simple.png").
 * updateInput() retourne : 0 = aucune action, 1 = lancer partie, 2 = créer carte.
 */
public class GameLaunchMenu {
    private boolean active = true;
    private int selection = 0; // 0 = Lancer une partie, 1 = Créer une carte
    private final Texture background;
    private final float EXTRA_HEIGHT;

    public GameLaunchMenu(float backgroundExtraHeight) {
        this.EXTRA_HEIGHT = backgroundExtraHeight;
        this.background = new Texture("Fond_simple.png");
    }

    public boolean isActive() { return active; }
    public void activate() { active = true; selection = 0; }
    public void deactivate() { active = false; }

    /**
     * Handle navigation when active.
     * @return 0 none, 1 start game, 2 create map
     */
    public int updateInput() {
        if (!active) return 0;
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            selection = Math.min(1, selection + 1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            selection = Math.max(0, selection - 1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            return selection == 0 ? 1 : 2;
        }
        return 0;
    }

    public void render(SpriteBatch batch, BitmapFont font) {
        if (!active) return;

        int sw = Gdx.graphics.getWidth();
        int sh = Gdx.graphics.getHeight();

        // draw background tiled similarly to Carte.render, with EXTRA_HEIGHT
        float bgW = background.getWidth();
        float bgH = background.getHeight();
        if (bgH <= 0) bgH = 1;
        float destHeight = sh + EXTRA_HEIGHT;
        float destWidth = (bgW / bgH) * destHeight;
        if (destWidth < 1f) destWidth = sw;

        for (float bx = 0; bx < sw; bx += destWidth) {
            batch.draw(background, bx, 0f, destWidth, destHeight);
        }

        // menu box
        float boxW = 420f;
        float boxH = 180f;
        float boxX = sw / 2f - boxW / 2f;
        float boxY = sh / 2f - boxH / 2f;

        font.getData().setScale(2f);
        font.draw(batch, "Herobrine Poursuite", boxX + 12, boxY + boxH - 25);
        font.getData().setScale(1f);

        String s0 = (selection == 0 ? "> Lancer une partie" : "  Lancer une partie");
        String s1 = (selection == 1 ? "> Creer une carte" : "  Creer une carte");

        font.draw(batch, s0, boxX + 24, boxY + boxH - 80);
        font.draw(batch, s1, boxX + 24, boxY + boxH - 120);
    }

    public void dispose() {
        if (background != null) background.dispose();
    }
}
