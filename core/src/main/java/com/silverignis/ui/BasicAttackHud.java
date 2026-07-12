package com.silverignis.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.silverignis.assets.GeneratedAssets;
import com.silverignis.entities.Player;
import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillDeck;

/**
 * Always-on HUD that draws the player's basic-attack skill — the icon held in
 * {@code player.getBasicAttack()} — one slot-gap to the right of the X/Y/B
 * column in {@link SlotsHud}. When the skill is on cooldown, a translucent
 * black veil fills the top of the icon and drains downward as the remaining
 * cooldown shrinks.
 */
public class BasicAttackHud {

    // Sized to match SlotsHud so the icon visually fits with the slot column.
    private static final float ICON_W   = 0.80f;
    private static final float ICON_H   = 0.76f;
    private static final float SLOT_GAP = 0.20f;
    private static final float MARGIN_L = 0.4f;
    private static final float MARGIN_B = 1.2f;      // aligned with the X/B front-card baseline in SlotsHud

    // One slot-gap past where the B slot ends (SlotsHud has 3 slots at indices 0..2).
    private static final float ANCHOR_X = MARGIN_L + 3 * (ICON_W + SLOT_GAP);

    private static final Color CARD_BG       = new Color(0.08f, 0.08f, 0.12f, 0.92f);
    private static final Color SHADOW        = new Color(0f, 0f, 0f, 0.35f);
    private static final Color COOLDOWN_VEIL = new Color(0f, 0f, 0f, 0.55f);
    private static final float SHADOW_OFF    = 0.045f;

    private final Texture pixel;
    private final Texture card;
    private final Texture frame;

    public BasicAttackHud(GeneratedAssets generated) {
        this.pixel = generated.pixel();
        this.card  = generated.card();
        this.frame = generated.cardFrame();
    }

    public void render(SpriteBatch batch, Viewport viewport, Player player) {
        float x = ANCHOR_X;
        float y = MARGIN_B;

        Color prev = batch.getColor().cpy();

        batch.setColor(SHADOW);
        batch.draw(card, x + SHADOW_OFF, y - SHADOW_OFF, ICON_W, ICON_H);
        batch.setColor(CARD_BG);
        batch.draw(card, x, y, ICON_W, ICON_H);

        Skill skill = player.getBasicAttack();
        if (skill != null && skill.getIcon() != null) {
            batch.setColor(Color.WHITE);
            batch.draw(skill.getIcon(), x, y, ICON_W, ICON_H);

            SkillDeck deck = player.getDeck();
            if (skill.getCooldown() > 0f && deck.isOnCooldown(skill)) {
                float frac = Math.min(1f, deck.remainingFor(skill) / skill.getCooldown());
                batch.setColor(COOLDOWN_VEIL);
                // ponytail: square veil over a rounded card; inset hides the corner overhang.
                float inset = 0.03f;
                batch.draw(pixel, x + inset, y + ICON_H * (1f - frac), ICON_W - 2 * inset,
                    ICON_H * frac - (frac >= 1f ? inset : 0f));
            }
        }

        batch.setColor(Color.WHITE);
        batch.draw(frame, x, y, ICON_W, ICON_H);

        batch.setColor(prev);
    }

}
