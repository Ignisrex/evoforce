package com.silverignis.skills;

import com.badlogic.gdx.math.MathUtils;

/**
 * The "loading bar" resource: fills over time, can be consumed once full.
 * Pure data + tick logic; no rendering.
 */
public class ChargeMeter {

    private final float max;
    private final float fillRate; // units per second
    private float current;

    public ChargeMeter(float max, float fillRate) {
        this.max = max;
        this.fillRate = fillRate;
        this.current = 0f;
    }

    public void update(float delta) {
        if (current < max) {
            current = MathUtils.clamp(current + fillRate * delta, 0f, max);
        }
    }

    /** Externally add charge — e.g. on kill, on hit landed. */
    public void add(float amount) {
        current = MathUtils.clamp(current + amount, 0f, max);
    }

    public boolean isFull()       { return current >= max; }

    /** Drains the meter back to zero. Call on confirm. */
    public void consume()         { current = 0f; }

    public float getCurrent()     { return current; }
    public float getMax()         { return max; }

    /** 0..1 — convenient for HUD rendering. */
    public float getFillRatio()   { return max <= 0f ? 0f : current / max; }
}
