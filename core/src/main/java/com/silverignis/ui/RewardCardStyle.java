package com.silverignis.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.silverignis.assets.GeneratedAssets;

public final class RewardCardStyle implements Disposable {

    public final Label.LabelStyle title, body, small;
    public final Drawable cardBg, cardBgSelected, statBox;
    public final Texture pixel;

    private final BitmapFont titleFont, bodyFont, smallFont;
    private final RoundedRectShader shader;

    public RewardCardStyle(GeneratedAssets generated, Viewport viewport){
        pixel = generated.pixel();
        shader = new RoundedRectShader(pixel);

        float worldScale = viewport.getWorldHeight() / Gdx.graphics.getHeight();
        FreeTypeFontGenerator grotesk = new FreeTypeFontGenerator(Gdx.files.internal("ui/fonts/SpaceGrotesk-Variable.ttf"));
        FreeTypeFontGenerator mono = new FreeTypeFontGenerator(Gdx.files.internal("ui/fonts/JetBrainsMono-Bold.ttf"));
        titleFont = UiUtil.genFont(grotesk, 30, worldScale);
        bodyFont  = UiUtil.genFont(mono, 17, worldScale);
        smallFont = UiUtil.genFont(mono, 16, worldScale);
        grotesk.dispose(); mono.dispose();

        title = new Label.LabelStyle(titleFont, UiTheme.TEXT);
        body  = new Label.LabelStyle(bodyFont,  UiTheme.TEXT);
        small = new Label.LabelStyle(smallFont, UiTheme.TEXT_DIM);

        cardBg = new BezelDrawable(shader, UiTheme.PANEL, UiTheme.OUTLINE_V, UiTheme.BORDER, UiTheme.CORNER_RADIUS);
        cardBgSelected = new BezelDrawable(shader, UiTheme.CARD_HI, UiTheme.CYAN_HI, UiTheme.BORDER,
            UiTheme.CORNER_RADIUS, UiTheme.CYAN_45, UiTheme.GLOW_WIDTH);
        statBox = new BezelDrawable(shader, UiTheme.STATBOX, UiTheme.OUTLINE_V, UiTheme.BORDER_THIN, UiTheme.CORNER_RADIUS);
    }

    @Override
    public void dispose() {
        shader.dispose();
        titleFont.dispose();
        bodyFont.dispose();
        smallFont.dispose();
    }
}
