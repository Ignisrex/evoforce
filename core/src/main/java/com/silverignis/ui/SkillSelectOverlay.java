package com.silverignis.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.silverignis.skills.Skill;

import java.util.Collections;
import java.util.List;

/**
 * Renders a horizontal row of skill cards with a cursor highlight.
 * Owns its own assets; called from {@code SkillSelectState}.
 */
public class SkillSelectOverlay {

    // Cards are drawn as small landscape rectangles, slightly bigger than
    // the ~1.5-diameter circular icon textures they enclose.
    private static final float CARD_W      = 1.75f;  // world units (width)
    private static final float CARD_H      = 1.50f;  // world units (height)
    private static final float CARD_GAP    = 0.3f;
    private static final float ROW_Y       = 3f;     // world Y of card row baseline
    private static final Color DIM         = new Color(0f, 0f, 0f, 0.55f);
    private static final Color CARD_BG     = new Color(0.1f, 0.1f, 0.15f, 0.9f);
    private static final Color CURSOR      = new Color(1f, 0.9f, 0.2f, 1f);
    /** Faint placeholder shown when there's no hand to draw. */
    private static final Color EMPTY_BG    = new Color(0.1f, 0.1f, 0.15f, 0.6f);
    private static final Color EMPTY_BORDER= new Color(0.5f, 0.5f, 0.55f, 0.8f);

    private final Texture pixel;
    private List<Skill> options = Collections.emptyList();
    private int cursor;
    private float pulse; // 0..1 cosine pulse for cursor highlight

    public SkillSelectOverlay() {
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fill();
        this.pixel = new Texture(p);
        p.dispose();
    }

    public void show(List<Skill> options) {
        this.options = options;
        this.cursor = 0;
        this.pulse = 0f;
    }

    public void hide() {
        this.options = Collections.emptyList();
    }

    public void update(float delta, int cursor) {
        this.cursor = cursor;
        this.pulse  = (this.pulse + delta * 4f) % (float) (Math.PI * 2);
    }

    /**
     * Renders the dimmer + cards + cursor. Caller must have {@code batch.begin()}
     * already open and the projection matrix set to {@code viewport}.
     *
     * <p>The dim layer is drawn unconditionally so the menu still reads as
     * "open" even when the hand comes back empty (all skills loaded into
     * slots, or all on cooldown). In that case a single empty placeholder
     * card is drawn so the player has visual confirmation they're in the
     * staging menu and can still {@code Enter}/{@code Esc} their way out.
     */
    public void render(SpriteBatch batch, Viewport viewport) {
        float worldW = viewport.getWorldWidth();
        float worldH = viewport.getWorldHeight();

        Color prev = batch.getColor().cpy();

        // Dim the world. Always draw — it's the visual cue that the menu is open.
        batch.setColor(DIM);
        batch.draw(pixel, 0f, 0f, worldW, worldH);

        if (options.isEmpty()) {
            renderEmptyPlaceholder(batch, worldW);
            batch.setColor(prev);
            return;
        }

        // Card row, centered horizontally.
        int n = options.size();
        float rowWidth = n * CARD_W + (n - 1) * CARD_GAP;
        float startX   = (worldW - rowWidth) / 2f;

        for (int i = 0; i < n; i++) {
            float x = startX + i * (CARD_W + CARD_GAP);
            float y = ROW_Y;

            // Card background
            batch.setColor(CARD_BG);
            batch.draw(pixel, x, y, CARD_W, CARD_H);

            // Icon
            Skill s = options.get(i);
            if (s.getIcon() != null) {
                batch.setColor(Color.WHITE);
                batch.draw(s.getIcon(), x, y, CARD_W, CARD_H);
            }

            // Cursor outline (drawn as four thin rectangles around the card)
            if (i == cursor) {
                float t = 0.06f + 0.02f * (float) Math.sin(pulse);
                batch.setColor(CURSOR);
                batch.draw(pixel, x - t, y - t, CARD_W + 2 * t, t);                 // bottom
                batch.draw(pixel, x - t, y + CARD_H, CARD_W + 2 * t, t);            // top
                batch.draw(pixel, x - t, y, t, CARD_H);                             // left
                batch.draw(pixel, x + CARD_W, y, t, CARD_H);                        // right
            }
        }

        batch.setColor(prev);
    }

    public void dispose() {
        pixel.dispose();
    }

    /**
     * Draws a single faint, dashed-border card centered on the screen as a
     * "no skills available" indicator. No cursor pulse — there's nothing to
     * point at.
     */
    private void renderEmptyPlaceholder(SpriteBatch batch, float worldW) {
        float x = (worldW - CARD_W) / 2f;
        float y = ROW_Y;

        // Faint card body.
        batch.setColor(EMPTY_BG);
        batch.draw(pixel, x, y, CARD_W, CARD_H);

        // Static outline so the card silhouette is visible against the dim.
        float t = 0.05f;
        batch.setColor(EMPTY_BORDER);
        batch.draw(pixel, x - t, y - t, CARD_W + 2 * t, t);             // bottom
        batch.draw(pixel, x - t, y + CARD_H, CARD_W + 2 * t, t);        // top
        batch.draw(pixel, x - t, y, t, CARD_H);                         // left
        batch.draw(pixel, x + CARD_W, y, t, CARD_H);                    // right
    }
}
