package com.github.herobrine;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

import com.badlogic.gdx.utils.ScreenUtils;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private Texture player, background;
    BitmapFont font;
    int temps = 0;
    float x,y;
    float speed = 200;

    @Override
    public void create() {
        batch = new SpriteBatch();
        player = new Texture("pngegg.png");
        background = new Texture("potence.png");
        font = new BitmapFont();
        x = 100;
        y = 100;
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        if(Gdx.input.isKeyPressed(Input.Keys.D)) x += speed * delta;
        if(Gdx.input.isKeyPressed(Input.Keys.A)) x -= speed * delta;
        if(Gdx.input.isKeyPressed(Input.Keys.W)) y += speed * delta;
        if(Gdx.input.isKeyPressed(Input.Keys.S)) y -= speed * delta;
        if(Gdx.input.isKeyPressed(Input.Keys.ESCAPE)) Gdx.app.exit();;
        temps += 1;
        int timer =  (int) (temps*delta);
        Gdx.gl.glClearColor(0.1f,0.1f,0.2f,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        batch.draw(background,0,0,1920,1080);
        batch.draw(player, x, y,6*10,6*19);

        font.draw(batch, "Temps : " + timer, 20,860);
        batch.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
        player.dispose();
    }
}
