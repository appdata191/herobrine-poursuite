package com.github.herobrine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.ArrayList;
import java.util.List;

/**
 * Menu de sélection des niveaux.
 * Affiche les fichiers .txt trouvés dans assets/levels/ (via Gdx.files.local).
 *
 * updateInput() retourne :
 *   0 = rien
 *   1 = niveau sélectionné
 *   2 = retour au menu principal (via ESC)
 */
public class LevelSelectionMenu {
    public static final int RESULT_NONE = 0;
    public static final int RESULT_LEVEL_SELECTED = 1;
    public static final int RESULT_BACK_TO_MAIN = 2;

    private boolean active = false;
    private final Texture background;
    private final float EXTRA_HEIGHT;

    private final List<FileHandle> levelFiles = new ArrayList<>();
    private int selectionIndex = 0;
    private String selectedLevelPath = null;

    public LevelSelectionMenu(float backgroundExtraHeight) {
        this.EXTRA_HEIGHT = backgroundExtraHeight;
        this.background = new Texture("Fond_simple.png");
    }

    public boolean isActive() { return active; }

    public void activate() {
        active = true;
        selectionIndex = 0;
        selectedLevelPath = null;
        refreshLevelList();
    }

    public void deactivate() { active = false; }

    /**
     * Scanne le répertoire assets/levels pour les fichiers .txt.
     * Gdx.files.local est utilisé pour correspondre à l'emplacement de sauvegarde de CreateMap.
     */
    private void refreshLevelList() {
        levelFiles.clear();
        FileHandle levelsDir = Gdx.files.local("assets/levels/");
        if (levelsDir.exists() && levelsDir.isDirectory()) {
            for (FileHandle file : levelsDir.list(".txt")) {
                levelFiles.add(file);
            }
        }
    }

    public int updateInput() {
        if (!active) return RESULT_NONE;

        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            if (!levelFiles.isEmpty()) {
                selectionIndex = (selectionIndex + 1) % levelFiles.size();
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            if (!levelFiles.isEmpty()) {
                selectionIndex = (selectionIndex - 1 + levelFiles.size()) % levelFiles.size();
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            if (selectionIndex >= 0 && selectionIndex < levelFiles.size()) {
                selectedLevelPath = levelFiles.get(selectionIndex).path();
                return RESULT_LEVEL_SELECTED;
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            return RESULT_BACK_TO_MAIN;
        }
        return RESULT_NONE;
    }

    public void render(SpriteBatch batch, BitmapFont font) {
        if (!active) return;

        int sw = Gdx.graphics.getWidth();
        int sh = Gdx.graphics.getHeight();

        // Dessiner le fond
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
        float boxW = 500f;
        float boxH = 400f;
        float boxX = sw / 2f - boxW / 2f;
        float boxY = sh / 2f - boxH / 2f;

        font.getData().setScale(1.8f);
        font.draw(batch, "Selection du Niveau", boxX + 20, boxY + boxH - 25);
        font.getData().setScale(1f);

        if (levelFiles.isEmpty()) {
            font.draw(batch, "Aucun niveau trouve dans assets/levels/", boxX + 20, boxY + boxH - 80);
            font.draw(batch, "Creez-en un avec l'editeur !", boxX + 20, boxY + boxH - 110);
        } else {
            float currentY = boxY + boxH - 80;
            for (int i = 0; i < levelFiles.size(); i++) {
                String levelName = levelFiles.get(i).nameWithoutExtension();
                String entry = (i == selectionIndex ? "> " : "  ") + levelName;
                font.draw(batch, entry, boxX + 40, currentY);
                currentY -= 30;
                if (currentY < boxY + 20) break; // Pour éviter de déborder
            }
        }
        font.draw(batch, "ESC pour revenir", boxX + 20, boxY + 30);
    }

    public String getSelectedLevelPath() {
        return selectedLevelPath;
    }

    public void dispose() {
        if (background != null) background.dispose();
    }
}
