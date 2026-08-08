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

    /** Spawn point sweeping a spiral around (cx, cz) in the x/z plane (inward or outward,
     *  per start radius vs end extents) — advances one step per call, so a
     *  burst(totalSpawns, window) emitter animates a vortex out of its own emission trail.
     *  The end shape is the border of an endHx×endHz rectangle (per-angle radius), so an
     *  outward sweep pushes into the corners instead of stopping on an inscribed circle.
     *  The radius lands on that border at {@code settle} (fraction of spawns); angle keeps
     *  advancing the whole time, so the remainder orbits tracing the border.
     *  Stateful: use with a single-emitter effect, one play() per arm ({@code phaseDeg}
     *  offsets the start angle). */
    static Anchor spiralIn(float cx, float cy, float cz, float startRadius, float endHx,
                           float endHz, float revolutions, int totalSpawns, float phaseDeg,
                           float settle) {
        float[] step = {0f};
        float last = Math.max(1, totalSpawns - 1);
        return out -> {
            float t = Math.min(step[0]++ / last, 1f);
            float ang = phaseDeg * MathUtils.degRad + t * revolutions * MathUtils.PI2;
            float cos = MathUtils.cos(ang), sin = MathUtils.sin(ang);
            float end = Math.min(endHx / Math.max(Math.abs(cos), 1e-4f),
                                 endHz / Math.max(Math.abs(sin), 1e-4f));
            float rt = Math.min(t / settle, 1f);
            float r = end + (startRadius - end) * (1f - rt);
            out.set(cx + cos * r, cy, cz + sin * r);
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
