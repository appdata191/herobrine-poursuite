package com.github.herobrine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class LevelSelectionMenuScreen extends AbstractMenuScreen {

    private final Main game;
    private final Table listTable; // La table qui contient la liste des boutons

    public LevelSelectionMenuScreen(Skin skin, final Main game) {
        super(skin);
        this.game = game;

        // --- Éléments de l'interface ---
        Label titleLabel = new Label("Selectionnez un niveau", skin);
        
        // La table intérieure qui contiendra la liste des boutons
        listTable = new Table(skin);
        
        // CORRECTION : Forcer la table à aligner son contenu en HAUT.
        // C'est la ligne qui résout le problème de désalignement des clics.
        listTable.top();

        // Le ScrollPane qui contiendra la listTable
        ScrollPane scrollPane = new ScrollPane(listTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setOverscroll(false, false);

        // La table principale qui servira de cadre et de conteneur
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

        // --- Calcul de la hauteur pour 8 niveaux ---
        final float heightPerLevel = 60f;
        final int maxLevelsVisible = 8;
        final float framePadding = 20f;
        final float frameHeight = (heightPerLevel * maxLevelsVisible) + framePadding;

        // --- Disposition finale ---
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
                    deactivate();
                    game.onLevelSelected(file.path());
                }
            });

            listTable.add(levelButton).growX().height(50).pad(5);
            listTable.row();
        }
    }
}
