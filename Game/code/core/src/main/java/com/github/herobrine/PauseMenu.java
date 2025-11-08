package com.github.herobrine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class PauseMenu {
    private Texture overlayTex;
    private int menuSelection = 0; // 0 = Continuer, 1 = Menu principal, 2 = Quitter
    private boolean paused = false;

    private final float overlayAlpha;
    private final float backgroundExtraHeight;
    private final float menuBoxW = 320f;
    private final float menuBoxH = 180f;

    public PauseMenu(float overlayAlpha, float backgroundExtraHeight) {
        this.overlayAlpha = overlayAlpha;
        this.backgroundExtraHeight = backgroundExtraHeight;
        createOverlay();
    }

    private void createOverlay() {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(0f, 0f, 0f, overlayAlpha);
        pm.fill();
        overlayTex = new Texture(pm);
        pm.dispose();
    }

    public void toggle() {
        paused = !paused;
        if (paused) menuSelection = 0;
    }

    public void deactivate() {
        paused = false;
    }

    public boolean isPaused() {
        return paused;
    }

    /**
     * Handle navigation while paused.
     * @return 0 = none, 1 = continue, 2 = quit, 3 = return to launch menu
     */
    public int updateInput() {
        if (!paused) return 0;
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            menuSelection = Math.min(2, menuSelection + 1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            menuSelection = Math.max(0, menuSelection - 1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            if (menuSelection == 0) {
                paused = false;
                return 1;
            } else if (menuSelection == 1) {
                paused = false;
                return 3;
            } else {
                return 2;
            }
        }
        return 0;
    }

    public void render(SpriteBatch batch, BitmapFont font) {
        if (!paused) return;
        int sw = Gdx.graphics.getWidth();
        int sh = Gdx.graphics.getHeight();

        // overlay full-screen with extra height (aligned bottom)
        batch.draw(overlayTex, 0, 0, sw, sh + backgroundExtraHeight);

        float boxX = sw / 2f - menuBoxW / 2f;
        float boxY = sh / 2f - menuBoxH / 2f;

        // darker box
        batch.draw(overlayTex, boxX, boxY, menuBoxW, menuBoxH);

        font.getData().setScale(1.4f);
        font.draw(batch, "Pause", boxX + 20, boxY + menuBoxH - 20);
        font.getData().setScale(1f);

        String opt0 = (menuSelection == 0 ? "> Continuer" : "  Continuer");
        String opt1 = (menuSelection == 1 ? "> Menu principal" : "  Menu principal");
        String opt2 = (menuSelection == 2 ? "> Quitter" : "  Quitter");

        font.draw(batch, opt0, boxX + 40, boxY + menuBoxH - 60);
        font.draw(batch, opt1, boxX + 40, boxY + menuBoxH - 100);
        font.draw(batch, opt2, boxX + 40, boxY + menuBoxH - 140);
    }

    public void dispose() {
        if (overlayTex != null) overlayTex.dispose();
    }
}