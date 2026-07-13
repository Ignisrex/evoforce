package com.silverignis.ui;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.silverignis.skills.ChargeMeter;

/**
 * The staging-menu charge meter, tucked directly under the {@link LifeBarHud}
 * in the top-left (it borrows that bar's geometry so the pair reads as one
 * cluster). Cyan while filling, gold once full — the same meter recipe as the
 * staging overlay's operator panel. Lives on the GameScreen's HUD stage.
 */
public class ChargeBarHud extends Group {

    private static final float BAR_HEIGHT = 0.18f;
    private static final float GAP_BELOW_LIFE = 0.1f;

    private final Bezel fill;

    public ChargeBarHud(RoundedRectShader shader, Viewport viewport) {
        setPosition(LifeBarHud.MARGIN_LEFT,
                viewport.getWorldHeight() - LifeBarHud.MARGIN_TOP - LifeBarHud.BAR_HEIGHT
                        - GAP_BELOW_LIFE - BAR_HEIGHT);

        Bezel bg = new Bezel(shader).fill(UiTheme.SURF_LOW).radius(UiTheme.CORNER_RADIUS);
        bg.setBounds(0f, 0f, LifeBarHud.BAR_WIDTH, BAR_HEIGHT);
        addActor(bg);

        fill = new Bezel(shader).radius(UiTheme.CORNER_RADIUS);
        addActor(fill);

        Bezel frame = new Bezel(shader).border(UiTheme.OUTLINE_80, UiTheme.BORDER_THIN).radius(UiTheme.CORNER_RADIUS);
        frame.setBounds(0f, 0f, LifeBarHud.BAR_WIDTH, BAR_HEIGHT);
        addActor(frame);
    }

    /** Update the fill from the meter; call once per frame before the stage draws. */
    public void refresh(ChargeMeter meter) {
        fill.fill(meter.isFull() ? UiTheme.GOLD : UiTheme.CYAN_HI);
        fill.setBounds(0f, 0f, LifeBarHud.BAR_WIDTH * meter.getFillRatio(), BAR_HEIGHT);
    }
}
