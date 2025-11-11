package com.github.herobrine;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class EditorStartMenuScreen extends AbstractMenuScreen {

    private final Main game;

    public EditorStartMenuScreen(Skin skin, final Main game) {
        super(skin);
        this.game = game;

        // Création des boutons existants
        TextButton newMapButton = new TextButton("Nouvelle carte", skin);
        TextButton modifyMapButton = new TextButton("Modifier carte", skin);
        TextButton backButton = new TextButton("Retour", skin);
        
        // NOUVEAU : Création du bouton Quitter
        TextButton quitButton = new TextButton("Quitter", skin);

        // Listeners existants
        newMapButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                deactivate();
                game.startEditor();
            }
        });

        modifyMapButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                deactivate();
                game.startLevelSelection(true);
            }
        });

        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                deactivate();
                game.returnToLaunchMenu();
            }
        });

        // NOUVEAU : Ajout du listener pour le bouton Quitter
        quitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.quitGame();
            }
        });

        // Disposition des boutons dans la table
        table.add(newMapButton).width(400).height(50).pad(10);
        table.row();
        table.add(modifyMapButton).width(400).height(50).pad(10);
        table.row();
        table.add(backButton).width(400).height(50).pad(10);
        table.row(); // NOUVEAU : Ligne suivante
        table.add(quitButton).width(400).height(50).pad(10); // Ajout du bouton Quitter
    }
}
