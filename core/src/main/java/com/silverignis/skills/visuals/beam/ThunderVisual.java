package com.silverignis.skills.visuals.beam;

import com.silverignis.skills.visuals.*;
import com.silverignis.skills.visuals.draw.*;
import com.badlogic.gdx.graphics.Color;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.particles.Vfx;
import com.silverignis.render.RenderContext;

/** Thunder: animated lightning beam across the row, embers and crackling
 *  discharge coming off it. */
public final class ThunderVisual extends AbstractSkillVisual {

    private final BeamQuad quad = new BeamQuad(
        SkillVisuals.assets.sheet("skills/animations/thunder_spritesheet.png", 192, 192, 0.08f),
        Color.WHITE);

    @Override
    protected void react(Trigger t, VisualState vs, ParticleEngine engine) {
        switch (t) {
            case CAST -> vs.pose.enterCast();
            case ACTIVE -> {
                play(engine, Vfx.beamEmbers(Vfx.tint(vs.element), vs.dir),
                     BeamQuad.alongSpan(vs), BeamQuad.intensity(vs));
                play(engine, Vfx.crackle(vs.element),
                     BeamQuad.alongSpan(vs), BeamQuad.intensity(vs));
            }
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
