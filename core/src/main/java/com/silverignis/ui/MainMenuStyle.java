package com.silverignis.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.silverignis.assets.GeneratedAssets;

/** Main menu widgets: translucent plates over the painting, cyan bezel + glow when focused. */
public final class MainMenuStyle implements Disposable {

    public final TextButton.TextButtonStyle button, buttonFocused, buttonPressed;
    public final Label.LabelStyle version;
    /** Button plate size in world units. */
    public static final float BUTTON_W = 2.6f, BUTTON_H = 0.5f, BUTTON_GAP = 0.1f;

    private final BitmapFont buttonFont, versionFont;
    private final RoundedRectShader shader;

    public MainMenuStyle(GeneratedAssets generated, Viewport viewport) {
        shader = new RoundedRectShader(generated.pixel());

        float worldScale = viewport.getWorldHeight() / Gdx.graphics.getHeight();
        FreeTypeFontGenerator mono = new FreeTypeFontGenerator(Gdx.files.internal("ui/fonts/JetBrainsMono-Bold.ttf"));
        buttonFont  = UiUtil.genFont(mono, 20, worldScale);
        versionFont = UiUtil.genFont(mono, 15, worldScale);
        mono.dispose();

        var plate   = new BezelDrawable(shader, new Color(0f, 0f, 0f, 0.45f), UiTheme.OUTLINE_V, UiTheme.BORDER, UiTheme.CORNER_RADIUS);
        var lit     = new BezelDrawable(shader, new Color(0f, 0f, 0f, 0.70f), UiTheme.CYAN_HI, UiTheme.BORDER,
            UiTheme.CORNER_RADIUS, UiTheme.CYAN_45, UiTheme.GLOW_WIDTH);
        var pressed = new BezelDrawable(shader, new Color(0f, 0f, 0f, 0.85f), UiTheme.CYAN, UiTheme.BORDER, UiTheme.CORNER_RADIUS);
        var faint   = new BezelDrawable(shader, new Color(0f, 0f, 0f, 0.30f), UiTheme.OUTLINE_V_60, UiTheme.BORDER_THIN, UiTheme.CORNER_RADIUS);

        button = new TextButton.TextButtonStyle(plate, pressed, null, buttonFont);
        button.over = lit;
        button.fontColor = UiTheme.TEXT;
        button.disabled = faint;
        button.disabledFontColor = UiTheme.withA(UiTheme.TEXT_DIM, 0.35f);

        buttonFocused = new TextButton.TextButtonStyle(button);
        buttonFocused.up = lit;
        buttonPressed = new TextButton.TextButtonStyle(button);
        buttonPressed.up = pressed;
        buttonPressed.fontColor = UiTheme.CYAN_HI;

        version = new Label.LabelStyle(versionFont, UiTheme.TEXT_DIM);
    }

    @Override
    public void dispose() {
        shader.dispose();
        buttonFont.dispose();
        versionFont.dispose();
    }
}
