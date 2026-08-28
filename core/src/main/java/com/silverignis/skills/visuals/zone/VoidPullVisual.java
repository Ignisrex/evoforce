package com.silverignis.skills.visuals.zone;


import com.silverignis.skills.visuals.*;
import com.silverignis.skills.visuals.draw.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.particles.Vfx;
import com.silverignis.render.RenderContext;
import com.silverignis.render.RenderLayer;

public final class VoidPullVisual extends AbstractSkillVisual {

    private static final Color VIOLET = Color.valueOf("8a2be2ff");

    private final GroundQuad quad = new GroundQuad(null, looping(), VIOLET);

    private static Animation<TextureRegion> looping() {
        Animation<TextureRegion> a = SkillVisuals.assets.sheet("skills/animations/void_pull_spritesheet.png", 192, 192, 0.08f);
        a.setPlayMode(Animation.PlayMode.LOOP);
        return a;
    }

    @Override
    protected void react(Trigger t, VisualState vs, ParticleEngine engine) {
        if (t==Trigger.ACTIVE) play(engine, Vfx.voidPull(), track(vs.bodyPos));
    }

    @Override
    public void render(RenderContext rc, VisualState vs) {
        if (hasEnded()) return;
        quad.draw(rc, vs);
    }

    @Override
    public RenderLayer layer() {
        return RenderLayer.GROUND;
    }
}
