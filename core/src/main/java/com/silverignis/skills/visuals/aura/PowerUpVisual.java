package com.silverignis.skills.visuals.aura;

import com.silverignis.skills.visuals.*;
import com.silverignis.skills.visuals.draw.*;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.particles.Vfx;
import com.silverignis.render.RenderContext;

/** Power Up: red-orange surge, then flames and embers burning for the buff. No sprite. */
public final class PowerUpVisual extends AbstractSkillVisual {

    @Override
    protected void react(Trigger t, VisualState vs, ParticleEngine engine) {
        switch (t) {
            case CAST   -> vs.pose.enterCast();
            case ACTIVE -> { vs.pose.enterIdle(); play(engine, Vfx.powerUp(), track(vs.casterPos)); }
            default -> {}
        }
    }

    @Override
    public void render(RenderContext rc, VisualState vs) {}
}
