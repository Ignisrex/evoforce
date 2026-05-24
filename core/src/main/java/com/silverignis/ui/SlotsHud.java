package com.silverignis.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.silverignis.skills.Skill;
import com.silverignis.skills.slots.ButtonSlot;
import com.silverignis.skills.slots.SkillSlots;
import com.silverignis.skills.slots.SlotKey;

import java.util.List;

/**
 * Always-on HUD that draws the X / Y / B button slots in the bottom-right.
 * Each slot is a small column of up to {@link ButtonSlot#CAPACITY} icons,
 * front-of-queue at the top.
 *
 * <p>Pure rendering — reads the {@link SkillSlots} object the {@code GameScreen}
 * owns and never writes back.
 */
public class SlotsHud {

    // Icons are drawn as small landscape rectangles, slightly bigger than the
    // ~0.55-diameter circular icon textures they enclose.
    private static final float ICON_W    = 0.70f;     // world units per icon (width)
    private static final float ICON_H    = 0.60f;     // world units per icon (height)
    private static final float ICON_GAP  = 0.06f;
    private static final float SLOT_GAP  = 0.20f;
    private static final float MARGIN_L  = 0.4f;      // left padding from world edge
    private static final float MARGIN_B  = 0.4f;      // bottom padding

    private static final Color SLOT_BG    = new Color(0.10f, 0.10f, 0.15f, 0.75f);
    private static final Color SLOT_LABEL = new Color(0.85f, 0.85f, 0.95f, 1f);
    private static final Color EMPTY_TINT = new Color(1f, 1f, 1f, 0.20f);

    private final Texture pixel;

    public SlotsHud(Texture pixel) {
        this.pixel = pixel;
    }

    public void render(SpriteBatch batch, Viewport viewport, SkillSlots slots) {
        SlotKey[] keys = SlotKey.values();
        float startX   = MARGIN_L;
        float baseY    = MARGIN_B;

        Color prev = batch.getColor().cpy();

        for (int i = 0; i < keys.length; i++) {
            float x = startX + i * (ICON_W + SLOT_GAP);
            drawSlot(batch, x, baseY, slots.get(keys[i]));
        }

        batch.setColor(prev);
    }

    private void drawSlot(SpriteBatch batch, float x, float y, ButtonSlot slot) {
        float colHeight = ButtonSlot.CAPACITY * ICON_H
                        + (ButtonSlot.CAPACITY - 1) * ICON_GAP;

        // Background panel covering the whole column + a bit of label space below.
        float panelPad = 0.06f;
        batch.setColor(SLOT_BG);
        batch.draw(pixel,
            x - panelPad, y - panelPad - 0.12f,
            ICON_W + 2 * panelPad,
            colHeight + 2 * panelPad + 0.12f);

        // Each capacity row, top = front of queue.
        List<Skill> queue = slot.view();
        for (int i = 0; i < ButtonSlot.CAPACITY; i++) {
            float iconY = y + (ButtonSlot.CAPACITY - 1 - i) * (ICON_H + ICON_GAP);

            if (i < queue.size()) {
                Skill s = queue.get(i);
                if (s.getIcon() != null) {
                    batch.setColor(Color.WHITE);
                    batch.draw(s.getIcon(), x, iconY, ICON_W, ICON_H);
                }
            } else {
                // Empty cell — dim placeholder.
                batch.setColor(EMPTY_TINT);
                batch.draw(pixel, x, iconY, ICON_W, ICON_H);
            }
        }

        // Slot key label could go here once a font is wired in. For now the
        // visual order (X / Y / B left-to-right) matches the action enum.
        batch.setColor(SLOT_LABEL);
        // No font draw yet — leave the label drawing to the caller if desired.
    }

}
