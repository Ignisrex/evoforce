package com.silverignis.skills.visuals;

import com.silverignis.particles.ParticleEngine;
import com.silverignis.particles.Vfx;
import com.silverignis.render.RenderContext;

/** Heal: green restoration bloom and rising hearts on the caster. No sprite. */
final class HealVisual extends AbstractSkillVisual {

    @Override
    protected void react(Trigger t, VisualState vs, ParticleEngine engine) {
        switch (t) {
            case CAST   -> vs.pose.enterCast();
            case ACTIVE -> { vs.pose.enterIdle(); play(engine, Vfx.heal(), track(vs.casterPos)); }
            default -> {}
        }
    }

    @Override
    public void render(RenderContext rc, VisualState vs) {}
}
