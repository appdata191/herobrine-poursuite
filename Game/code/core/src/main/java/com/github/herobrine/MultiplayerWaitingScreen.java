package com.github.herobrine;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class MultiplayerWaitingScreen extends AbstractMenuScreen {

    private final Main game;
    private final Label statusLabel;

    public MultiplayerWaitingScreen(Skin skin, Main game) {
        super(skin);
        this.game = game;

        Label title = new Label("Lobby multijoueur", skin);
        statusLabel = new Label("En attente...", skin);
        TextButton cancelButton = new TextButton("Retour", skin);
        cancelButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.cancelMultiplayerWait();
            }
        });

        table.add(title).padBottom(20);
        table.row();
        table.add(statusLabel).padBottom(30);
        table.row();
        table.add(cancelButton).width(300).height(50);
    }

    public void setStatusText(String status) {
        if (status == null || status.isBlank()) {
            statusLabel.setText("En attente...");
        } else {
            statusLabel.setText(status);
        }
    }
}
