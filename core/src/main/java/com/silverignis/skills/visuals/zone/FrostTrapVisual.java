package com.silverignis.skills.visuals.zone;

import com.silverignis.skills.visuals.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.particles.Vfx;
import com.silverignis.render.RenderContext;
import com.silverignis.render.RenderLayer;

/** Frost Trap: a sheet of ice frozen onto the tile ahead (frost_trap.frag),
 *  cold fog creeping off it while it holds. */
public final class FrostTrapVisual extends AbstractSkillVisual {

    private static final float HOLD_ALPHA = 1f;

    private final Color tint = new Color(Color.WHITE);

    @Override
    protected void react(Trigger t, VisualState vs, ParticleEngine engine) {
        if (t == Trigger.ACTIVE) play(engine, Vfx.frostFog(), track(vs.bodyPos));
    }

    @Override
    public void render(RenderContext rc, VisualState vs) {
        if (hasEnded()) return;
        float scale, alpha;
        switch (vs.phase) {
            case WINDUP   -> { scale = 0.6f + 0.4f * vs.phaseProgress; alpha = vs.phaseProgress; }
            case ACTIVE   -> { scale = 1f; alpha = HOLD_ALPHA; }
            case RECOVERY -> { scale = 1f; alpha = HOLD_ALPHA * (1f - vs.phaseProgress); }
            default       -> { return; }
        }
        float depth = rc.depthScale(vs.bodyPos.z);
        float w = rc.panelWidth() * depth * scale;
        float h = rc.panelRenderHeight() * depth * scale;
        Vector2 p = rc.project(vs.bodyPos.x, vs.bodyPos.z);
        tint.a = alpha;
        rc.skillShaders.draw(rc.batch, "frost_trap", p.x, p.y, w, h, vs.elapsed, vs.dir, tint);
    }

    @Override
    public RenderLayer layer() { return RenderLayer.GROUND; }
}
