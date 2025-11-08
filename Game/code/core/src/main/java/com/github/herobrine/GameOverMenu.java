package com.github.herobrine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Menu de fin de partie : active un overlay et propose Recommencer / Menu principal / Quitter.
 * updateInput() retourne :
 *   0 = aucune action
 *   1 = restart demandé
 *   2 = quit demandé
 *   3 = retour menu demandé
 */
public class GameOverMenu {
    private final float overlayAlpha;
    private final float extraHeight;
    private final float boxW = 360f;
    private final float boxH = 200f;

    private Texture overlayTex;
    private boolean active = false;
    private int menuSelection = 0; // 0 = Recommencer, 1 = Menu principal, 2 = Quitter
    private String title = "Fin de partie";

    public GameOverMenu(float overlayAlpha, float extraHeight) {
        this.overlayAlpha = overlayAlpha;
        this.extraHeight = extraHeight;
        createOverlay();
    }

    private void createOverlay() {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(0f, 0f, 0f, overlayAlpha);
        pm.fill();
        overlayTex = new Texture(pm);
        pm.dispose();
    }

    public void activate(String title) {
        this.title = (title != null && !title.isEmpty()) ? title : "Fin de partie";
        active = true;
        menuSelection = 0;
    }

    public void deactivate() {
        active = false;
    }

    public boolean isActive() {
        return active;
    }

    /**
     * Handle navigation when active.
     * @return 0 none, 1 restart, 2 quit, 3 return to launch menu
     */
    public int updateInput() {
        if (!active) return 0;
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            menuSelection = Math.min(2, menuSelection + 1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            menuSelection = Math.max(0, menuSelection - 1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            if (menuSelection == 0) return 1;
            if (menuSelection == 1) return 3;
            return 2;
        }
        return 0;
    }

    public void render(SpriteBatch batch, BitmapFont font) {
        if (!active) return;

        int sw = Gdx.graphics.getWidth();
        int sh = Gdx.graphics.getHeight();

        // overlay full-screen with extra height
        batch.draw(overlayTex, 0, 0, sw, sh + extraHeight);

        float boxX = sw / 2f - boxW / 2f;
        float boxY = sh / 2f - boxH / 2f;

        // darker box
        batch.draw(overlayTex, boxX, boxY, boxW, boxH);

        font.getData().setScale(1.6f);
        font.draw(batch, title, boxX + 20, boxY + boxH - 20);
        font.getData().setScale(1f);

        String opt0 = (menuSelection == 0 ? "> Recommencer" : "  Recommencer");
        String opt1 = (menuSelection == 1 ? "> Menu principal" : "  Menu principal");
        String opt2 = (menuSelection == 2 ? "> Quitter" : "  Quitter");

        font.draw(batch, opt0, boxX + 40, boxY + boxH - 60);
        font.draw(batch, opt1, boxX + 40, boxY + boxH - 100);
        font.draw(batch, opt2, boxX + 40, boxY + boxH - 140);
    }

    public void dispose() {
        if (overlayTex != null) overlayTex.dispose();
    }
}