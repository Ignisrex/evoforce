package com.silverignis.skills.visuals;

import com.silverignis.particles.ParticleEngine;
import com.silverignis.particles.Vfx;
import com.silverignis.render.RenderContext;

/** Regen: a dim breathing halo and a few rising motes while the status ticks. No sprite. */
final class RegenVisual extends AbstractSkillVisual {

    @Override
    protected void react(Trigger t, VisualState vs, ParticleEngine engine) {
        switch (t) {
            case CAST   -> vs.pose.enterCast();
            case ACTIVE -> { vs.pose.enterIdle(); play(engine, Vfx.regen(), track(vs.casterPos)); }
            default -> {}
        }
    }

    @Override
    public void render(RenderContext rc, VisualState vs) {}
}
