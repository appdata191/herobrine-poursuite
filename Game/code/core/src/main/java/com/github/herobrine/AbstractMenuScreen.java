package com.github.herobrine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/**
 * NOUVELLE CLASSE DE BASE pour tous les menus utilisant Scene2D.
 * Gère la scène, la disposition et le style.
 */
public abstract class AbstractMenuScreen {
    protected Stage stage;
    protected Table table;
    protected Skin skin;
    protected boolean active = false;

    public AbstractMenuScreen(Skin skin) {
        this.stage = new Stage(new ScreenViewport());
        this.skin = skin;
        this.table = new Table();
        this.table.setFillParent(true); // La table prendra toute la place de la scène
        this.stage.addActor(this.table);
    }

    public void activate() {
        this.active = true;
        // Crucial : Indiquer à LibGDX que le Stage est maintenant le processeur d'entrées principal
        Gdx.input.setInputProcessor(stage);
    }

    public void deactivate() {
        this.active = false;
        // Crucial : Rendre la main au processeur d'entrées par défaut
        Gdx.input.setInputProcessor(null);
    }

    public boolean isActive() {
        return active;
    }

    /**
     * La méthode de rendu met à jour la logique de la scène et la dessine.
     */
    public void render() {
        if (!active) return;
        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
    }

    /**
     * Libère les ressources de la scène.
     */
    public void dispose() {
        if (stage != null) stage.dispose();
    }
}
