package com.silverignis.particles;

import com.badlogic.gdx.math.MathUtils;

public final class Val {

    public final float min, max;
    private Val(float min, float max) { this.min = min; this.max = max; }

    public static Val of(float c) { return new Val(c, c); }
    public static Val range(float a, float b) { return new Val(a, b); }

    public float sample() { return min == max ? min : MathUtils.random(min, max); }
}
