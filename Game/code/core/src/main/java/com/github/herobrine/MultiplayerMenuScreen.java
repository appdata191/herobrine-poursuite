package com.github.herobrine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ObjectMap;

public class MultiplayerMenuScreen extends AbstractMenuScreen {

    private final Main game;
    private final TextField hostIpField;
    private final TextButton prevLevelButton;
    private final TextButton nextLevelButton;
    private final Label hostLevelLabel;
    private final TextField hostPlayerCountField;
    private final TextField joinIpField;
    private final TextButton hostButton;
    private final ObjectMap<String, String> levelPathMap = new ObjectMap<>();
    private Array<String> levelNames = new Array<>();
    private int selectedLevelIndex = 0;

    public MultiplayerMenuScreen(Skin skin, Main game) {
        super(skin);
        this.game = game;

        Label title = new Label("Mode multijoueur", skin);

        Label hostTitle = new Label("Host Game", skin);
        hostIpField = new TextField("", skin);
        hostIpField.setMessageText("Ex : 127.0.0.1");
        prevLevelButton = new TextButton("<", skin);
        nextLevelButton = new TextButton(">", skin);
        hostLevelLabel = new Label("Aucun niveau", skin);
        hostLevelLabel.setAlignment(Align.center);
        hostPlayerCountField = new TextField("", skin);
        hostPlayerCountField.setMessageText("Nombre total de joueurs");

        hostButton = new TextButton("Host & Play", skin);
        hostButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                String ip = hostIpField.getText().trim();
                if (ip.isEmpty()) {
                    System.out.println("Veuillez saisir une IP pour l'hôte.");
                    return;
                }
                if (levelNames.size == 0) {
                    System.out.println("Aucun niveau disponible.");
                    return;
                }
                String selected = levelNames.get(selectedLevelIndex);
                String levelPath = selected != null ? levelPathMap.get(selected) : null;
                if (levelPath == null || levelPath.isEmpty()) {
                    System.out.println("Veuillez saisir le chemin du niveau.");
                    return;
                }
                int playerCount;
                try {
                    playerCount = Integer.parseInt(hostPlayerCountField.getText().trim());
                } catch (NumberFormatException e) {
                    System.out.println("Nombre de joueurs invalide.");
                    return;
                }
                if (playerCount <= 0) {
                    System.out.println("Le nombre de joueurs doit être supérieur à 0.");
                    return;
                }
                game.setRemoteHost(ip);
                game.hostAndPlay();
                if (!game.isClientConnected()) {
                    System.out.println("Client non connecté, impossible d'initialiser le lobby.");
                    return;
                }
                if (game.configureMultiplayerLobby(levelPath, playerCount)) {
                    game.showMultiplayerWaitingScreen("En attente des joueurs...");
                }
            }
        });

        Label joinTitle = new Label("Join Game", skin);
        joinIpField = new TextField("", skin);
        joinIpField.setMessageText("IP du serveur");

        TextButton joinButton = new TextButton("Rejoindre", skin);
        joinButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                String ip = joinIpField.getText().trim();
                if (ip.isEmpty()) {
                    System.out.println("Veuillez saisir l'IP du serveur à rejoindre.");
                    return;
                }
                game.setRemoteHost(ip);
                game.connectToConfiguredHost();
                if (game.isClientConnected()) {
                    game.awaitMultiplayerStart();
                    game.showMultiplayerWaitingScreen("En attente de l'hôte...");
                }
            }
        });

        TextButton backButton = new TextButton("Retour", skin);
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.showLaunchMenuOnly();
            }
        });

        table.add(title).padBottom(30);
        table.row();

        table.add(hostTitle).padBottom(10);
        table.row();
        table.add(hostIpField).width(400).height(50).padBottom(10);
        table.row();
        table.add(buildLevelSelectorTable()).width(430).height(50).padBottom(10);
        table.row();
        table.add(hostPlayerCountField).width(400).height(50).padBottom(10);
        table.row();
        table.add(hostButton).width(400).height(50).padBottom(30);
        table.row();

        table.add(joinTitle).padBottom(10);
        table.row();
        table.add(joinIpField).width(400).height(50).padBottom(10);
        table.row();
        table.add(joinButton).width(400).height(50).padBottom(40);
        table.row();

        table.add(backButton).width(300).height(50);
    }

    @Override
    public void activate() {
        refreshLevelChoices();
        hostIpField.setText(game.getRemoteHost());
        joinIpField.setText(game.getRemoteHost());
        hostPlayerCountField.setText("");
        super.activate();
    }

    private Table buildLevelSelectorTable() {
        Table levelTable = new Table();
        levelTable.add(prevLevelButton).width(70).height(50).padRight(10);
        levelTable.add(hostLevelLabel).expandX().fillX();
        levelTable.add(nextLevelButton).width(70).height(50).padLeft(10);

        prevLevelButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                changeLevel(-1);
            }
        });
        nextLevelButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                changeLevel(1);
            }
        });
        return levelTable;
    }

    private void changeLevel(int delta) {
        if (levelNames.size == 0) return;
        selectedLevelIndex = Math.min(levelNames.size - 1, Math.max(0, selectedLevelIndex + delta));
        hostLevelLabel.setText(levelNames.get(selectedLevelIndex));
        prevLevelButton.setDisabled(selectedLevelIndex == 0);
        nextLevelButton.setDisabled(selectedLevelIndex == levelNames.size - 1);
    }

    // Met à jour la liste des niveaux disponibles pour l'hébergement
    private void refreshLevelChoices() {
        FileHandle directory = Gdx.files.local("assets/levels/");
        levelPathMap.clear();
        levelNames.clear();

        if (directory.exists() && directory.isDirectory()) {
            for (FileHandle file : directory.list(".txt")) {
                String displayName = file.nameWithoutExtension();
                levelNames.add(displayName);
                levelPathMap.put(displayName, file.path());
            }
        }

        if (levelNames.size == 0) {
            hostLevelLabel.setText("Aucun niveau disponible");
            hostButton.setDisabled(true);
        } else {
            selectedLevelIndex = Math.min(selectedLevelIndex, levelNames.size - 1);
            if (selectedLevelIndex < 0) selectedLevelIndex = 0;
            hostLevelLabel.setText(levelNames.get(selectedLevelIndex));
            prevLevelButton.setDisabled(selectedLevelIndex == 0);
            nextLevelButton.setDisabled(selectedLevelIndex == levelNames.size - 1);
            System.out.println("Niveaux disponibles pour l'hébergement : " + levelNames);
            hostButton.setDisabled(false);
        }
    }
}
