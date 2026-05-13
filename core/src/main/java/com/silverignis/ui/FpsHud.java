package com.silverignis.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Top-right frame-rate readout. Samples {@link Gdx.Graphics#getFramesPerSecond()}
 * at a fixed interval so the displayed number doesn't flicker every frame.
 */
public class FpsHud {

    private static final float MARGIN_X    = 0.2f;   // world units from right edge
    private static final float MARGIN_TOP  = 0.1f;   // world units from top edge
    private static final float SAMPLE_RATE = 0.25f;  // seconds between text updates

    private static final Color TEXT_COLOR = new Color(1f, 1f, 1f, 1f);

    private final GlyphLayout layout = new GlyphLayout();
    private float sampleAccum = 0f;
    private int sampledFps = -1;

    public void render(SpriteBatch batch, Viewport viewport, BitmapFont font, float delta) {
        sampleAccum += delta;
        if (sampledFps < 0 || sampleAccum >= SAMPLE_RATE) {
            sampleAccum = 0f;
            sampledFps = Gdx.graphics.getFramesPerSecond();
        }

        String text = "FPS " + sampledFps;
        layout.setText(font, text);

        float x = viewport.getWorldWidth() - layout.width - MARGIN_X;
        float y = viewport.getWorldHeight() - MARGIN_TOP;

        boolean wasDrawing = batch.isDrawing();
        if (!wasDrawing) {
            batch.setProjectionMatrix(viewport.getCamera().combined);
            batch.begin();
        }

        Color prev = font.getColor().cpy();
        font.setColor(TEXT_COLOR);
        font.draw(batch, layout, x, y);
        font.setColor(prev);

        if (!wasDrawing) batch.end();
    }
}
