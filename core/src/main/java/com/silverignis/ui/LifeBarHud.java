package com.silverignis.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.silverignis.entities.Player;

/**
 * Player HP bar anchored top-left, on the GameScreen's HUD stage. Fill ratio
 * is {@code hp / maxHp}; tint shifts green → yellow → red as HP drops (these
 * are functional colors, deliberately not {@link UiTheme}). The pill frame
 * matches the staging overlay's meter recipe. {@link ChargeBarHud} hangs its
 * geometry off this bar's constants so the cluster moves together.
 */
public class LifeBarHud extends Group {

    static final float MARGIN_LEFT = 0.4f;   // world units from left edge
    static final float MARGIN_TOP  = 0.3f;   // world units from top edge
    static final float BAR_WIDTH   = 4.0f;
    static final float BAR_HEIGHT  = 0.4f;

    private static final Color HIGH = new Color(0.25f, 0.85f, 0.30f, 1f);
    private static final Color MID  = new Color(1.00f, 0.85f, 0.20f, 1f);
    private static final Color LOW  = new Color(0.95f, 0.25f, 0.20f, 1f);

    private final Bezel fill;

    public LifeBarHud(RoundedRectShader shader, Viewport viewport) {
        setPosition(MARGIN_LEFT, viewport.getWorldHeight() - MARGIN_TOP - BAR_HEIGHT);

        Bezel bg = new Bezel(shader).fill(UiTheme.SURF_LOW).radius(UiTheme.CORNER_RADIUS);
        bg.setBounds(0f, 0f, BAR_WIDTH, BAR_HEIGHT);
        addActor(bg);

        fill = new Bezel(shader).radius(UiTheme.CORNER_RADIUS);
        addActor(fill);

        Bezel frame = new Bezel(shader).border(UiTheme.OUTLINE_80, UiTheme.BORDER_THIN).radius(UiTheme.CORNER_RADIUS);
        frame.setBounds(0f, 0f, BAR_WIDTH, BAR_HEIGHT);
        addActor(frame);
    }

    /** Update the fill from the player's HP; call once per frame before the stage draws. */
    public void refresh(Player player) {
        int maxHp = Math.max(1, player.getMaxHp());
        float ratio = Math.clamp(player.getHp() / (float) maxHp, 0f, 1f);
        fill.fill(ratio > 0.5f ? HIGH : ratio > 0.25f ? MID : LOW);
        fill.setBounds(0f, 0f, BAR_WIDTH * ratio, BAR_HEIGHT);
    }
}
