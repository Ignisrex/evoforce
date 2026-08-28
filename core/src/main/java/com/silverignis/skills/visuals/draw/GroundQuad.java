package com.silverignis.skills.visuals.draw;

import com.silverignis.skills.visuals.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.silverignis.render.RenderContext;

public final class GroundQuad {

    private static final float HOLD_ALPHA = 0.8f;

    private final TextureRegion region;
    private final Animation<TextureRegion> animation;
    private final Color tint;

    public GroundQuad(TextureRegion region, Animation<TextureRegion> animation, Color tint){
        this.region = region;
        this.animation = animation;
        this.tint = tint;
    }

    public void draw(RenderContext rc, VisualState vs) {
        float scale, alpha;
        switch(vs.phase) {
            case WINDUP -> { scale = 1f + 0.3f * (1f - vs.phaseProgress); alpha = vs.phaseProgress; }
            case ACTIVE -> { scale = 1f; alpha = HOLD_ALPHA; }
            case RECOVERY -> { scale = 1f; alpha = HOLD_ALPHA * (1f - vs.phaseProgress); }
            default -> {return;}
        }

        float depth = rc.depthScale(vs.bodyPos.z);
        float w = rc.panelWidth() * depth * scale;
        float h = rc.panelRenderHeight() * depth * scale;
        Vector2 p = rc.project(vs.bodyPos.x, vs.bodyPos.z);

        TextureRegion frame = animation != null ? animation.getKeyFrame(vs.elapsed) : region;
        rc.batch.setColor(tint.r, tint.g, tint.b, tint.a * alpha);
        rc.batch.draw(frame, p.x - w * 0.5f, p.y - h * 0.5f, w, h);
        rc.batch.setColor(Color.WHITE);
    }

}
