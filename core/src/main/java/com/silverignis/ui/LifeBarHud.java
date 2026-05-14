package com.silverignis.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.silverignis.entities.Player;

/**
 * Horizontal player HP bar anchored to the top-left of the viewport. Width is
 * fixed; fill ratio is {@code player.getHp() / player.getMaxHp()}. Tint shifts
 * green → yellow → red as HP drops. Uses a 1x1 white pixel + {@link SpriteBatch#setColor}
 * for chrome so no art asset is needed.
 */
public class LifeBarHud {

    private static final float MARGIN_LEFT = 0.4f;     // world units from left edge
    private static final float MARGIN_TOP  = 0.3f;     // world units from top edge
    private static final float BAR_WIDTH   = 4.0f;
    private static final float BAR_HEIGHT  = 0.4f;
    private static final float BORDER      = 0.04f;    // outer frame thickness

    private static final Color FRAME = new Color(0f, 0f, 0f, 0.85f);
    private static final Color BG    = new Color(0.15f, 0.05f, 0.05f, 0.85f);
    private static final Color HIGH  = new Color(0.25f, 0.85f, 0.30f, 1f);
    private static final Color MID   = new Color(1.00f, 0.85f, 0.20f, 1f);
    private static final Color LOW   = new Color(0.95f, 0.25f, 0.20f, 1f);

    private final Texture pixel;

    public LifeBarHud() {
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fill();
        this.pixel = new Texture(p);
        p.dispose();
    }

    public void render(SpriteBatch batch, Viewport viewport, Player player) {
        float x = MARGIN_LEFT;
        float y = viewport.getWorldHeight() - MARGIN_TOP - BAR_HEIGHT;

        int maxHp = Math.max(1, player.getMaxHp());
        float ratio = Math.max(0f, Math.min(1f, player.getHp() / (float) maxHp));

        Color prev = batch.getColor().cpy();

        batch.setColor(FRAME);
        batch.draw(pixel, x - BORDER, y - BORDER, BAR_WIDTH + 2 * BORDER, BAR_HEIGHT + 2 * BORDER);

        batch.setColor(BG);
        batch.draw(pixel, x, y, BAR_WIDTH, BAR_HEIGHT);

        batch.setColor(ratio > 0.5f ? HIGH : ratio > 0.25f ? MID : LOW);
        batch.draw(pixel, x, y, BAR_WIDTH * ratio, BAR_HEIGHT);

        batch.setColor(prev);
    }

    public void dispose() {
        pixel.dispose();
    }
}
