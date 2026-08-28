package com.silverignis.skills.visuals;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.particles.Vfx;
import com.silverignis.render.RenderContext;

final class FireBlastVisual extends AbstractSkillVisual {

    private final Sprite sprite = new Sprite(SkillVisuals.assets.textureOnDemand("effects/fireball.png"));

    @Override
    protected void react(Trigger t, VisualState state, ParticleEngine engine) {
        switch(t) {
            case CAST -> {
                state.pose.enterAttack();
                if (state.dir < 0) sprite.setFlip(true, false);
                play(engine, Vfx.fireTrail(state.dir), track(state.bodyPos));
            }
            case IMPACT, CLASH -> burst(engine, Vfx.impact(state.element), state.impactPos);
            default -> {}
        }
    }

    @Override
    public void render(RenderContext rc, VisualState state) {
        if (hasEnded()) return;
        float w = rc.panelWidth();
        float h = rc.panelRenderHeight();
        Vector2 p = rc.project(state.bodyPos.x, state.bodyPos.z);
        sprite.setSize(w, h);
        sprite.setPosition(p.x - w * 0.5f, p.y);
        sprite.draw(rc.batch);
    }
}
