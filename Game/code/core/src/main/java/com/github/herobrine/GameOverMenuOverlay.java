package com.github.herobrine;

import com.badlogic.gdx.graphics.Color; // CORRIGÉ : Import manquant
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class GameOverMenuOverlay extends AbstractMenuScreen {

    private final Main game;
    private final Label messageLabel;

    public GameOverMenuOverlay(Skin skin, final Main game) {
        super(skin);
        this.game = game;

        skin.add("default", new Label.LabelStyle(skin.getFont("default-font"), Color.WHITE));
        messageLabel = new Label("", skin);

        TextButton restartButton = new TextButton("Recommencer", skin);
        TextButton quitButton = new TextButton("Quitter le jeu", skin);
        TextButton mainMenuButton = new TextButton("Retour au menu principal", skin);

        restartButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.restartLevel();
            }
        });

        quitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.quitGame();
            }
        });

        mainMenuButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.returnToLaunchMenu();
            }
        });

        table.add(messageLabel).padBottom(40);
        table.row();
        table.add(restartButton).width(400).height(50).pad(10);
        table.row();
        table.add(mainMenuButton).width(400).height(50).pad(10);
        table.row();
        table.add(quitButton).width(400).height(50).pad(10);
    }

    public void setMessage(String message) {
        messageLabel.setText(message);
    }
}
