package com.silverignis.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.github.tommyettinger.textra.Font;
import com.github.tommyettinger.textra.TypingLabel;

/** {@link TypingLabel} for the 16×9 world-unit stage. TextraTypist 2.2.14 lets a
 *  wrapped line overrun its target by a hardcoded 1 (one pixel in its world, a
 *  whole unit here), so this wraps one unit early to land on the real edge.
 *  Static text by construction ({@code skipToTheEnd}); {EFFECT} markup still animates. */
public final class WorldTextLabel extends TypingLabel {

    private static final float WRAP_SLACK = 1f;

    public WorldTextLabel(String markup, Font font, Color color) {
        super(markup, font, color);
        setWrap(true);
        skipToTheEnd();
    }

    @Override
    public void layout() {
        float width = getWidth() - WRAP_SLACK;
        float originalHeight = workingLayout.getHeight();
        float actualWidth = font.calculateSize(workingLayout);
        if (wrap && width > 0f && (workingLayout.getTargetWidth() != width || actualWidth > width)) {
            workingLayout.setTargetWidth(width);
            workingLayout.setJustification(defaultJustify);
            font.regenerateLayout(workingLayout);
            font.calculateSize(workingLayout);
        }
        float newHeight = workingLayout.getHeight();
        if (!MathUtils.isEqual(originalHeight, newHeight)) {
            setSuperHeight(newHeight);
            invalidateHierarchy();
        }
    }
}
