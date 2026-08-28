package com.silverignis.skills.visuals.aura;

import com.silverignis.skills.visuals.*;
import com.silverignis.skills.visuals.draw.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.render.RenderContext;

/** Shield: a translucent blue dome held in front of the caster (shield.frag),
 *  blooming in and holding until the status drops. */
public final class ShieldVisual extends AbstractSkillVisual {

    private static final float SIZE_SCALE = 1.6f;   // vs the larger panel dimension
    private static final float HOLD_ALPHA = 1.0f;

    private final Color tint = new Color(Color.WHITE);

    @Override
    protected void react(Trigger t, VisualState vs, ParticleEngine engine) {
        switch (t) {
            case CAST   -> vs.pose.enterCast();
            case ACTIVE -> vs.pose.enterIdle();
            default -> {}
        }
    }

    @Override
    public void render(RenderContext rc, VisualState vs) {
        if (hasEnded()) return;
        float scale, alpha;
        switch (vs.phase) {
            case WINDUP   -> { scale = 0.3f + 0.7f * vs.phaseProgress; alpha = 0.5f + 0.5f * vs.phaseProgress; }
            case ACTIVE   -> { scale = 1f; alpha = HOLD_ALPHA; }
            case RECOVERY -> { scale = 1f; alpha = HOLD_ALPHA * (1f - vs.phaseProgress); }
            default       -> { return; }
        }
        float panelW = rc.panelWidth();
        float panelH = rc.panelHeight();
        Vector2 p = rc.project(vs.casterPos.x, vs.casterPos.z);
        float cy = p.y + panelH * 0.5f;
        float size = Math.max(panelW, panelH) * SIZE_SCALE * scale;
        tint.a = alpha;
        rc.skillShaders.draw(rc.batch, "shield", p.x, cy, size, vs.elapsed, vs.dir, tint);
    }
}
