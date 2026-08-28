package com.silverignis.skills.visuals;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.silverignis.render.RenderContext;

final class SlashFlash {

    private static final float DURATION = 0.25f;
    private static final float START_SCALE = 0.6f;
    private static final float END_SCALE = 1.6f;

    private final Animation<TextureRegion> animation;
    private final TextureRegion region;
    private final Color tint;
    private final Vector3 at = new Vector3();
    private float elapsed;

    SlashFlash(TextureRegion region, Animation<TextureRegion> animation, Color tint, Vector3 pos){
        this.region = region;
        this.animation = animation;
        this.tint = tint;
        this.at.set(pos);
    }

    void update(float delta) {
        elapsed += delta;
    }

    boolean isDone() {
        return elapsed >= DURATION;
    }

    void render(RenderContext rc) {
        float t = MathUtils.clamp(elapsed/DURATION, 0f, 1f);
        float scale = MathUtils.lerp(START_SCALE, END_SCALE, t);

        float depth = rc.depthScale(at.z);
        float panelH = rc.panelRenderHeight() * depth;
        float size = Math.max(rc.panelWidth() *depth , panelH) * scale;
        Vector2 p = rc.project(at.x, at.z);
        float cy = p.y + panelH * 0.5f;

        TextureRegion frame = animation != null ? animation.getKeyFrame(elapsed) : region;
        rc.batch.setColor(tint.r, tint.g, tint.b, (1f - t) * tint.a);
        rc.batch.draw(frame, p.x - size * 0.5f, cy - size * 0.5f, size, size);
        rc.batch.setColor(Color.WHITE);
    }
}
