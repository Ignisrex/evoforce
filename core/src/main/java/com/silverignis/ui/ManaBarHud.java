package com.silverignis.ui;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.silverignis.components.ManaPool;

public class ManaBarHud extends Group {

    public static final float BAR_HEIGHT = 0.18f;
    public static final float GAP_BELOW_LIFE = 0.1f;

    /** Width of the half-bar gate tick. */
    private static final float GATE_TICK_W = 0.03f;

    private final Bezel fill;
    private final Bezel gate;
    private final Bezel frame;

    public ManaBarHud(RoundedRectShader shader) {
        Bezel bg = new Bezel(shader).fill(UiTheme.SURF_LOW).radius(UiTheme.CORNER_RADIUS);
        bg.setBounds(0f, 0f, LifeBarHud.BAR_WIDTH, BAR_HEIGHT);
        addActor(bg);

        fill = new Bezel(shader).radius(UiTheme.CORNER_RADIUS);
        addActor(fill);

        // Marks the half-bar staging gate, so the threshold is visible rather than learned.
        gate = new Bezel(shader);
        gate.setBounds((LifeBarHud.BAR_WIDTH - GATE_TICK_W) / 2f, 0f, GATE_TICK_W, BAR_HEIGHT);
        addActor(gate);

        frame = new Bezel(shader).border(UiTheme.OUTLINE_80, UiTheme.BORDER_THIN).radius(UiTheme.CORNER_RADIUS);
        frame.setBounds(0f, 0f, LifeBarHud.BAR_WIDTH, BAR_HEIGHT);
        addActor(frame);
    }

    public void refresh(ManaPool mana) {
        float ratio = mana.getCurrent() / mana.getMax();
        fill.fill(UiTheme.MANA);
        fill.setBounds(0f, 0f, LifeBarHud.BAR_WIDTH * ratio, BAR_HEIGHT);

        // Three readings off one bar: can't stage, can stage, full bar.
        boolean canOpen = mana.canOpenMenu();
        gate.fill(canOpen ? UiTheme.CYAN_HI : UiTheme.OUTLINE_80);
        if (ratio >= 1f) {
            frame.glow(UiTheme.withA(UiTheme.MANA, 0.5f), UiTheme.GLOW_WIDTH);
        } else if (canOpen) {
            frame.glow(UiTheme.withA(UiTheme.CYAN, 0.35f), UiTheme.GLOW_WIDTH);
        } else {
            frame.glow(null, 0f);
        }
    }
}
