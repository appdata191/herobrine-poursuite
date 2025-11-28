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
import com.github.herobrine.reseau.GameClient;
import com.github.herobrine.reseau.GameServer;
import com.github.herobrine.reseau.PacketDoorState;
import com.github.herobrine.reseau.PacketGameOver;
import com.github.herobrine.reseau.PacketStartGame;
import com.github.herobrine.reseau.PacketRestartRequest;
import com.github.herobrine.reseau.PacketReturnToMenu;
import java.io.IOException;

public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private Skin uiSkin;

    // Textures
    private Texture background, blockTop, blockBottom, creeperTex, picsTex, doorTex, pressurePlateTex;;

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
    private MultiplayerMenuScreen multiplayerMenu;
    private MultiplayerWaitingScreen multiplayerWaitingScreen;

    // Réseau
    private GameServer gameServer;
    private GameClient gameClient;
    private String remoteHost = "127.0.0.1";

    private static final String DEFAULT_REMOTE_HOST = "127.0.0.1" ;

    // États
    private float cameraX = 0f;
    private boolean isEditing = false;
    private boolean waitingForMultiplayerStart = false;
    private String pendingLevelPath = null;
    private static final float BACKGROUND_EXTRA_HEIGHT = 60f;
    private boolean multiplayerSessionActive = false;
    private boolean isHostPlayer = false;
    private int lastHandledRestartId = -1;
    private boolean waitingReturnToMenu = false;
    private long returnToMenuRequestTimeMs = 0L;
    private static final long RETURN_TO_MENU_TIMEOUT_MS = 1500L;

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
        doorTex = new Texture("Porte fermee.png");
        pressurePlateTex = new Texture("pressure_plate.png");

        uiSkin = UIFactory.createSkin(font);
        carte = new Carte(blockTop, blockBottom);
        Porte.setDoorStateNotifier(this::handleLocalDoorStateChange);
        createMap = new CreateMap(BACKGROUND_EXTRA_HEIGHT, background, blockTop, blockBottom, creeperTex, picsTex, doorTex, pressurePlateTex);
    


        // Initialisation des écrans de menu
        launchMenu = new GameLaunchMenuScreen(uiSkin, this);
        editorStartMenu = new EditorStartMenuScreen(uiSkin, this);
        pauseMenu = new PauseMenuOverlay(uiSkin, this);
        gameOverMenu = new GameOverMenuOverlay(uiSkin, this);
        levelSelectionMenu = new LevelSelectionMenuScreen(uiSkin, this, LevelSelectionMenuScreen.SelectionMode.PLAY);
        multiplayerMenu = new MultiplayerMenuScreen(uiSkin, this);
        multiplayerWaitingScreen = new MultiplayerWaitingScreen(uiSkin, this);
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
            setMultiplayerSessionActive(false);
            initGame(levelPath);
        }
    }

    public void showEditorMenu() { editorStartMenu.activate(); }
    public void startEditor() { createMap.activate(null); }
    public void showMultiplayerMenu() {
        launchMenu.deactivate();
        multiplayerWaitingScreen.deactivate();
        multiplayerMenu.activate();
    }
    public void showLaunchMenuOnly() {
        multiplayerMenu.deactivate();
        multiplayerWaitingScreen.deactivate();
        waitingForMultiplayerStart = false;
        pendingLevelPath = null;
        setMultiplayerSessionActive(false);
        launchMenu.activate();
    }
    public void showMultiplayerWaitingScreen(String statusMessage) {
        multiplayerMenu.deactivate();
        multiplayerWaitingScreen.setStatusText(statusMessage);
        multiplayerWaitingScreen.activate();
    }
    public void cancelMultiplayerWait() {
        stopNetwork();
        multiplayerWaitingScreen.deactivate();
        multiplayerMenu.activate();
    }
    public void awaitMultiplayerStart() {
        waitingForMultiplayerStart = true;
    }
    public boolean configureMultiplayerLobby(String levelPath, int expectedPlayers) {
        if (gameClient == null || !gameClient.connected) {
            System.out.println("Client non connecté, impossible de configurer le lobby multijoueur.");
            return false;
        }
        if (levelPath == null || levelPath.isBlank() || expectedPlayers <= 0) {
            System.out.println("Paramètres de lobby invalides.");
            return false;
        }
        waitingForMultiplayerStart = true;
        pendingLevelPath = levelPath;
        gameClient.sendLobbyConfig(levelPath, expectedPlayers);
        System.out.println("Lobby configuré : " + levelPath + " pour " + expectedPlayers + " joueurs.");
        return true;
    }
    public boolean isClientConnected() {
        return gameClient != null && gameClient.connected;
    }
    public void returnToLaunchMenu() { 
        if (waitingReturnToMenu) return;
        if (multiplayerSessionActive) {
            if (gameServer != null) {
                gameServer.broadcastReturnToMenu("Retour au menu principal");
            } else if (gameClient != null && gameClient.connected) {
                gameClient.sendReturnToMenu("Retour au menu principal");
            }
            waitingReturnToMenu = true;
            returnToMenuRequestTimeMs = System.currentTimeMillis();
            return;
        }
        handleReturnToMenuFromNetwork("Retour local au menu principal");
    }
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
        String currentLevel = carte.getCurrentLevelPath();
        if (currentLevel == null) {
            returnToLaunchMenu();
            return;
        }

        if (multiplayerSessionActive) {
            if (!canCurrentPlayerRestart()) {
                System.out.println("Seul l'hôte peut redémarrer une partie multijoueur.");
                return;
            }
            // Host relance la partie via le serveur pour synchroniser tous les clients.
            if (gameServer != null) {
                gameServer.restartGame(currentLevel);
            }
            gameOverMenu.deactivate();
            return;
        }

        gameOverMenu.deactivate();
        initGame(currentLevel);
    }
    private void triggerGameOver(String message) {
        //if (gameOverMenu.isActive()) return;
        gameOverMenu.setMessage(message);
        gameOverMenu.setRestartButtonVisible(canCurrentPlayerRestart());
        gameOverMenu.activate();
    }

    private void initGame(String levelPath) {
        carte.create(levelPath);
        if (joueur != null) joueur.dispose();
        
        joueur = new Joueur(carte.getTile() * 5, carte.getGroundYAtGridX(5));
        joueur.create();
        joueur.setDead(false);
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
        multiplayerMenu.deactivate();
        multiplayerWaitingScreen.deactivate();
        waitingForMultiplayerStart = false;
        pendingLevelPath = null;
        setMultiplayerSessionActive(false);

        launchMenu.activate();
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        processNetworkEvents();
        checkReturnToMenuTimeout();

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
        if (multiplayerMenu.isActive()) multiplayerMenu.render();
        if (multiplayerWaitingScreen.isActive()) multiplayerWaitingScreen.render();
        
        if (createMap.isActive()) {
            if (createMap.updateInput() == 1) goToLaunchMenu();
            createMap.render(batch, font);
        }
    }

    private void processNetworkEvents() {
        if (gameClient == null || !gameClient.connected) return;

        PacketReturnToMenu returnToMenuPacket;
        while ((returnToMenuPacket = gameClient.pollReturnToMenu()) != null) {
            handleReturnToMenuFromNetwork(returnToMenuPacket.reason);
        }

        PacketRestartRequest restartPacket;
        while ((restartPacket = gameClient.pollRestartRequest()) != null) {
            if (restartPacket.restartId <= lastHandledRestartId) {
                gameClient.sendRestartAck(restartPacket.restartId);
                continue;
            }
            gameClient.resetNetworkState();
            waitingForMultiplayerStart = false;
            pendingLevelPath = restartPacket.levelPath;
            multiplayerWaitingScreen.deactivate();
            initGame(restartPacket.levelPath);
            lastHandledRestartId = restartPacket.restartId;
            gameClient.sendRestartAck(restartPacket.restartId);
        }

        PacketStartGame startPacket = gameClient.pollStartGamePacket();
        if (startPacket != null) {
            lastHandledRestartId = -1;
            if (gameClient != null) {
                gameClient.resetNetworkState();
            }
            waitingForMultiplayerStart = false;
            pendingLevelPath = startPacket.levelPath;
            multiplayerWaitingScreen.deactivate();
            initGame(startPacket.levelPath);
        }

        PacketGameOver overPacket = gameClient.pollGameOverPacket();
        if (overPacket != null) {
            String reason = (overPacket.reason != null && !overPacket.reason.isBlank())
                ? overPacket.reason
                : "Partie terminée.";
            waitingForMultiplayerStart = false;
            pendingLevelPath = null;
            multiplayerWaitingScreen.deactivate();
            triggerGameOver(reason);
        }

        PacketDoorState doorPacket;
        while ((doorPacket = gameClient.pollDoorStatePacket()) != null) {
            if (carte != null) {
                carte.applyDoorState(doorPacket.doorId, doorPacket.open);
            }
        }
    }

    private void handleReturnToMenuFromNetwork(String reason) {
        System.out.println("Retour au menu principal demandé : " + (reason != null ? reason : ""));
        waitingReturnToMenu = false;
        stopNetwork();
        goToLaunchMenu();
    }

    private boolean isGameBlocked() {
        return launchMenu.isActive() || editorStartMenu.isActive() || levelSelectionMenu.isActive() ||
               createMap.isActive() || pauseMenu.isActive() || gameOverMenu.isActive() ||
               multiplayerMenu.isActive() || multiplayerWaitingScreen.isActive() || waitingForMultiplayerStart || waitingReturnToMenu;
    }

    private void handleGlobalInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) && joueur != null) {
            togglePause();
        }

    }

    private void updateGame(float delta) {
        joueur.update(delta, carte);
        updateCamera();
        if (carte.updateAutomates(delta, joueur)) {
            joueur.setDead(true);
            triggerGameOver("Vous vous etes fait tuer !");
            if (multiplayerSessionActive && gameClient != null) {
                gameClient.sendGameOver("Un joueur est mort.");
            }
            return;
        }
        if (joueur.getY() < 1) {
            joueur.setDead(true);
            triggerGameOver("Vous etes tombe dans un gouffre !");
            if (multiplayerSessionActive && gameClient != null) {
                gameClient.sendGameOver("Un joueur est mort.");
            }
            return;
        }
        if (multiplayerSessionActive) {
            syncNetworkState();
        } else if (joueur != null) {
            joueur.updateRemotePlayers(null);
        }
        if (joueur.getX() + joueur.getWidth() >= carte.getMapWidth()) {
            triggerGameOver("Vous avez gagné !");
            if (multiplayerSessionActive && gameClient != null) {
                gameClient.sendGameOver("Victoire !");
            }
        }
    }

    private void syncNetworkState() {
        if (joueur == null) return;
        if (gameClient == null || !gameClient.connected) {
            joueur.updateRemotePlayers(null);
            return;
        }

        gameClient.sendPlayerState(joueur.getX(), joueur.getY(), joueur.isDead());
        joueur.updateRemotePlayers(gameClient.getRemotePlayersSnapshot());
        if (joueur.isAnyRemoteDead()) {
            triggerGameOver("Un autre joueur est mort !");
        }
    }

    private void setMultiplayerSessionActive(boolean active) {
        if (multiplayerSessionActive == active) return;
        multiplayerSessionActive = active;
        if (!active && joueur != null) {
            joueur.updateRemotePlayers(null);
        }
        refreshRestartButtonState();
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

    // --- Gestion du réseau ---

    public String getRemoteHost() {
        return remoteHost;
    }

    public void setRemoteHost(String host) {
        if (host != null && !host.isBlank()) {
            remoteHost = host.trim();
        } else {
            remoteHost = DEFAULT_REMOTE_HOST;
        }
    }

    public void hostLocalServer() {
        if (gameServer != null) {
            System.out.println("Serveur déjà lancé.");
            return;
        }
        try {
            gameServer = new GameServer();
            System.out.println("Serveur local prêt.");
            setHostPlayer(true);
        } catch (IOException e) {
            System.err.println("Impossible de démarrer le serveur local.");
            e.printStackTrace();
        }
    }

    public void hostAndPlay() {
        try {
            if (gameServer == null) {
                gameServer = new GameServer();
                System.out.println("✅ Serveur local démarré.");
            } else {
                System.out.println("⚠️ Serveur déjà actif.");
            }

            if (gameClient == null || !gameClient.connected) {
                String host = (remoteHost != null && !remoteHost.isBlank()) ? remoteHost : DEFAULT_REMOTE_HOST;
                gameClient = new GameClient(host);
                setHostPlayer(true);
                setMultiplayerSessionActive(true);
                System.out.println("✅ Client local connecté au serveur local.");
            } else {
                System.out.println("⚠️ Client déjà connecté.");
            }
        } catch (IOException e) {
            System.err.println("❌ Échec du mode Host & Play.");
            e.printStackTrace();
        }
    }

    public void connectToConfiguredHost() {
        startClient(remoteHost);
    }

    public void startClient(String host) {
        if (gameClient != null && gameClient.connected) {
            System.out.println("Client déjà connecté.");
            return;
        }
        try {
            gameClient = new GameClient(host);
            setHostPlayer(false);
            setMultiplayerSessionActive(true);
        } catch (IOException e) {
            System.err.println("Échec de connexion au serveur " + host);
            e.printStackTrace();
        }
    }

    public void stopNetwork() {
        setMultiplayerSessionActive(false);
        waitingReturnToMenu = false;
        if (gameClient != null) {
            gameClient.stop();
            gameClient = null;
        }
        if (gameServer != null) {
            gameServer.stop();
            gameServer = null;
        }
        if (joueur != null) {
            joueur.updateRemotePlayers(null);
        }
        waitingForMultiplayerStart = false;
        pendingLevelPath = null;
        if (multiplayerWaitingScreen != null) {
            multiplayerWaitingScreen.deactivate();
        }
        setHostPlayer(false);
        lastHandledRestartId = -1;
    }

    private void checkReturnToMenuTimeout() {
        if (!waitingReturnToMenu) return;
        long now = System.currentTimeMillis();
        if (now - returnToMenuRequestTimeMs >= RETURN_TO_MENU_TIMEOUT_MS) {
            handleReturnToMenuFromNetwork("Timeout retour menu");
        }
    }

    private boolean canCurrentPlayerRestart() {
        return !multiplayerSessionActive || isHostPlayer;
    }

    private void setHostPlayer(boolean isHost) {
        if (this.isHostPlayer == isHost) return;
        this.isHostPlayer = isHost;
        refreshRestartButtonState();
    }

    private void refreshRestartButtonState() {
        if (gameOverMenu != null) {
            gameOverMenu.setRestartButtonVisible(canCurrentPlayerRestart());
        }
    }

    private void handleLocalDoorStateChange(int doorId, boolean open) {
        if (gameClient != null && gameClient.connected) {
            gameClient.sendDoorState(doorId, open);
        }
    }

    @Override
    public void dispose() {
        stopNetwork();
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

        if (doorTex != null) doorTex.dispose();
        if (pressurePlateTex != null) pressurePlateTex.dispose();
        multiplayerMenu.dispose();
        multiplayerWaitingScreen.dispose();
    }
}
