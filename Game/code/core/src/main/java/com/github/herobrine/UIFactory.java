package com.github.herobrine;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;

public class UIFactory {

    public static Skin createSkin(BitmapFont font) {
        Skin skin = new Skin();
        skin.add("default-font", font);

        // --- 1. Style pour les TextButton ---
        TextButtonStyle buttonStyle = new TextButtonStyle();
        buttonStyle.font = skin.getFont("default-font");
        buttonStyle.fontColor = Color.BLACK;
        buttonStyle.up = createNinePatchDrawable(Color.WHITE, Color.GOLD, 8);
        buttonStyle.down = createNinePatchDrawable(Color.LIGHT_GRAY, Color.GOLD, 8);
        skin.add("default", buttonStyle);

        // --- 2. CORRECTION : Style pour le ScrollPane avec barre noire ---
        ScrollPaneStyle scrollPaneStyle = new ScrollPaneStyle();
        // Le fond de la barre de défilement (track)
        scrollPaneStyle.vScroll = createDrawable(new Color(0.1f, 0.1f, 0.1f, 1f)); // Gris très foncé
        // Le curseur de la barre de défilement (knob)
        scrollPaneStyle.vScrollKnob = createDrawable(Color.BLACK); // Noir
        skin.add("default", scrollPaneStyle);

        // --- 3. Style pour le cadre de la liste ---
        // Ce Drawable a un fond blanc et une bordure dorée. Il sera utilisé pour le cadre.
        skin.add("list-frame", createNinePatchDrawable(Color.WHITE, Color.GOLD, 12), Drawable.class);

        return skin;
    }

    private static NinePatchDrawable createNinePatchDrawable(Color innerColor, Color borderColor, int border) {
        final int width = 32;
        final int height = 32;

        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(borderColor);
        pixmap.fill();
        pixmap.setColor(innerColor);
        pixmap.fillRectangle(border, border, width - border * 2, height - border * 2);

        Texture texture = new Texture(pixmap);
        pixmap.dispose();

        NinePatch ninePatch = new NinePatch(texture, border, border, border, border);
        return new NinePatchDrawable(ninePatch);
    }

    private static Drawable createDrawable(Color color) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Drawable drawable = new NinePatchDrawable(new NinePatch(new Texture(pixmap)));
        pixmap.dispose();
        return drawable;
    }
}
