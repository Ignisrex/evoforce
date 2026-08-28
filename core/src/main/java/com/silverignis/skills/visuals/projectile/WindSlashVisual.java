package com.silverignis.skills.visuals.projectile;

import com.silverignis.skills.visuals.*;
import com.silverignis.skills.visuals.draw.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.particles.Vfx;
import com.silverignis.render.RenderContext;

public final class WindSlashVisual extends  AbstractSkillVisual {
    private final Sprite sprite = new Sprite(SkillVisuals.assets.texture("skills/sprites/wind_slash.png"));

    /** Wind quad side in panel widths — twice the sprite, so the lines can
     *  trail a full sprite-width behind it (per the concept sketch). */
    private static final float WIND_QUAD_SCALE = 2.0f;

    @Override
    protected void react(Trigger t, VisualState state, ParticleEngine engine) {
        switch(t) {
            case CAST -> {
                state.pose.enterAttack();
                if (state.dir < 0) sprite.setFlip(true, false);
            }
            case IMPACT, CLASH -> burst(engine, Vfx.impact(state.element), state.impactPos);
            default -> {}
        }
    }

    @Override
    public void render(RenderContext rc, VisualState state) {
        if (hasEnded()) return;
        float depth = rc.depthScale(state.bodyPos.z);
        float w = rc.panelWidth() * depth;
        float h = rc.panelRenderHeight() * depth;
        Vector2 p = rc.project(state.bodyPos.x, state.bodyPos.z);
        // Wind layer first, slash sprite on top of its own slipstream. The quad
        // is sprite-height so the shader's y-space IS the crescent's height.
        rc.skillShaders.draw(rc.batch, "wind_slash", p.x, p.y + h * 0.5f,
                WIND_QUAD_SCALE * w, h, state.elapsed, state.dir, Color.WHITE);
        sprite.setSize(w, h);
        sprite.setPosition(p.x - w * 0.5f, p.y);
        sprite.draw(rc.batch);
    }
}
