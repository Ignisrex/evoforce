package com.silverignis.skills.visuals.draw;

import com.silverignis.skills.visuals.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.silverignis.entities.Battlefield;
import com.silverignis.particles.Anchor;
import com.silverignis.particles.Drive;
import com.silverignis.render.RenderContext;

/** Drawing helper for row-spanning beam bodies: an animated quad from the tile
 *  ahead of the caster to the far grid edge, growing out during WINDUP, held
 *  through ACTIVE, and receding downrange (far end pinned) through RECOVERY.
 *  A tool per-skill visuals call from their own render — not a base class. */
public final class BeamQuad {

    private final Animation<TextureRegion> animation;
    private final Color tint;
    private final Vector2 nearScratch = new Vector2();
    private final Vector2 farScratch = new Vector2();

    public BeamQuad(Animation<TextureRegion> animation, Color tint) {
        this.animation = animation;
        this.tint = tint;
    }

    /** Random ground point along the beam span — where emitters spawn. */
    public static Anchor alongSpan(VisualState vs) {
        return out -> out.set(MathUtils.random(Battlefield.floorX(vs.nearCol), Battlefield.floorX(vs.farCol)),
                              0f, Battlefield.floorZ(vs.row));
    }

    /** Emitter drive: full while firing, dying with the fade. */
    public static Drive intensity(VisualState vs) {
        return () -> switch (vs.phase) {
            case ACTIVE   -> 1f;
            case RECOVERY -> 1f - vs.phaseProgress;
            default       -> 0f;
        };
    }

    public void draw(RenderContext rc, VisualState vs) {
        if (vs.nearCol < 0 || vs.nearCol >= Battlefield.COLS) return;

        float scale = rc.tileDepthScale(vs.row);
        float depth = rc.tileDepthScale(vs.row);
        float panelW = rc.panelWidth() * scale * depth;
        float panelH = rc.panelRenderHeight() * scale * depth;
        Vector2 nearPos = rc.tileWorld(vs.nearCol, vs.row, nearScratch);
        Vector2 farPos = rc.tileWorld(vs.farCol, vs.row, farScratch);
        float leftX = Math.min(nearPos.x, farPos.x) - panelW * 0.5f;
        float rightX = Math.max(nearPos.x, farPos.x) + panelW * 0.5f;
        float y = nearPos.y - panelH * 0.5f;
        float fullW = rightX - leftX;

        float w, alpha;
        switch(vs.phase) {
            case WINDUP -> {w = fullW * vs.phaseProgress;
                                alpha = 0.5f + 0.5f * vs.phaseProgress;}
            case ACTIVE -> { w = fullW; alpha = 1f; }
            case RECOVERY -> {
                w = fullW * (1f - vs.phaseProgress);
                alpha = 1f  - vs.phaseProgress * vs.phaseProgress;
            }
            default -> {return;}
        }

        // Anchored at the caster so it grows outward — except in RECOVERY,
        // where the far end stays pinned and the tail recedes.
        float drawX;
        if (vs.phase == Phase.RECOVERY) drawX = vs.dir > 0 ? rightX - w : leftX;
        else                            drawX = vs.dir > 0 ? leftX : rightX - w;

        // Play mode is the visual's call: one-shot sheets (beam → dissipate)
        // freeze on their last frame; looping sheets set LOOP themselves.
        TextureRegion frame = animation.getKeyFrame(vs.elapsed);
        rc.batch.setColor(tint.r, tint.g, tint.b, tint.a * alpha);
        if (vs.dir < 0) rc.batch.draw(frame, drawX + w, y, -w, panelH);
        else rc.batch.draw(frame, drawX, y, w, panelH);
        rc.batch.setColor(Color.WHITE);
    }
}
