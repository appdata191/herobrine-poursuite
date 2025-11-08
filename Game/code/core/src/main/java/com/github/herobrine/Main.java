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
    private String gameOverMessage = null; // message de fin (ex: mort)

    // menus
    private PauseMenu pauseMenu;
    private GameOverMenu gameOverMenu;
    private GameLaunchMenu launchMenu;

    // configuration / constants
    private static final float OVERLAY_ALPHA = 0.6f;
    private static final float BACKGROUND_EXTRA_HEIGHT = 60f; // hauteur supplémentaire appliquée au fond

    // add field
    private CreateMap createMap;

    @Override
    public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont();

        // launch menu active at start
        launchMenu = new GameLaunchMenu(BACKGROUND_EXTRA_HEIGHT);
        pauseMenu = new PauseMenu(OVERLAY_ALPHA, BACKGROUND_EXTRA_HEIGHT);
        gameOverMenu = new GameOverMenu(OVERLAY_ALPHA, BACKGROUND_EXTRA_HEIGHT);
        createMap = new CreateMap(BACKGROUND_EXTRA_HEIGHT);
        // game not initialized until player starts a partie
    }

    private void initGame() {
        if (carte != null) carte.dispose();
        if (joueur != null) joueur.dispose();

        carte = new Carte();
        carte.create();

        joueur = new Joueur(carte.getGroundY());
        joueur.create();

        cameraX = 0f;
        gameOver = false;
        gameOverMessage = null;
        if (gameOverMenu != null) gameOverMenu.deactivate();
        if (pauseMenu != null && pauseMenu.isPaused()) pauseMenu.deactivate();
    }

    private void goToLaunchMenu() {
        // cleanup current game and show launch menu
        if (carte != null) {
            carte.dispose();
            carte = null;
        }
        if (joueur != null) {
            joueur.dispose();
            joueur = null;
        }
        gameOver = false;
        gameOverMessage = null;
        if (pauseMenu != null) pauseMenu.deactivate();
        if (gameOverMenu != null) gameOverMenu.deactivate();
        if (launchMenu != null) launchMenu.activate();
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        if (delta <= 0) return;

        // if launch menu active -> handle it first
        if (launchMenu != null && launchMenu.isActive()) {
            int action = launchMenu.updateInput();
            if (action == 1) { // start game
                launchMenu.deactivate();
                initGame();
            } else if (action == 2) { // create map -> open editor
                launchMenu.deactivate();
                createMap.activate();
            }
        } else if (createMap != null && createMap.isActive()) {
            // delegate to map editor (CreateMap now handles its SaveMenu and performs saving itself)
            int result = createMap.updateInput();
            if (result == 1) goToLaunchMenu();
            else if (result == 2) Gdx.app.exit();
        } else {
            // handle pause menu input
            if (pauseMenu != null && pauseMenu.isPaused()) {
                int action = pauseMenu.updateInput();
                if (action == 1) {
                    // continue -> nothing else
                } else if (action == 2) {
                    Gdx.app.exit();
                    return;
                } else if (action == 3) {
                    goToLaunchMenu();
                }
            }

            if (!pauseMenu.isPaused() && !gameOver) {
                updateGame(delta);
                boolean killed = (carte != null && joueur != null) ? carte.updateAutomates(delta, joueur) : false;
                if (killed) {
                    gameOver = true;
                    gameOverMessage = "Vous vous êtes fait tuer";
                    if (gameOverMenu != null) gameOverMenu.activate(gameOverMessage);
                }
            }

            updateCamera();

            // when game over, handle gameOverMenu input
            if (gameOver && gameOverMenu != null && gameOverMenu.isActive()) {
                int action = gameOverMenu.updateInput();
                if (action == 1) initGame();
                else if (action == 2) Gdx.app.exit();
                else if (action == 3) goToLaunchMenu();
            }
        }

        // render
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        if (launchMenu != null && launchMenu.isActive()) {
            launchMenu.render(batch, font);
        } else if (createMap != null && createMap.isActive()) {
            createMap.render(batch, font);
        } else {
            if (carte != null) carte.render(batch, cameraX);
            if (joueur != null) joueur.render(batch, cameraX);
            renderHUD();
            if (gameOver) renderGameOver();
            if (pauseMenu != null) pauseMenu.render(batch, font);
            if (gameOverMenu != null) gameOverMenu.render(batch, font);
        }
        batch.end();
    }

    private void handleTogglePause() {
        if (launchMenu != null && launchMenu.isActive()) return; // disable pause during launch menu
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) && !gameOver) {
            pauseMenu.toggle();
        }
    }

    private void updateGame(float delta) {
        if (joueur != null && carte != null) {
            joueur.update(delta, carte);
            if (joueur.getX() + joueur.getWidth() >= carte.getMapWidth()) {
                gameOver = true;
                gameOverMessage = "Fin de partie";
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
        font.draw(batch, "Temps : " + (int)(joueur.getElapsedTime()) + "s", 20, 860);
    }

    private void renderGameOver() {
        font.getData().setScale(2f);
        float x = Gdx.graphics.getWidth() / 2f - 150;
        float y = Gdx.graphics.getHeight() / 2f + 80;
        String msg = (gameOverMessage != null) ? gameOverMessage : "Fin de partie";
        font.draw(batch, msg, x, y);
        font.getData().setScale(1f);
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
    }
}