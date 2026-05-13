package com.silverignis.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.silverignis.skills.ChargeMeter;

/**
 * A simple loading bar drawn at the top of the world. Uses a 1x1 white pixel
 * tinted with {@link SpriteBatch#setColor} so we don't need any art assets.
 */
public class ChargeBarHud {

    private static final float MARGIN_X    = 1f;     // world units
    private static final float MARGIN_TOP  = 0.4f;
    private static final float BAR_HEIGHT  = 0.3f;

    private static final Color BG    = new Color(0f, 0f, 0f, 0.6f);
    private static final Color FILL  = new Color(0.2f, 0.8f, 1f, 1f);
    private static final Color FULL  = new Color(1f, 0.9f, 0.2f, 1f);

    private final Texture pixel;

    public ChargeBarHud() {
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fill();
        this.pixel = new Texture(p);
        p.dispose();
    }

    public void render(SpriteBatch batch, Viewport viewport, ChargeMeter meter) {
        float w = viewport.getWorldWidth() - 2 * MARGIN_X;
        float h = BAR_HEIGHT;
        float x = MARGIN_X;
        float y = viewport.getWorldHeight() - MARGIN_TOP - h;

        boolean wasDrawing = batch.isDrawing();
        if (!wasDrawing) {
            batch.setProjectionMatrix(viewport.getCamera().combined);
            batch.begin();
        }

        Color prev = batch.getColor().cpy();

        // Background
        batch.setColor(BG);
        batch.draw(pixel, x, y, w, h);

        // Fill
        float ratio = meter.getFillRatio();
        batch.setColor(meter.isFull() ? FULL : FILL);
        batch.draw(pixel, x, y, w * ratio, h);

        batch.setColor(prev);

        if (!wasDrawing) batch.end();
    }

    public void dispose() {
        pixel.dispose();
    }
}
