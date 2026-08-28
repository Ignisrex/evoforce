package com.silverignis.skills.visuals;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.particles.Vfx;
import com.silverignis.render.RenderContext;
import com.silverignis.render.RenderLayer;

/** Electro Ball's landing field: electric decal on the tile with bolts
 *  striking out of it and charged motes rising off. */
final class ElectroBallCloudVisual extends AbstractSkillVisual {

    private final GroundQuad quad = new GroundQuad(
        new TextureRegion(SkillVisuals.assets.texture("skills/sprites/electro_zone.png")), null, Color.WHITE);

    @Override
    protected void react(Trigger t, VisualState vs, ParticleEngine engine) {
        if (t == Trigger.ACTIVE) {
            play(engine, Vfx.electricArcs(vs.element), track(vs.bodyPos));
            play(engine, Vfx.energyMotes(vs.element), track(vs.bodyPos));
        }
    }

    @Override
    public void render(RenderContext rc, VisualState vs) {
        if (hasEnded()) return;
        quad.draw(rc, vs);
    }

    @Override
    public RenderLayer layer() { return RenderLayer.GROUND; }
}
