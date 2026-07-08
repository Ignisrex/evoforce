package com.silverignis.particles;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.silverignis.systems.combat.Combatant;

@FunctionalInterface
public interface Anchor {

    void point(Vector3 out);

    static Anchor at(float x, float y, float z) { return out -> out.set(x, y, z); }
    static Anchor follow(Combatant c) { return c::worldPos; }

    /** Uniform random point inside an axis-aligned box (center ± half-extents). */
    static Anchor region(float cx, float cy, float cz, float hx, float hy, float hz) {
        return out -> out.set(
            cx + MathUtils.random(-hx, hx),
            cy + MathUtils.random(-hy, hy),
            cz + MathUtils.random(-hz, hz));
    }
}
