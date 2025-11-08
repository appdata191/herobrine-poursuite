package com.github.herobrine;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private BitmapFont font;

    private Carte carte;
    private Joueur joueur;

    private float cameraX = 0f;
    private boolean gameOver = false;
    private String gameOverMessage = null;

    private PauseMenu pauseMenu;
    private GameOverMenu gameOverMenu;
    private GameLaunchMenu launchMenu;
    private LevelSelectionMenu levelSelectionMenu;
    private EditorStartMenu editorStartMenu;

    private boolean isEditing = false;

    private static final float OVERLAY_ALPHA = 0.6f;
    private static final float BACKGROUND_EXTRA_HEIGHT = 60f;

    private CreateMap createMap;

    @Override
    public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont();

        launchMenu = new GameLaunchMenu(BACKGROUND_EXTRA_HEIGHT);
        pauseMenu = new PauseMenu(OVERLAY_ALPHA, BACKGROUND_EXTRA_HEIGHT);
        gameOverMenu = new GameOverMenu(OVERLAY_ALPHA, BACKGROUND_EXTRA_HEIGHT);
        createMap = new CreateMap(BACKGROUND_EXTRA_HEIGHT);
        levelSelectionMenu = new LevelSelectionMenu(BACKGROUND_EXTRA_HEIGHT);
        editorStartMenu = new EditorStartMenu(BACKGROUND_EXTRA_HEIGHT);
    }

    private void initGame(String levelPath) {
        if (carte != null) carte.dispose();
        if (joueur != null) joueur.dispose();

        carte = new Carte();
        carte.create(levelPath);

        final int startTileX = 5;
        float startX = startTileX * carte.getTile();
        float startY = carte.getSurfaceYAt(startTileX);

        joueur = new Joueur(startX, startY);
        joueur.create();

        cameraX = 0f;
        gameOver = false;
        gameOverMessage = null;
        if (gameOverMenu != null) gameOverMenu.deactivate();
        if (pauseMenu != null && pauseMenu.isPaused()) pauseMenu.deactivate();
    }

    private void goToLaunchMenu() {
        if (carte != null) carte.dispose();
        if (joueur != null) joueur.dispose();
        
        gameOver = false;
        isEditing = false;
        if (pauseMenu != null) pauseMenu.deactivate();
        if (gameOverMenu != null) gameOverMenu.deactivate();
        if (levelSelectionMenu != null) levelSelectionMenu.deactivate();
        if (editorStartMenu != null) editorStartMenu.deactivate();
        if (createMap != null) createMap.deactivate();

        if (launchMenu != null) launchMenu.activate();
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        if (delta <= 0) return;

        if (launchMenu.isActive()) {
            int action = launchMenu.updateInput();
            if (action == 1) {
                isEditing = false;
                launchMenu.deactivate();
                levelSelectionMenu.activate();
            } else if (action == 2) {
                isEditing = true;
                launchMenu.deactivate();
                editorStartMenu.activate();
            }
        } else if (editorStartMenu.isActive()) {
            int action = editorStartMenu.updateInput();
            if (action == EditorStartMenu.RESULT_NEW_MAP) {
                editorStartMenu.deactivate();
                createMap.activate(null);
            } else if (action == EditorStartMenu.RESULT_MODIFY_MAP) {
                editorStartMenu.deactivate();
                levelSelectionMenu.activate();
            } else if (action == EditorStartMenu.RESULT_BACK) {
                editorStartMenu.deactivate();
                launchMenu.activate();
            }
        } else if (levelSelectionMenu.isActive()) {
            int action = levelSelectionMenu.updateInput();
            if (action == LevelSelectionMenu.RESULT_LEVEL_SELECTED) {
                String levelPath = levelSelectionMenu.getSelectedLevelPath();
                levelSelectionMenu.deactivate();
                
                if (isEditing) {
                    createMap.activate(levelPath);
                } else {
                    initGame(levelPath);
                }
            } else if (action == LevelSelectionMenu.RESULT_BACK_TO_MAIN) {
                levelSelectionMenu.deactivate();
                if (isEditing) {
                    editorStartMenu.activate();
                } else {
                    launchMenu.activate();
                }
            }
        } else if (createMap.isActive()) {
            int result = createMap.updateInput();
            if (result == 1) goToLaunchMenu();
            else if (result == 2) Gdx.app.exit();
        } else {
            if (pauseMenu.isPaused()) {
                int action = pauseMenu.updateInput();
                if (action == 2) Gdx.app.exit();
                else if (action == 3) goToLaunchMenu();
            }

            if (!pauseMenu.isPaused() && !gameOver) {
                updateGame(delta);
                boolean killed = (carte != null && joueur != null) ? carte.updateAutomates(delta, joueur) : false;
                if (killed) {
                    gameOver = true;
                    gameOverMessage = "Vous vous etes fait tuer";
                    if (gameOverMenu != null) gameOverMenu.activate(gameOverMessage);
                }
            }

            updateCamera();

            if (gameOver && gameOverMenu.isActive()) {
                int action = gameOverMenu.updateInput();
                if (action == 1) {
                    if (carte != null && carte.getCurrentLevelPath() != null) {
                        initGame(carte.getCurrentLevelPath());
                    } else {
                        goToLaunchMenu();
                    }
                }
                else if (action == 2) Gdx.app.exit();
                else if (action == 3) goToLaunchMenu();
            }
        }

        Gdx.gl.glClearColor(0.1f, 0.1f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        if (launchMenu.isActive()) {
            launchMenu.render(batch, font);
        } else if (editorStartMenu.isActive()) {
            editorStartMenu.render(batch, font);
        } else if (levelSelectionMenu.isActive()) {
            levelSelectionMenu.render(batch, font);
        } else if (createMap.isActive()) {
            createMap.render(batch, font);
        } else {
            if (carte != null) carte.render(batch, cameraX);
            if (joueur != null) joueur.render(batch, cameraX);
            renderHUD();
            if (pauseMenu != null) pauseMenu.render(batch, font);
            if (gameOverMenu != null) gameOverMenu.render(batch, font);
        }
        batch.end();
    }

    private void handleTogglePause() {
        if (launchMenu.isActive()) return;
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) && !gameOver) {
            pauseMenu.toggle();
        }
    }

    /**
     * MODIFIÉ : Ajout de la détection de chute mortelle.
     */
    private void updateGame(float delta) {
        if (joueur != null && carte != null) {
            handleTogglePause();
            joueur.update(delta, carte);

            // Condition de victoire : atteindre la fin de la carte
            if (joueur.getX() + joueur.getWidth() >= carte.getMapWidth()) {
                gameOver = true;
                gameOverMessage = "Fin de partie";
                if (gameOverMenu != null) gameOverMenu.activate(gameOverMessage);
            }

            // NOUVEAU : Condition de défaite par chute
            // Si le bas du joueur est bien en dessous de l'écran (ex: -200 pixels)
            if (joueur.getY() + joueur.getHeight() < -200) {
                gameOver = true;
                gameOverMessage = "Vous etes tombe dans un gouffre";
                if (gameOverMenu != null) gameOverMenu.activate(gameOverMessage);
            }
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
        font.draw(batch, "Temps : " + (int)(joueur.getElapsedTime()) + "s", 20, Gdx.graphics.getHeight() - 20);
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        if (joueur != null) joueur.dispose();
        if (carte != null) carte.dispose();
        if (pauseMenu != null) pauseMenu.dispose();
        if (gameOverMenu != null) gameOverMenu.dispose();
        if (launchMenu != null) launchMenu.dispose();
        if (levelSelectionMenu != null) levelSelectionMenu.dispose();
        if (editorStartMenu != null) editorStartMenu.dispose();
        if (createMap != null) createMap.dispose();
    }
}
