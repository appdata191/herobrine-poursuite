package com.github.herobrine;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private Skin uiSkin;

    // Textures
    private Texture background, blockTop, blockBottom, creeperTex, picsTex;

    // Objets du jeu
    private Carte carte;
    private Joueur joueur;

    // Écrans et Menus
    private GameLaunchMenuScreen launchMenu;
    private EditorStartMenuScreen editorStartMenu;
    private PauseMenuOverlay pauseMenu;
    private GameOverMenuOverlay gameOverMenu;
    private LevelSelectionMenuScreen levelSelectionMenu;
    private CreateMap createMap;

    // États
    private float cameraX = 0f;
    private boolean isEditing = false;
    private static final float BACKGROUND_EXTRA_HEIGHT = 60f;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();

        // Chargement des textures
        background = new Texture("Fond_simple.png");
        blockTop = new Texture("Bloc du dessus.png");
        blockBottom = new Texture("Bloc du dessous.png");
        creeperTex = new Texture("creeper.png");
        picsTex = new Texture("Picss.png");

        uiSkin = UIFactory.createSkin(font);
        carte = new Carte(blockTop, blockBottom);
        createMap = new CreateMap(BACKGROUND_EXTRA_HEIGHT, background, blockTop, blockBottom, creeperTex, picsTex);

        // Initialisation des écrans de menu
        launchMenu = new GameLaunchMenuScreen(uiSkin, this);
        editorStartMenu = new EditorStartMenuScreen(uiSkin, this);
        pauseMenu = new PauseMenuOverlay(uiSkin, this);
        gameOverMenu = new GameOverMenuOverlay(uiSkin, this);
        levelSelectionMenu = new LevelSelectionMenuScreen(uiSkin, this, LevelSelectionMenuScreen.SelectionMode.PLAY);
        launchMenu.activate();
    }

    // --- API Publique pour les Menus ---
    public void startLevelSelection(boolean forEditing) {
        this.isEditing = forEditing;
        if (isEditing) {
            levelSelectionMenu = new LevelSelectionMenuScreen(uiSkin, this, LevelSelectionMenuScreen.SelectionMode.EDIT);
        } else {
            levelSelectionMenu = new LevelSelectionMenuScreen(uiSkin, this, LevelSelectionMenuScreen.SelectionMode.PLAY);
        }
        levelSelectionMenu.activate();
    }

    public void returnToPreviousMenuFromSelection() {
        if (isEditing) {
            editorStartMenu.activate();
        } else {
            launchMenu.activate();
        }
    }

    public void onLevelSelected(String levelPath) {
        if (isEditing) {
            createMap.activate(levelPath);
        } else {
            initGame(levelPath);
        }
    }

    public void showEditorMenu() { editorStartMenu.activate(); }
    public void startEditor() { createMap.activate(null); }
    public void returnToLaunchMenu() { goToLaunchMenu(); }
    public void quitGame() { Gdx.app.exit(); }
    public void togglePause() {
        if (gameOverMenu.isActive()) return;
        if (pauseMenu.isActive()) {
            pauseMenu.deactivate();
        } else {
            pauseMenu.activate();
        }
    }
    public void restartLevel() {
        if (carte.getCurrentLevelPath() != null) {
            gameOverMenu.deactivate();
            initGame(carte.getCurrentLevelPath());
        } else {
            returnToLaunchMenu();
        }
    }
    private void triggerGameOver(String message) {
        if (gameOverMenu.isActive()) return;
        gameOverMenu.setMessage(message);
        gameOverMenu.activate();
    }

    private void initGame(String levelPath) {
        carte.create(levelPath);
        if (joueur != null) joueur.dispose();
        joueur = new Joueur(carte.getTile() * 5, carte.getSurfaceYAt(5));
        joueur.create();
        cameraX = 0f;
        if (pauseMenu.isActive()) pauseMenu.deactivate();
        if (gameOverMenu.isActive()) gameOverMenu.deactivate();
    }

    private void goToLaunchMenu() {
        if (joueur != null) {
            joueur.dispose();
            joueur = null;
        }
        carte.clear();
        
        launchMenu.deactivate();
        editorStartMenu.deactivate();
        levelSelectionMenu.deactivate();
        createMap.deactivate();
        pauseMenu.deactivate();
        gameOverMenu.deactivate();

        launchMenu.activate();
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (!isGameBlocked()) {
            updateGame(delta);
        }
        handleGlobalInput();

        batch.begin();
        drawBackground(); // MÉTHODE RÉINTÉGRÉE
        if (joueur != null) {
            carte.render(batch, cameraX);
            joueur.render(batch, cameraX);
            renderHUD(); // MÉTHODE RÉINTÉGRÉE
        }
        batch.end();

        // Rendu des écrans UI
        if (launchMenu.isActive()) launchMenu.render();
        if (editorStartMenu.isActive()) editorStartMenu.render();
        if (pauseMenu.isActive()) pauseMenu.render();
        if (gameOverMenu.isActive()) gameOverMenu.render();
        if (levelSelectionMenu.isActive()) levelSelectionMenu.render();
        
        if (createMap.isActive()) {
            if (createMap.updateInput() == 1) goToLaunchMenu();
            createMap.render(batch, font);
        }
    }

    private boolean isGameBlocked() {
        return launchMenu.isActive() || editorStartMenu.isActive() || levelSelectionMenu.isActive() ||
               createMap.isActive() || pauseMenu.isActive() || gameOverMenu.isActive();
    }

    private void handleGlobalInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) && joueur != null) {
            togglePause();
        }
    }

    private void updateGame(float delta) {
        joueur.update(delta, carte);
        updateCamera();
        if (carte.updateAutomates(delta, joueur)) triggerGameOver("Vous vous etes fait tuer !");
        if (joueur.getX() + joueur.getWidth() >= carte.getMapWidth()) triggerGameOver("Vous avez gagné !");
        if (joueur.getY() < 1) triggerGameOver("Vous etes tombe dans un gouffre !");
    }

    // --- MÉTHODES MANQUANTES RÉINTÉGRÉES ---

    private void drawBackground() {
        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();
        float destHeight = screenH + BACKGROUND_EXTRA_HEIGHT;
        float destWidth = (background.getWidth() / (float)background.getHeight()) * destHeight;
        float parallaxFactor = 0.5f;
        float backgroundCamX = (carte != null && joueur != null) ? cameraX * parallaxFactor : 0;
        float mapWidth = (carte != null && joueur != null) ? carte.getMapWidth() : screenW;

        for (float x = 0; x < mapWidth + destWidth; x += destWidth) {
            batch.draw(background, x - backgroundCamX, 0, destWidth, destHeight);
        }
    }

    private void updateCamera() {
        if (joueur == null || carte == null) return;
        float screenW = Gdx.graphics.getWidth();
        cameraX = joueur.getX() + joueur.getWidth() / 2f - screenW / 2f;
        if (cameraX < 0) cameraX = 0;
        float maxCam = Math.max(0, carte.getMapWidth() - screenW);
        if (cameraX > maxCam) cameraX = maxCam;
    }

    private void renderHUD() {
        if (joueur == null) return;
        font.setColor(Color.WHITE);
        font.draw(batch, "Temps : " + (int)(joueur.getElapsedTime()) + "s", 20, Gdx.graphics.getHeight() - 20);
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        font.dispose();
        uiSkin.dispose();

        background.dispose();
        blockTop.dispose();
        blockBottom.dispose();
        creeperTex.dispose();
        picsTex.dispose();

        if (joueur != null) joueur.dispose();
        carte.dispose();
        launchMenu.dispose();
        editorStartMenu.dispose();
        pauseMenu.dispose();
        gameOverMenu.dispose();
        levelSelectionMenu.dispose();
        createMap.dispose();
    }
}
