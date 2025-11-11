package com.github.herobrine;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class GameLaunchMenuScreen extends AbstractMenuScreen {

    private final Main game;

    public GameLaunchMenuScreen(Skin skin, final Main game) {
        super(skin);
        this.game = game;

        // Création des boutons existants
        TextButton playButton = new TextButton("Jouer", skin);
        TextButton editorButton = new TextButton("Editeur de carte", skin);
        
        // NOUVEAU : Création du bouton Quitter
        TextButton quitButton = new TextButton("Quitter", skin);

        // Ajout des listeners (la logique du clic)
        playButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                deactivate();
                game.startLevelSelection(false);
            }
        });

        editorButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                deactivate();
                game.showEditorMenu();
            }
        });

        // NOUVEAU : Ajout du listener pour le bouton Quitter
        quitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.quitGame(); // Appelle la méthode pour fermer l'application
            }
        });

        // Disposition des boutons dans la table
        table.add(playButton).width(400).height(50).pad(10);
        table.row(); // Ligne suivante
        table.add(editorButton).width(400).height(50).pad(10);
        table.row(); // NOUVEAU : Ligne suivante pour le nouveau bouton
        table.add(quitButton).width(400).height(50).pad(10); // Ajout du bouton Quitter
    }
}
