package com.silverignis.skills.visuals;


import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.particles.Vfx;
import com.silverignis.render.RenderContext;
import com.silverignis.skills.elements.Element;

/** Dark Blast: void sphere drawn entirely by the dark_blast fragment shader,
 *  a dark-lightning trail riding the ball, dark-tinted bursts on impact and
 *  clash. */
final class DarkBlastVisual extends AbstractSkillVisual {

    private static final float QUAD_SCALE = 1.4f;
    private static final Color TINT = Color.WHITE;

    @Override
    protected void react(Trigger t, VisualState state, ParticleEngine engine) {
        switch (t) {
            case CAST -> {
                state.pose.enterAttack();
                play(engine, Vfx.darkTrail(state.dir), track(state.bodyPos));
            }
            case IMPACT, CLASH -> burst(engine, Vfx.impact(Element.DARK), state.impactPos);
            default -> {}
        }
    }

    @Override
    public void render(RenderContext rc, VisualState state) {
        if (hasEnded()) return;
        float w = rc.panelWidth();
        float h = rc.panelRenderHeight();
        Vector2 p = rc.project(state.bodyPos.x, state.bodyPos.z);
        rc.skillShaders.draw(rc.batch, "dark_blast", p.x, p.y + h * 0.5f,
            QUAD_SCALE *w, state.elapsed, state.dir, TINT);
    }
}
