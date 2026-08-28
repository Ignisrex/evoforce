package com.silverignis.skills.visuals.lob;

import com.silverignis.skills.visuals.*;
import com.silverignis.skills.visuals.draw.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.particles.Vfx;
import com.silverignis.render.RenderContext;
import com.silverignis.render.RenderLayer;

/** Venom Bomb's landing cloud: the glob sprite lingering on the tile as a
 *  ground decal (it never had its own zone art), toxic vapor rolling off it. */
public final class VenomBombCloudVisual extends AbstractSkillVisual {

    private final GroundQuad quad = new GroundQuad(
        new TextureRegion(SkillVisuals.assets.texture("skills/sprites/venom_ball.png")), null, Color.WHITE);

    @Override
    protected void react(Trigger t, VisualState vs, ParticleEngine engine) {
        if (t == Trigger.ACTIVE) play(engine, Vfx.toxicClouds(), track(vs.bodyPos));
    }

    @Override
    public void render(RenderContext rc, VisualState vs) {
        if (hasEnded()) return;
        quad.draw(rc, vs);
    }

    @Override
    public RenderLayer layer() { return RenderLayer.GROUND; }
}
