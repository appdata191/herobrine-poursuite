package com.github.herobrine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class SaveMenu {
    public static final int RESULT_NONE = 0;
    public static final int RESULT_SAVED = 1;
    public static final int RESULT_RETURN_MENU = 2;
    public static final int RESULT_QUIT = 3;

    private Texture overlayTex;
    private boolean active = false;
    private int menuSelection = 0;
    private final float extraHeight;

    // MODIFIÉ : Contexte de sauvegarde
    private String levelToOverwrite = null; // Nom du fichier en cours de modif
    private final String[] optionsNewMap = {"Enregistrer sous...", "Menu principal", "Quitter"};
    private final String[] optionsModifyMap = {"Ecraser", "Enregistrer sous...", "Menu principal", "Quitter"};
    private String[] currentOptions;

    private boolean typingName = false;
    private String filename = "";
    private InputProcessor previousInputProcessor = null;
    private final InputAdapter typingProcessor = new InputAdapter() {
        @Override
        public boolean keyTyped(char character) {
            if (!typingName) return false;
            if (character == '\r' || character == '\n') return false;
            if (character == '\b') {
                if (filename.length() > 0) filename = filename.substring(0, filename.length() - 1);
                return true;
            }
            if ((character >= 'a' && character <= 'z') ||
                (character >= 'A' && character <= 'Z') ||
                (character >= '0' && character <= '9') ||
                character == '-' || character == '_') { // Point retiré pour éviter les confusions
                if (filename.length() < 64) filename += character;
                return true;
            }
            return false;
        }
    };

    private boolean confirmActive = false;
    private String confirmMsg = "";
    private int confirmAction = -1;

    public SaveMenu(float extraHeight) {
        this.extraHeight = extraHeight;
        Pixmap pm = new Pixmap(1,1, Pixmap.Format.RGBA8888);
        pm.setColor(0f,0f,0f,0.6f);
        pm.fill();
        overlayTex = new Texture(pm);
        pm.dispose();
    }

    // MODIFIÉ : La méthode d'activation prend le contexte
    public void activate(String loadedLevelName) {
        this.levelToOverwrite = loadedLevelName;
        if (this.levelToOverwrite != null) {
            currentOptions = optionsModifyMap;
        } else {
            currentOptions = optionsNewMap;
        }
        active = true;
        menuSelection = 0;
        typingName = false;
        confirmActive = false;
    }

    public void deactivate() {
        active = false;
        typingName = false;
        confirmActive = false;
        levelToOverwrite = null;
    }

    public boolean isActive() { return active; }

    public int updateInput() {
        if (!active) return RESULT_NONE;

        if (confirmActive) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                confirmActive = false;
                active = false;
                return confirmAction;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                confirmActive = false;
            }
            return RESULT_NONE;
        }

        if (typingName) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                typingName = false;
                Gdx.input.setInputProcessor(previousInputProcessor);
                previousInputProcessor = null;
                // On ne désactive pas le menu, on retourne le résultat
                // active = false;
                return RESULT_SAVED;
            }
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
            menuSelection = Math.min(currentOptions.length - 1, menuSelection + 1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            menuSelection = Math.max(0, menuSelection - 1);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            String selectedOption = currentOptions[menuSelection];

            switch (selectedOption) {
                case "Ecraser":
                    // On pré-remplit le nom de fichier et on retourne "SAVED"
                    this.filename = this.levelToOverwrite;
                    return RESULT_SAVED;
                case "Enregistrer sous...":
                    // On active le mode de saisie de texte
                    typingName = true;
                    filename = "";
                    previousInputProcessor = Gdx.input.getInputProcessor();
                    Gdx.input.setInputProcessor(typingProcessor);
                    return RESULT_NONE;
                case "Menu principal":
                    confirmActive = true;
                    confirmMsg = "Retour au menu ? (ENTER pour confirmer, ESC pour annuler)";
                    confirmAction = RESULT_RETURN_MENU;
                    break;
                case "Quitter":
                    confirmActive = true;
                    confirmMsg = "Quitter ? (ENTER pour confirmer, ESC pour annuler)";
                    confirmAction = RESULT_QUIT;
                    break;
            }
        }

        return RESULT_NONE;
    }

    public void render(SpriteBatch batch, BitmapFont font) {
        if (!active) return;
        int sw = Gdx.graphics.getWidth();
        int sh = Gdx.graphics.getHeight();

        batch.draw(overlayTex, 0, 0, sw, sh + extraHeight);

        float boxW = 420f;
        float boxH = (levelToOverwrite != null) ? 220f : 180f; // Boîte plus grande si on modifie
        float boxX = sw / 2f - boxW / 2f;
        float boxY = sh / 2f - boxH / 2f;

        batch.draw(overlayTex, boxX, boxY, boxW, boxH);

        font.getData().setScale(1.4f);
        font.draw(batch, "Sauvegarder / Quitter", boxX + 20, boxY + boxH - 20);
        font.getData().setScale(1f);

        float yPos = boxY + boxH - 60;
        for (int i = 0; i < currentOptions.length; i++) {
            String optionText = currentOptions[i];
            if (optionText.equals("Ecraser") && levelToOverwrite != null) {
                optionText = "Ecraser '" + levelToOverwrite + "'";
            }
            String finalOption = (menuSelection == i ? "> " : "  ") + optionText;
            font.draw(batch, finalOption, boxX + 40, yPos);
            yPos -= 40;
        }

        if (typingName) {
            float inW = boxW - 40f;
            float inH = 30f;
            float inX = boxX + 20f;
            float inY = boxY + 20f;
            batch.draw(overlayTex, inX, inY, inW, inH);
            font.draw(batch, "Nom fichier: " + filename + ((System.currentTimeMillis()/500)%2==0 ? "_" : ""), inX + 6f, inY + inH - 8f);
        }

        if (confirmActive) {
            font.draw(batch, confirmMsg, boxX + 20, boxY + 50);
        }
    }

    public String getFilename() { return filename; }

    public void dispose() {
        if (overlayTex != null) overlayTex.dispose();
    }
}
