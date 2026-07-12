package com.silverignis.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.silverignis.assets.GeneratedAssets;
import com.silverignis.skills.Skill;
import com.silverignis.skills.slots.ButtonSlot;
import com.silverignis.skills.slots.SkillSlots;
import com.silverignis.skills.slots.SlotKey;

import java.util.List;

/**
 * Always-on HUD that draws the X / Y / B button slots in the bottom-left,
 * staggered like controller face buttons (Y raised, X and B lower).
 *
 * <p>Only the front-of-queue skill gets a framed card; queued skills peek
 * out underneath as stacked rounded cards. Sliver height is adaptive: while
 * the whole queue fits at {@link #SLIVER_MAX} every card gets a generous
 * peek; once it would overflow the space below, deeper cards compress
 * geometrically so the front of the queue stays the most readable.
 *
 * <p>Pure rendering — reads the {@link SkillSlots} object the {@code GameScreen}
 * owns and never writes back.
 */
public class SlotsHud {

    // Icons are drawn as small landscape rectangles, slightly bigger than the
    // ~0.55-diameter circular icon textures they enclose.
    private static final float ICON_W    = 0.80f;     // world units per icon (width)
    private static final float ICON_H    = 0.76f;     // world units per icon (height)
    private static final float SLOT_GAP  = 0.20f;
    private static final float MARGIN_L  = 0.4f;      // left padding from world edge
    private static final float MARGIN_B  = 0.1f;      // stack may grow down to here
    private static final float BASE_Y    = 1.2f;      // front-card y for X and B
    private static final float STAGGER   = 0.35f;     // Y slot raised above X/B

    // Queue slivers: uniform SLIVER_MAX while the stack fits, geometric
    // compression (front-weighted) once it would overflow the space below.
    private static final float SLIVER_MAX = 0.44f;
    private static final float SLIVER_MIN = 0.06f;
    private static final float ALPHA_DECAY = 0.85f;
    private static final float ALPHA_MIN   = 0.35f;

    private static final float SHADOW_OFF = 0.045f;

    private static final Color CARD_BG   = new Color(0.08f, 0.08f, 0.12f, 0.92f);
    private static final Color SHADOW    = new Color(0f, 0f, 0f, 0.35f);
    private static final Color EMPTY_TINT = new Color(1f, 1f, 1f, 0.35f);

    private final Texture card;
    private final Texture frame;

    public SlotsHud(GeneratedAssets generated) {
        this.card  = generated.card();
        this.frame = generated.cardFrame();
    }

    public void render(SpriteBatch batch, Viewport viewport, SkillSlots slots) {
        SlotKey[] keys = SlotKey.values();

        Color prev = batch.getColor().cpy();

        for (int i = 0; i < keys.length; i++) {
            float x = MARGIN_L + i * (ICON_W + SLOT_GAP);
            float y = keys[i] == SlotKey.Y ? BASE_Y + STAGGER : BASE_Y;
            drawSlot(batch, x, y, slots.get(keys[i]));
        }

        batch.setColor(prev);
    }

    private void drawSlot(SpriteBatch batch, float x, float frontY, ButtonSlot slot) {
        List<Skill> queue = slot.view();

        if (queue.isEmpty()) {
            batch.setColor(CARD_BG.r, CARD_BG.g, CARD_BG.b, CARD_BG.a * 0.5f);
            batch.draw(card, x, frontY, ICON_W, ICON_H);
            batch.setColor(EMPTY_TINT);
            batch.draw(frame, x, frontY, ICON_W, ICON_H);
            return;
        }

        float[] offsets = sliverOffsets(queue.size(), frontY - MARGIN_B);

        // Deepest first so nearer cards draw on top; each card is drawn full
        // size offset downward, so only its bottom strip peeks out.
        // ponytail: cards past the bottom margin just aren't drawn — add a
        // "+N" badge once a font is wired into this HUD.
        for (int i = queue.size() - 1; i >= 1; i--) {
            float y = frontY - offsets[i];
            if (y < MARGIN_B) continue;
            float alpha = Math.max(ALPHA_MIN, (float) Math.pow(ALPHA_DECAY, i));
            drawCard(batch, queue.get(i), x, y, alpha);
        }

        drawCard(batch, queue.get(0), x, frontY, 1f);
        batch.setColor(Color.WHITE);
        batch.draw(frame, x, frontY, ICON_W, ICON_H);
    }

    /**
     * Downward offset of each queued card from the front card. Uniform
     * {@link #SLIVER_MAX} while everything fits in {@code space}; otherwise a
     * geometric ratio (found by bisection) shrinks deeper slivers to fit.
     */
    private static float[] sliverOffsets(int n, float space) {
        float[] offsets = new float[n];
        int slivers = n - 1;
        if (slivers <= 0) return offsets;

        float ratio = 1f;
        if (slivers * SLIVER_MAX > space) {
            float lo = 0f, hi = 1f;
            for (int it = 0; it < 24; it++) {
                float mid = (lo + hi) / 2f, sum = 0f, h = SLIVER_MAX;
                for (int i = 0; i < slivers; i++) {
                    sum += Math.max(SLIVER_MIN, h);
                    h *= mid;
                }
                if (sum > space) hi = mid; else lo = mid;
            }
            ratio = lo;
        }

        float h = SLIVER_MAX, off = 0f;
        for (int i = 1; i < n; i++) {
            off += Math.max(SLIVER_MIN, h);
            offsets[i] = off;
            h *= ratio;
        }
        return offsets;
    }

    private void drawCard(SpriteBatch batch, Skill s, float x, float y, float alpha) {
        batch.setColor(SHADOW.r, SHADOW.g, SHADOW.b, SHADOW.a * alpha);
        batch.draw(card, x + SHADOW_OFF, y - SHADOW_OFF, ICON_W, ICON_H);
        batch.setColor(CARD_BG.r, CARD_BG.g, CARD_BG.b, CARD_BG.a * alpha);
        batch.draw(card, x, y, ICON_W, ICON_H);
        if (s.getIcon() != null) {
            batch.setColor(1f, 1f, 1f, alpha);
            batch.draw(s.getIcon(), x, y, ICON_W, ICON_H);
        }
    }

}
