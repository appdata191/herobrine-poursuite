package com.github.herobrine;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox.SelectBoxStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle; // NOUVEL IMPORT
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

        // --- 2. Style pour les TextField ---
        TextFieldStyle textFieldStyle = new TextFieldStyle();
        textFieldStyle.font = skin.getFont("default-font");
        textFieldStyle.fontColor = Color.BLACK;
        textFieldStyle.background = createNinePatchDrawable(Color.WHITE, Color.GOLD, 6);
        textFieldStyle.cursor = createDrawable(Color.BLACK);
        textFieldStyle.selection = createDrawable(new Color(1f, 1f, 0f, 0.35f));
        skin.add("default", textFieldStyle);

        // --- 3. Style pour les Labels ---
        LabelStyle labelStyle = new LabelStyle();
        labelStyle.font = skin.getFont("default-font");
        labelStyle.fontColor = Color.BLACK;
        skin.add("default", labelStyle);

        // --- 4. Style pour le ScrollPane ---
        ScrollPaneStyle scrollPaneStyle = new ScrollPaneStyle();
        scrollPaneStyle.vScroll = createDrawable(new Color(0.1f, 0.1f, 0.1f, 1f));
        scrollPaneStyle.vScrollKnob = createDrawable(Color.BLACK);
        scrollPaneStyle.background = createNinePatchDrawable(Color.WHITE, Color.GOLD, 6);
        skin.add("default", scrollPaneStyle);

        // --- 5. Style pour les listes ---
        ListStyle listStyle = new ListStyle();
        listStyle.font = skin.getFont("default-font");
        listStyle.fontColorSelected = Color.BLACK;
        listStyle.fontColorUnselected = Color.BLACK;
        listStyle.background = createNinePatchDrawable(Color.WHITE, Color.GOLD, 6);
        listStyle.selection = createNinePatchDrawable(new Color(1f, 1f, 0f, 0.35f), Color.GOLD, 6);
        skin.add("default", listStyle);

        // --- 6. Style pour les SelectBox ---
        SelectBoxStyle selectBoxStyle = new SelectBoxStyle();
        selectBoxStyle.font = skin.getFont("default-font");
        selectBoxStyle.fontColor = Color.BLACK;
        selectBoxStyle.background = createNinePatchDrawable(Color.WHITE, Color.GOLD, 6);
        selectBoxStyle.scrollStyle = scrollPaneStyle;
        selectBoxStyle.listStyle = listStyle;
        skin.add("default", selectBoxStyle);

        // --- 7. Style pour le cadre de la liste ---
        skin.add("list-frame", createNinePatchDrawable(Color.WHITE, Color.GOLD, 12), Drawable.class);

        // --- 8. NOUVEAU : Style pour la fenêtre de dialogue ---
        WindowStyle dialogStyle = new WindowStyle();
        dialogStyle.titleFont = skin.getFont("default-font");
        dialogStyle.titleFontColor = Color.BLACK;
        // On réutilise le même fond que la liste pour un look cohérent
        dialogStyle.background = skin.getDrawable("list-frame");
        skin.add("default", dialogStyle);

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
