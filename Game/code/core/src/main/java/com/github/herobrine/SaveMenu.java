package com.github.herobrine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

public class SaveMenu {
    private boolean active = false;
    private final Texture overlayBackground;
    private final Texture whiteTexture;
    private final Texture goldTexture;
    private final Texture inputBgTexture;
    private final float EXTRA_HEIGHT;

    private enum SaveMenuState { MAIN_OPTIONS, SAVE_AS_PROMPT }
    private SaveMenuState currentState = SaveMenuState.MAIN_OPTIONS;

    private final Rectangle resumeButton;
    private final Rectangle saveAsButton;
    private final Rectangle saveButton;
    private final Rectangle returnMenuButton;
    private final Rectangle quitButton;

    private final Rectangle filenameInputBox;
    private final Rectangle validateButton;
    private final Rectangle cancelButton;

    private String filenameInput = "";
    private String loadedLevelName = null;
    private final GlyphLayout layout = new GlyphLayout();

    public static final int RESULT_NONE = 0;
    public static final int RESULT_RESUME_EDITING = 1;
    public static final int RESULT_SAVE_AS_VALIDATE = 3;
    public static final int RESULT_SAVE_DIRECT = 4;
    public static final int RESULT_RETURN_MENU = 5;
    public static final int RESULT_QUIT = 6;
    public static final int RESULT_SAVE_AS_CANCEL = 7;

    public SaveMenu(float extraHeight) {
        this.EXTRA_HEIGHT = extraHeight;
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(0.1f, 0.1f, 0.2f, 0.8f);
        pm.fill();
        overlayBackground = new Texture(pm);
        
        pm.setColor(Color.WHITE);
        pm.fill();
        whiteTexture = new Texture(pm);

        pm.setColor(Color.GOLD);
        pm.fill();
        goldTexture = new Texture(pm);

        pm.setColor(0.2f, 0.2f, 0.25f, 1f);
        pm.fill();
        inputBgTexture = new Texture(pm);
        pm.dispose();

        float width = 400;
        float height = 50;
        float screenW = Gdx.graphics.getWidth();
        float centerX = screenW / 2f;
        
        float currentY = Gdx.graphics.getHeight() / 2f + 150f;
        resumeButton = new Rectangle(centerX - width / 2, currentY, width, height);
        currentY -= (height + 20);
        saveAsButton = new Rectangle(centerX - width / 2, currentY, width, height);
        currentY -= (height + 20);
        saveButton = new Rectangle(centerX - width / 2, currentY, width, height);
        currentY -= (height + 20);
        returnMenuButton = new Rectangle(centerX - width / 2, currentY, width, height);
        currentY -= (height + 20);
        quitButton = new Rectangle(centerX - width / 2, currentY, width, height);

        currentY = Gdx.graphics.getHeight() / 2f + 50f;
        filenameInputBox = new Rectangle(centerX - width / 2, currentY, width, height);
        currentY -= (height + 20);
        validateButton = new Rectangle(centerX - width / 2, currentY, width / 2 - 10, height);
        cancelButton = new Rectangle(centerX + 10, currentY, width / 2 - 10, height);
    }

    public void activate(String currentLoadedLevelName) {
        active = true;
        this.loadedLevelName = currentLoadedLevelName;
        this.filenameInput = "";
        this.currentState = SaveMenuState.MAIN_OPTIONS;
    }

    public void deactivate() {
        active = false;
    }

    public boolean isActive() {
        return active;
    }

    public String getFilenameInput() {
        return filenameInput;
    }

