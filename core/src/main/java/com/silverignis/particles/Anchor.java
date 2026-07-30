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

    /** Spawn point sweeping an inward spiral around (cx, cz) in the x/z plane — advances
     *  one step per call, so a burst(totalSpawns, window) emitter animates a converging
     *  vortex out of its own emission trail. Stateful: use with a single-emitter effect,
     *  one play() per arm ({@code phaseDeg} offsets the start angle). */
    static Anchor spiralIn(float cx, float cy, float cz, float startRadius, float revolutions,
                           int totalSpawns, float phaseDeg) {
        float[] step = {0f};
        float last = Math.max(1, totalSpawns - 1);
        return out -> {
            float t = Math.min(step[0]++ / last, 1f);
            float ang = phaseDeg * MathUtils.degRad + t * revolutions * MathUtils.PI2;
            float r = startRadius * (1f - t);
            out.set(cx + MathUtils.cos(ang) * r, cy, cz + MathUtils.sin(ang) * r);
        };
    }

    /** Random point on the edge of an x/z rectangle (center ± half-extents) — spawn from a frame, not its middle. */
    static Anchor rim(float cx, float cy, float cz, float hx, float hz) {
        return out -> {
            boolean topOrBottom = MathUtils.randomBoolean();
            float x = topOrBottom ? MathUtils.random(-hx, hx) : (MathUtils.randomBoolean() ? hx : -hx);
            float z = topOrBottom ? (MathUtils.randomBoolean() ? hz : -hz) : MathUtils.random(-hz, hz);
            out.set(cx + x, cy, cz + z);
        };
    }
}
