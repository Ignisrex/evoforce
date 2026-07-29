package com.silverignis.ui;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

/** Shared scene2d helpers for the world-unit UI. */
public final class UiUtil {

    private UiUtil() {}

    /** Tables round child geometry to ints by default, which mangles a 16x9 world-unit stage. */
    public static Table newTable() {
        Table t = new Table();
        t.setRound(false);
        return t;
    }

    public static BitmapFont genFont(FreeTypeFontGenerator g, int px, float worldScale) {
        FreeTypeFontParameter p = new FreeTypeFontParameter();
        p.size = px;
        p.minFilter = Texture.TextureFilter.Linear;
        p.magFilter = Texture.TextureFilter.Linear;
        BitmapFont f = g.generateFont(p);
        f.setUseIntegerPositions(false);
        f.getData().setScale(worldScale);
        return f;
    }
}
