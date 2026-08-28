package com.silverignis.skills.visuals;

import com.badlogic.gdx.graphics.Color;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.particles.Vfx;
import com.silverignis.render.RenderContext;

/** Ice Beam: animated freezing stream across the row with icy vapor rolling off it. */
final class IceBeamVisual extends AbstractSkillVisual {

    private final BeamQuad quad = new BeamQuad(
        SkillVisuals.assets.sheet("skills/animations/icebeam_spritesheet.png", 256, 128, 0.1f),
        Color.WHITE);

    @Override
    protected void react(Trigger t, VisualState vs, ParticleEngine engine) {
        switch (t) {
            case CAST     -> vs.pose.enterCast();
            case ACTIVE   -> play(engine, Vfx.beamIceMist(Vfx.tint(vs.element), vs.dir),
                                  BeamQuad.alongSpan(vs), BeamQuad.intensity(vs));
            case RECOVERY -> vs.pose.enterIdle();
            default -> {}
        }
    }

    @Override
    public void render(RenderContext rc, VisualState vs) {
        if (hasEnded()) return;
        quad.draw(rc, vs);
    }
}
