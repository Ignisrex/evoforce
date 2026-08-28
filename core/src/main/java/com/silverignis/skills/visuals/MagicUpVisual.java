package com.silverignis.skills.visuals;

import com.silverignis.particles.ParticleEngine;
import com.silverignis.particles.Vfx;
import com.silverignis.render.RenderContext;

/** Magic Up: arcane sigil stamp and swirl veil, then glints for the buff. No sprite. */
final class MagicUpVisual extends AbstractSkillVisual {

    @Override
    protected void react(Trigger t, VisualState vs, ParticleEngine engine) {
        switch (t) {
            case CAST   -> vs.pose.enterCast();
            case ACTIVE -> { vs.pose.enterIdle(); play(engine, Vfx.magicUp(), track(vs.casterPos)); }
            default -> {}
        }
    }

    @Override
    public void render(RenderContext rc, VisualState vs) {}
}
