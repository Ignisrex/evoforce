package com.silverignis.ui;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.silverignis.components.ManaPool;

public class ManaBarHud extends Group {

    public static final float BAR_HEIGHT = 0.18f;
    public static final float GAP_BELOW_LIFE = 0.1f;

    private final Bezel fill;
    private final Bezel frame;

    public ManaBarHud(RoundedRectShader shader) {
        Bezel bg = new Bezel(shader).fill(UiTheme.SURF_LOW).radius(UiTheme.CORNER_RADIUS);
        bg.setBounds(0f, 0f, LifeBarHud.BAR_WIDTH, BAR_HEIGHT);
        addActor(bg);

        fill = new Bezel(shader).radius(UiTheme.CORNER_RADIUS);
        addActor(fill);

        frame = new Bezel(shader).border(UiTheme.OUTLINE_80, UiTheme.BORDER_THIN).radius(UiTheme.CORNER_RADIUS);
        frame.setBounds(0f, 0f, LifeBarHud.BAR_WIDTH, BAR_HEIGHT);
        addActor(frame);
    }

    public void refresh(ManaPool mana) {
        float ratio = mana.getCurrent() / mana.getMax();
        fill.fill(UiTheme.MANA);
        fill.setBounds(0f, 0f, LifeBarHud.BAR_WIDTH * ratio, BAR_HEIGHT);
        if(ratio>=1f) {
            frame.glow(UiTheme.withA(UiTheme.MANA, 0.5f), UiTheme.GLOW_WIDTH);
        }else {
            frame.glow(null, 0f);
        }
    }
}