    public int updateInput() {
        if (!active) return RESULT_NONE;

        if (currentState == SaveMenuState.SAVE_AS_PROMPT) {
            handleTextInput();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (currentState == SaveMenuState.SAVE_AS_PROMPT) {
                currentState = SaveMenuState.MAIN_OPTIONS;
                filenameInput = "";
                return RESULT_SAVE_AS_CANCEL;
            } else {
                return RESULT_RESUME_EDITING;
            }
        }

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            int mx = Gdx.input.getX();
            int my = Gdx.graphics.getHeight() - Gdx.input.getY();

            if (currentState == SaveMenuState.MAIN_OPTIONS) {
                if (resumeButton.contains(mx, my)) return RESULT_RESUME_EDITING;
                if (saveAsButton.contains(mx, my)) {
                    currentState = SaveMenuState.SAVE_AS_PROMPT;
                    filenameInput = "";
                    return RESULT_NONE;
                }
                if (saveButton.contains(mx, my)) {
                    if (loadedLevelName == null) {
                        currentState = SaveMenuState.SAVE_AS_PROMPT;
                        filenameInput = "";
                        return RESULT_NONE;
                    } else {
                        return RESULT_SAVE_DIRECT;
                    }
                }
                if (returnMenuButton.contains(mx, my)) return RESULT_RETURN_MENU;
                if (quitButton.contains(mx, my)) return RESULT_QUIT;
            } else if (currentState == SaveMenuState.SAVE_AS_PROMPT) {
                if (validateButton.contains(mx, my) && !filenameInput.trim().isEmpty()) {
                    return RESULT_SAVE_AS_VALIDATE;
                }
                if (cancelButton.contains(mx, my)) {
                    currentState = SaveMenuState.MAIN_OPTIONS;
                    filenameInput = "";
                    return RESULT_SAVE_AS_CANCEL;
                }
            }
        }
        return RESULT_NONE;
    }

    private void handleTextInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE) && !filenameInput.isEmpty()) {
            filenameInput = filenameInput.substring(0, filenameInput.length() - 1);
        } else {
            for (int i = Input.Keys.A; i <= Input.Keys.Z; i++) {
                if (Gdx.input.isKeyJustPressed(i)) filenameInput += Input.Keys.toString(i).toLowerCase();
            }
            for (int i = Input.Keys.NUM_0; i <= Input.Keys.NUM_9; i++) {
                if (Gdx.input.isKeyJustPressed(i)) filenameInput += Input.Keys.toString(i);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.MINUS)) filenameInput += "-";
        }
    }

    public void render(SpriteBatch batch, ShapeRenderer shapeRenderer, BitmapFont font) {
        if (!active) return;

        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();
        float centerX = screenW / 2f;
        
        batch.begin();
        batch.setColor(1, 1, 1, 1);
        batch.draw(overlayBackground, 0, 0, screenW, screenH + EXTRA_HEIGHT);

        if (currentState == SaveMenuState.MAIN_OPTIONS) {
            drawButton(batch, font, resumeButton, "Reprendre", false);
            drawButton(batch, font, saveAsButton, "Enregistrer sous", false);
            drawButton(batch, font, saveButton, (loadedLevelName == null ? "Enregistrer (nouveau)" : "Enregistrer"), false);
            drawButton(batch, font, returnMenuButton, "Retour au menu principal", false);
            drawButton(batch, font, quitButton, "Quitter le jeu", false);
        } else if (currentState == SaveMenuState.SAVE_AS_PROMPT) {
            font.setColor(Color.WHITE);
            layout.setText(font, "Enregistrer sous");
            font.draw(batch, layout, centerX - layout.width / 2, Gdx.graphics.getHeight() / 2f + 120f);

            drawTextInput(batch, font, filenameInputBox, "Nom du fichier...");
            
            drawButton(batch, font, validateButton, "Valider", filenameInput.trim().isEmpty());
            drawButton(batch, font, cancelButton, "Annuler", false);
        }
        
        font.setColor(Color.WHITE);
        batch.end();
    }

    private void drawButton(SpriteBatch batch, BitmapFont font, Rectangle rect, String text, boolean disabled) {
        float borderThickness = 4f;
        batch.setColor(Color.GOLD);
        batch.draw(goldTexture, rect.x, rect.y, rect.width, rect.height);
        batch.setColor(Color.WHITE);
        batch.draw(whiteTexture, rect.x + borderThickness, rect.y + borderThickness, rect.width - 2 * borderThickness, rect.height - 2 * borderThickness);
        
        font.setColor(disabled ? Color.GRAY : Color.BLACK);
        layout.setText(font, text);
        // CORRIGÉ : Utilisation de la chaîne de caractères `text` au lieu de `layout`
        font.draw(batch, text, rect.x + (rect.width - layout.width) / 2, rect.y + (rect.height + layout.height) / 2);
    }
    
    private void drawTextInput(SpriteBatch batch, BitmapFont font, Rectangle rect, String placeholder) {
        float borderThickness = 4f;
        batch.setColor(Color.GOLD);
        batch.draw(goldTexture, rect.x, rect.y, rect.width, rect.height);
        batch.setColor(Color.DARK_GRAY);
        batch.draw(inputBgTexture, rect.x + borderThickness, rect.y + borderThickness, rect.width - 2 * borderThickness, rect.height - 2 * borderThickness);

        String textToDraw;
        if (filenameInput.isEmpty()) {
            font.setColor(Color.LIGHT_GRAY);
            textToDraw = placeholder;
        } else {
            font.setColor(Color.WHITE);
            textToDraw = filenameInput;
        }
        layout.setText(font, textToDraw);
        // CORRIGÉ : Utilisation de la chaîne de caractères `textToDraw` au lieu de `layout`
        font.draw(batch, textToDraw, rect.x + (rect.width - layout.width) / 2, rect.y + (rect.height + layout.height) / 2);
    }

    public void dispose() {
        if (overlayBackground != null) overlayBackground.dispose();
        if (whiteTexture != null) whiteTexture.dispose();
        if (goldTexture != null) goldTexture.dispose();
        if (inputBgTexture != null) inputBgTexture.dispose();
    }
}
