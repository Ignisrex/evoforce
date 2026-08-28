package com.silverignis.skills.visuals;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.render.RenderContext;
import com.silverignis.render.RenderLayer;

/** Frost Trap: a freezing decal on the tile ahead, drawn on the ground. */
final class FrostTrapVisual extends AbstractSkillVisual {

    private final GroundQuad quad = new GroundQuad(
        new TextureRegion(SkillVisuals.assets.textureOnDemand("effects/zone.png")), null, Color.WHITE);

    @Override
    protected void react(Trigger t, VisualState vs, ParticleEngine engine) {}

    @Override
    public void render(RenderContext rc, VisualState vs) {
        if (hasEnded()) return;
        quad.draw(rc, vs);
    }

    @Override
    public RenderLayer layer() { return RenderLayer.GROUND; }
}
