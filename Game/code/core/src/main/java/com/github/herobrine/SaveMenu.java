package com.github.herobrine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Menu d'enregistrement / sortie pour l'éditeur CreateMap.
 * - OPTIONS: Enregistrer (demande nom), Menu principal (avec confirmation), Quitter (avec confirmation)
 *
 * API:
 *   activate() / deactivate() ; isActive()
 *   updateInput() -> one of RESULT_* or 0
 *
 * RESULT_*:
 *   RESULT_NONE = 0
 *   RESULT_SAVED = 1 (saved and keep editing)
 *   RESULT_RETURN_MENU = 2 (confirmed return to launch menu)
 *   RESULT_QUIT = 3 (confirmed quit)
 */
public class SaveMenu {
    public static final int RESULT_NONE = 0;
    public static final int RESULT_SAVED = 1;
    public static final int RESULT_RETURN_MENU = 2;
    public static final int RESULT_QUIT = 3;

    private Texture overlayTex;
    private boolean active = false;
    private int menuSelection = 0; // 0 = Enregistrer, 1 = Menu principal, 2 = Quitter
    private final float extraHeight;

    // filename input
    private boolean typingName = false; // kept for compatibility but not used for per-frame capture
    private String filename = "";
    // input processor handling for in-game text entry
    private InputProcessor previousInputProcessor = null;
    private final InputAdapter typingProcessor = new InputAdapter() {
        @Override
        public boolean keyTyped(char character) {
            if (!typingName) return false;
            if (character == '\r' || character == '\n') return false;
            if (character == '\b') { // backspace
                if (filename.length() > 0) filename = filename.substring(0, filename.length() - 1);
                return true;
            }
            // accept basic filename chars
            if ((character >= 'a' && character <= 'z') ||
                (character >= 'A' && character <= 'Z') ||
                (character >= '0' && character <= '9') ||
                character == '-' || character == '_' || character == '.') {
                if (filename.length() < 64) filename += character;
                return true;
            }
            return false;
        }
    };

    // confirmation step
    private boolean confirmActive = false;
    private String confirmMsg = "";
    private int confirmAction = -1; // 2 return, 3 quit

    public SaveMenu(float extraHeight) {
        this.extraHeight = extraHeight;
        Pixmap pm = new Pixmap(1,1, Pixmap.Format.RGBA8888);
        pm.setColor(0f,0f,0f,0.6f);
        pm.fill();
        overlayTex = new Texture(pm);
        pm.dispose();
    }

    public void activate() {
        active = true;
        menuSelection = 0;
        typingName = false;
        confirmActive = false;
    }

    public void deactivate() {
        active = false;
        typingName = false;
        confirmActive = false;
    }

    public boolean isActive() { return active; }

    /**
     * updateInput() doit être appelé depuis CreateMap.
     * Retourne RESULT_* codes.
     */
    public int updateInput() {
        if (!active) return RESULT_NONE;

        // (removed legacy savedRequested check - using in-game typing now)

        if (confirmActive) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                // confirm action
                confirmActive = false;
                active = false;
                if (confirmAction == RESULT_RETURN_MENU) return RESULT_RETURN_MENU;
                else if (confirmAction == RESULT_QUIT) return RESULT_QUIT;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                confirmActive = false;
            }
            return RESULT_NONE;
        }

        // handle typing mode
        if (typingName) {
            // confirm save
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                typingName = false;
                // restore previous input processor
                Gdx.input.setInputProcessor(previousInputProcessor);
                previousInputProcessor = null;
                active = false;
                return RESULT_SAVED;
            }
            // cancel typing
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                typingName = false;
                filename = "";
                Gdx.input.setInputProcessor(previousInputProcessor);
                previousInputProcessor = null;
                return RESULT_NONE;
            }
            return RESULT_NONE;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            menuSelection = Math.min(2, menuSelection + 1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            menuSelection = Math.max(0, menuSelection - 1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            if (menuSelection == 0) {
                // start in-game typing: capture keyTyped events
                typingName = true;
                filename = "";
                previousInputProcessor = Gdx.input.getInputProcessor();
                Gdx.input.setInputProcessor(typingProcessor);
                return RESULT_NONE;
            } else if (menuSelection == 1) {
                // confirm return to launch menu
                confirmActive = true;
                confirmMsg = "Retour au menu ? (ENTER pour confirmer, ESC pour annuler)";
                confirmAction = RESULT_RETURN_MENU;
            } else {
                confirmActive = true;
                confirmMsg = "Quitter ? (ENTER pour confirmer, ESC pour annuler)";
                confirmAction = RESULT_QUIT;
            }
        }

        return RESULT_NONE;
    }

    public void render(SpriteBatch batch, BitmapFont font) {
        if (!active) return;
        int sw = Gdx.graphics.getWidth();
        int sh = Gdx.graphics.getHeight();

        // overlay
        batch.draw(overlayTex, 0, 0, sw, sh + extraHeight);

        float boxW = 420f;
        float boxH = 180f;
        float boxX = sw / 2f - boxW / 2f;
        float boxY = sh / 2f - boxH / 2f;

        batch.draw(overlayTex, boxX, boxY, boxW, boxH);

        font.getData().setScale(1.4f);
        font.draw(batch, "Sauvegarder / Quitter", boxX + 20, boxY + boxH - 20);
        font.getData().setScale(1f);

        String opt0 = (menuSelection == 0 ? "> Enregistrer" : "  Enregistrer");
        String opt1 = (menuSelection == 1 ? "> Menu principal" : "  Menu principal");
        String opt2 = (menuSelection == 2 ? "> Quitter" : "  Quitter");

        font.draw(batch, opt0, boxX + 40, boxY + boxH - 60);
        font.draw(batch, opt1, boxX + 40, boxY + boxH - 100);
        font.draw(batch, opt2, boxX + 40, boxY + boxH - 140);

        if (typingName) {
            // draw input rectangle
            float inW = boxW - 40f;
            float inH = 30f;
            float inX = boxX + 20f;
            float inY = boxY + 20f;
            // background rect (reuse overlayTex for flat color)
            batch.draw(overlayTex, inX, inY, inW, inH);
            font.draw(batch, "Nom fichier: " + filename + ( (System.currentTimeMillis()/500)%2==0 ? "_" : "" ), inX + 6f, inY + inH - 8f);
        }

        if (confirmActive) {
            font.draw(batch, confirmMsg, boxX + 20, boxY + 50);
        }
    }

    // allow CreateMap to read filename after RESULT_SAVED
    public String getFilename() { return filename; }

    public void dispose() {
        if (overlayTex != null) overlayTex.dispose();
    }
}