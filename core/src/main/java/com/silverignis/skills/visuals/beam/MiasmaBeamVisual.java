package com.silverignis.skills.visuals.beam;

import com.silverignis.skills.visuals.*;
import com.silverignis.skills.visuals.draw.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.particles.Vfx;
import com.silverignis.render.RenderContext;

/** Miasma beam: sickly violet ray. Static beam sprite for now — swap the
 *  one-frame animation for a proper sheet when the art lands. */
public final class MiasmaBeamVisual extends AbstractSkillVisual {

    private final BeamQuad quad = new BeamQuad(
        new Animation<>(1f, new TextureRegion(SkillVisuals.assets.texture("skills/sprites/beam.png"))),
        new Color(0.62f, 0.35f, 0.85f, 0.9f));

    @Override
    protected void react(Trigger t, VisualState vs, ParticleEngine engine) {
        switch (t) {
            case CAST -> vs.pose.enterCast();
            case ACTIVE -> play(engine, Vfx.beamEmbers(Vfx.tint(vs.element), vs.dir),
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
