package com.silverignis.skills.visuals.projectile;

import com.silverignis.skills.visuals.*;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.particles.Vfx;
import com.silverignis.render.RenderContext;

public final class VoidCollapseVisual extends AbstractSkillVisual {

    /** Drawn larger than a one-row projectile so the sprite reads as covering both rows. */
    private static final float SPAN_SCALE = 1.8f;

    private final Sprite sprite = new Sprite(SkillVisuals.assets.texture("skills/sprites/void_orb.png"));

    @Override
    protected void react(Trigger t, VisualState state, ParticleEngine engine) {
        switch(t) {
            case CAST -> state.pose.enterAttack();
            case IMPACT, CLASH -> burst(engine, Vfx.impact(state.element), state.impactPos);
            default -> {}
        }
    }

    @Override
    public void render(RenderContext rc, VisualState state) {
        if (hasEnded()) return;
        float depth = rc.depthScale(state.bodyPos.z);
        float w = rc.panelWidth() * depth * SPAN_SCALE;
        float h = rc.panelRenderHeight() * depth * SPAN_SCALE;
        Vector2 p = rc.project(state.bodyPos.x, state.bodyPos.z);
        sprite.setSize(w, h);
        sprite.setPosition(p.x - w * 0.5f, p.y);
        sprite.draw(rc.batch);
    }
}
