package com.silverignis.skills.visuals;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.particles.Vfx;
import com.silverignis.render.RenderContext;

final class ElectroBallVisual extends AbstractSkillVisual {

    private static final float BALL_SCALE = 0.55f;

    private final Sprite sprite = new Sprite(SkillVisuals.assets.textureOnDemand("effects/electro_ball.png"));

    @Override
    protected void react(Trigger t, VisualState vs, ParticleEngine engine) {
        switch (t) {
            case CAST -> {
                vs.pose.enterAttack();
                if (vs.dir < 0) sprite.setFlip(true, false);
                play(engine, Vfx.electricArcs(vs.element), track(vs.bodyPos));
                play(engine, Vfx.energyMotes(vs.element), track(vs.bodyPos));
            }
            default -> {}
        }
    }

    @Override
    public void render(RenderContext rc, VisualState vs) {
        if (hasEnded()) return;
        float size = rc.panelWidth() * BALL_SCALE;
        Vector2 p = rc.project(vs.bodyPos.x, vs.bodyPos.z);
        float y = p.y + vs.bodyPos.y * rc.depthScale(vs.bodyPos.z);
        sprite.setSize(size, size);
        sprite.setPosition(p.x - size * 0.5f, y);
        sprite.draw(rc.batch);
    }
}
