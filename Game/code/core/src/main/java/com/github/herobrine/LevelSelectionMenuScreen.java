package com.github.herobrine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class LevelSelectionMenuScreen extends AbstractMenuScreen {

    // ÉTAPE 1 : Définir les modes de fonctionnement possibles pour cet écran.
    public enum SelectionMode {
        PLAY, // Pour lancer une partie
        EDIT  // Pour modifier/supprimer une carte
    }

    private final Main game;
    private final Table listTable;
    private final SelectionMode mode; // Stocke le mode actuel de l'écran

    // ÉTAPE 2 : Mettre à jour le constructeur pour accepter un mode.
    public LevelSelectionMenuScreen(Skin skin, final Main game, SelectionMode mode) {
        super(skin);
        this.game = game;
        this.mode = mode; // On sauvegarde le mode

        // Le reste du constructeur est identique à votre code, car la disposition est bonne.
        Label titleLabel = new Label("Selectionnez un niveau", skin);
        
        listTable = new Table(skin);
        listTable.top();

        ScrollPane scrollPane = new ScrollPane(listTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setOverscroll(false, false);

        Table frameTable = new Table();
        frameTable.setBackground(skin.getDrawable("list-frame"));
        frameTable.setClip(true);
        frameTable.add(scrollPane).expand().fill().pad(10f);

        TextButton backButton = new TextButton("Retour", skin);
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                deactivate();
                game.returnToPreviousMenuFromSelection();
            }
        });

        final float heightPerLevel = 60f;
        final int maxLevelsVisible = 8;
        final float framePadding = 20f;
        final float frameHeight = (heightPerLevel * maxLevelsVisible) + framePadding;

        table.add(titleLabel).padTop(20).padBottom(20);
        table.row();
        table.add(frameTable).width(640).height(frameHeight);
        table.row();
        table.add(backButton).width(400).height(50).pad(20);
    }

    @Override
    public void activate() {
        super.activate();
        populateLevelList();
    }

    private void populateLevelList() {
        listTable.clear();

        FileHandle dirHandle = Gdx.files.local("assets/levels/");
        if (!dirHandle.exists() || !dirHandle.isDirectory()) {
            listTable.add("Dossier 'assets/levels/' non trouve !");
            return;
        }

        FileHandle[] files = dirHandle.list(".txt");

        if (files.length == 0) {
            listTable.add("Aucun niveau trouve.");
            return;
        }

        for (final FileHandle file : files) {
            String levelName = file.nameWithoutExtension();
            TextButton levelButton = new TextButton(levelName, skin);

            levelButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    // ÉTAPE 3 : Exécuter l'action appropriée en fonction du mode.
                    if (mode == SelectionMode.PLAY) {
                        // Si on est en mode JEU, on lance la partie directement.
                        deactivate();
                        game.onLevelSelected(file.path());
                    } else { // mode == SelectionMode.EDIT
                        // Sinon (mode ÉDITION), on affiche la boîte de dialogue.
                        showEditOrDeleteDialog(file);
                    }
                }
            });

            listTable.add(levelButton).growX().height(50).pad(5);
            listTable.row();
        }
    }

    // La méthode showEditOrDeleteDialog est identique à votre code, elle est parfaite.
    private void showEditOrDeleteDialog(final FileHandle file) {
        Dialog dialog = new Dialog("Action pour '" + file.nameWithoutExtension() + "'", skin) {
            @Override
protected void result(Object object) {
                if (object.equals("delete")) {
                    boolean deleted = file.delete();
                    if (deleted) {
                        Gdx.app.log("FileAction", "Fichier supprime : " + file.path());
                        populateLevelList();
                    } else {
                        Gdx.app.error("FileAction", "Echec de la suppression : " + file.path());
                    }
                } else if (object.equals("edit")) {
                    deactivate();
                    game.onLevelSelected(file.path());
                }
            }
        };

        Table buttonTable = dialog.getButtonTable();

        TextButton editButton = new TextButton("Modifier", skin);
        buttonTable.add(editButton).width(300).height(50).pad(17);
        dialog.setObject(editButton, "edit");
        buttonTable.row();

        TextButton deleteButton = new TextButton("Supprimer", skin);
        buttonTable.add(deleteButton).width(300).height(50).pad(17);
        dialog.setObject(deleteButton, "delete");
        buttonTable.row();

        TextButton cancelButton = new TextButton("Annuler", skin);
        buttonTable.add(cancelButton).width(300).height(50).pad(17);
        dialog.setObject(cancelButton, "cancel");

        dialog.padBottom(17f);
        dialog.show(stage);
    }
}
