package com.github.herobrine;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class PauseMenuOverlay extends AbstractMenuScreen {

    private final Main game;

    public PauseMenuOverlay(Skin skin, final Main game) {
        super(skin);
        this.game = game;

        TextButton resumeButton = new TextButton("Reprendre", skin);
        TextButton quitButton = new TextButton("Quitter le jeu", skin);
        TextButton mainMenuButton = new TextButton("Retour au menu principal", skin);

        resumeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.togglePause();
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

        table.add(resumeButton).width(400).height(50).pad(10);
        table.row();
        table.add(mainMenuButton).width(400).height(50).pad(10);
        table.row();
        table.add(quitButton).width(400).height(50).pad(10);
    }
}
