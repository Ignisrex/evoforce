package com.silverignis.ui;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

/** Shared scene2d helpers for the world-unit UI. */
public final class UiUtil {

    private UiUtil() {}

    /** Tables round child geometry to ints by default, which mangles a 16x9 world-unit stage. */
    public static Table newTable() {
        Table t = new Table();
        t.setRound(false);
        return t;
    }

    public static BitmapFont genFont(FreeTypeFontGenerator g, int px, float worldScale) {
        FreeTypeFontParameter p = new FreeTypeFontParameter();
        p.size = px;
        p.minFilter = Texture.TextureFilter.Linear;
        p.magFilter = Texture.TextureFilter.Linear;
        BitmapFont f = g.generateFont(p);
        f.setUseIntegerPositions(false);
        f.getData().setScale(worldScale);
        return f;
    }

    /**
     * Downward offset of each card in a stack of {@code n} from the top one.
     * Uniform {@code max} while everything fits in {@code space}; otherwise a
     * geometric ratio (found by bisection) shrinks deeper steps, never below
     * {@code min}, so the whole stack stays inside.
     */
    public static float[] stackOffsets(int n, float space, float max, float min) {
        float[] offsets = new float[n];
        int steps = n - 1;
        if (steps <= 0) return offsets;

        float ratio = 1f;
        if (steps * max > space) {
            float lo = 0f, hi = 1f;
            for (int it = 0; it < 24; it++) {
                float mid = (lo + hi) / 2f, sum = 0f, h = max;
                for (int i = 0; i < steps; i++) {
                    sum += Math.max(min, h);
                    h *= mid;
                }
                if (sum > space) hi = mid; else lo = mid;
            }
            ratio = lo;
        }

        float h = max, off = 0f;
        for (int i = 1; i < n; i++) {
            off += Math.max(min, h);
            offsets[i] = off;
            h *= ratio;
        }
        return offsets;
    }
}
