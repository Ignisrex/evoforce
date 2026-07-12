package com.silverignis.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;

/**
 * {@link Bezel} as a {@code Drawable}, for Table/widget backgrounds.
 * The batch color (set by the widget) carries opacity, so parent fades
 * pass through.
 */
final class BezelDrawable extends BaseDrawable {
    private final RoundedRectShader shader;
    private final Color fill, border, glow;
    private final float borderWidth, radius, glowWidth;

    BezelDrawable(RoundedRectShader shader, Color fill, Color border, float borderWidth, float radius) {
        this(shader, fill, border, borderWidth, radius, null, 0f);
    }

    BezelDrawable(RoundedRectShader shader, Color fill, Color border, float borderWidth, float radius,
                  Color glow, float glowWidth) {
        this.shader = shader;
        this.fill = fill;
        this.border = border;
        this.borderWidth = borderWidth;
        this.radius = radius;
        this.glow = glow;
        this.glowWidth = glowWidth;
    }

    @Override
    public void draw(Batch b, float x, float y, float w, float h) {
        shader.draw(b, x, y, w, h, fill, border, borderWidth, radius, glow, glowWidth);
    }
}
