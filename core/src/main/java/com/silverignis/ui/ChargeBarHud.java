package com.silverignis.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.silverignis.skills.ChargeMeter;

/**
 * A small loading bar drawn just above the {@link SlotsHud} in the bottom-left,
 * gating access to the staging menu when full. Tinted via {@link SpriteBatch#setColor}
 * on a 1x1 white pixel so we don't need any art assets.
 */
public class ChargeBarHud {

    // Positioned to sit just above the SlotsHud column. Width matches the
    // span of the three X/Y/B slot panels so the two HUDs read as one stack.
    private static final float BAR_X      = 0.34f;   // world units (slot panel left edge)
    private static final float BAR_Y      = 1.82f;   // world units (just above slot panel top)
    private static final float BAR_WIDTH  = 2.62f;
    private static final float BAR_HEIGHT = 0.18f;

    private static final Color BG    = new Color(0f, 0f, 0f, 0.6f);
    private static final Color FILL  = new Color(0.2f, 0.8f, 1f, 1f);
    private static final Color FULL  = new Color(1f, 0.9f, 0.2f, 1f);

    private final Texture pixel;

    public ChargeBarHud(Texture pixel) {
        this.pixel = pixel;
    }

    public void render(SpriteBatch batch, Viewport viewport, ChargeMeter meter) {
        float x = BAR_X;
        float y = BAR_Y;
        float w = BAR_WIDTH;
        float h = BAR_HEIGHT;

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

}
