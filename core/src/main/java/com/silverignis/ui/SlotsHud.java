package com.silverignis.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.silverignis.skills.Skill;
import com.silverignis.skills.slots.ButtonSlot;
import com.silverignis.skills.slots.SkillSlots;
import com.silverignis.skills.slots.SlotKey;

import java.util.List;

/**
 * Battle-time X / Y / B slot stacks in the bottom-left, staggered like
 * controller face buttons (Y raised). A Scene2D actor on the GameScreen's
 * HUD stage, drawn with the same SDF bezels and {@link UiTheme} palette as
 * the staging overlay so the two read as one system.
 *
 * <p>Only the top-of-stack skill gets the full framed card; deeper cards peek
 * out underneath as compressed slivers. Sliver height is adaptive: uniform
 * {@link #SLIVER_MAX} while the stack fits, geometric compression
 * (top-weighted) once it would overflow the space below.
 *
 * <p>{@link #refresh(SkillSlots)} rebuilds the children each frame — the same
 * rebuild-per-frame pattern as the staging overlay. Pure rendering; never
 * writes back to the slots.
 */
public class SlotsHud extends Group {

    // Cards are small landscape rectangles, slightly bigger than the
    // ~0.55-diameter circular icon textures they enclose.
    private static final float CARD_W   = 0.80f;      // world units per card (width)
    private static final float CARD_H   = 0.76f;      // world units per card (height)
    private static final float SLOT_GAP = 0.20f;
    private static final float MARGIN_L = 0.4f;       // left padding from world edge
    private static final float MARGIN_B = 0.1f;       // stack may grow down to here
    private static final float BASE_Y   = 1.2f;       // top-card y for X and B
    private static final float STAGGER  = 0.35f;      // Y slot raised above X/B
    private static final float ICON_INSET = 0.08f;

    // Stack slivers: uniform SLIVER_MAX while the stack fits, geometric
    // compression (top-weighted) once it would overflow the space below.
    private static final float SLIVER_MAX = 0.44f;
    private static final float SLIVER_MIN = 0.06f;
    private static final float ALPHA_DECAY = 0.85f;
    private static final float ALPHA_MIN   = 0.35f;

    private final RoundedRectShader shader; // shared battle-HUD instance; GameScreen owns it
    private final BitmapFont keyFont;
    private final Label.LabelStyle keyStyle;

    public SlotsHud(RoundedRectShader shader, Viewport viewport) {
        this.shader = shader;

        float worldScale = viewport.getWorldHeight() / Gdx.graphics.getHeight();
        FreeTypeFontGenerator mono = new FreeTypeFontGenerator(Gdx.files.internal("ui/fonts/JetBrainsMono-Bold.ttf"));
        FreeTypeFontParameter p = new FreeTypeFontParameter();
        p.size = 13;
        p.minFilter = Texture.TextureFilter.Linear;
        p.magFilter = Texture.TextureFilter.Linear;
        keyFont = mono.generateFont(p);
        keyFont.setUseIntegerPositions(false);
        keyFont.getData().setScale(worldScale);
        mono.dispose();
        keyStyle = new Label.LabelStyle(keyFont, Color.WHITE);
    }

    /** Rebuild from the current slots; call once per frame before the stage draws. */
    public void refresh(SkillSlots slots) {
        clearChildren();
        SlotKey[] keys = SlotKey.values();
        for (int i = 0; i < keys.length; i++) {
            float x = MARGIN_L + i * (CARD_W + SLOT_GAP);
            float y = keys[i] == SlotKey.Y ? BASE_Y + STAGGER : BASE_Y;
            buildStack(keys[i], slots.get(keys[i]), x, y);
        }
    }

    private void buildStack(SlotKey key, ButtonSlot slot, float x, float topY) {
        List<Skill> stack = slot.view();

        // Deeper cards first so nearer ones draw on top; each is a full card
        // offset downward, so only its bottom strip peeks out.
        // ponytail: cards pushed past the bottom margin just aren't drawn —
        // add a "+N" badge if stacks ever get deep enough to matter.
        if (!stack.isEmpty()) {
            float[] offsets = sliverOffsets(stack.size(), topY - MARGIN_B);
            for (int i = stack.size() - 1; i >= 1; i--) {
                float y = topY - offsets[i];
                if (y < MARGIN_B) continue;
                float alpha = Math.max(ALPHA_MIN, (float) Math.pow(ALPHA_DECAY, i));
                sliverCard(stack.get(i), x, y, alpha);
            }
        }
        topCard(key, stack.isEmpty() ? null : stack.get(0), x, topY);
    }

    /** A deeper card peeking out under the top one. */
    private void sliverCard(Skill s, float x, float y, float alpha) {
        Group card = card(s, x, y, UiTheme.CARD_70, UiTheme.OUTLINE_V_60, false);
        card.getColor().a = alpha; // fades bezel + icon together via parentAlpha
    }

    /** The framed face of the stack — or the dim empty slot when {@code s} is null. */
    private void topCard(SlotKey key, Skill s, float x, float y) {
        boolean filled = s != null;
        Group card = filled
                ? card(s, x, y, UiTheme.CARD, UiTheme.GOLD, true)
                : card(null, x, y, UiTheme.SURF_LOW, UiTheme.OUTLINE_V_60, false);

        Label letter = new Label(key.name(), keyStyle);
        letter.setColor(filled ? UiTheme.GOLD : UiTheme.OUTLINE);
        letter.pack();
        letter.setPosition(CARD_W - 0.06f - letter.getWidth(), 0.16f - letter.getHeight());
        card.addActor(letter);
    }

    /** Card scaffold: a group at ({@code x},{@code y}) holding a styled bezel + optional icon. */
    private Group card(Skill s, float x, float y, Color fill, Color border, boolean glow) {
        Group g = new Group();
        g.setPosition(x, y);

        Bezel bezel = new Bezel(shader).fill(fill).border(border, UiTheme.BORDER).radius(UiTheme.CORNER_RADIUS);
        if (glow) bezel.glow(UiTheme.withA(UiTheme.GOLD, 0.4f), UiTheme.GLOW_WIDTH);
        bezel.setBounds(0f, 0f, CARD_W, CARD_H);
        g.addActor(bezel);

        if (s != null && s.getIcon() != null) {
            Image icon = new Image(s.getIcon());
            icon.setBounds(ICON_INSET, ICON_INSET, CARD_W - 2 * ICON_INSET, CARD_H - 2 * ICON_INSET);
            g.addActor(icon);
        }
        addActor(g);
        return g;
    }

    /**
     * Downward offset of each stacked card from the top card. Uniform
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

    public void dispose() {
        keyFont.dispose(); // the shared shader is GameScreen's to dispose
    }
}
