package com.silverignis.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.silverignis.render.RenderContext;
import com.silverignis.render.RenderLayer;

/**
 * A one-shot, short-lived visual flourish drawn at the world point where
 * two opposing projectiles cancel each other. Scales up and fades out
 * over {@link #DURATION} seconds, then reports {@code !isAlive()} so the
 * owning screen can cull it.
 */
public class ClashEffect implements BattleVfx {

    /** Total lifetime of the effect, in seconds. */
    private static final float DURATION = 0.25f;
    /** Sprite scale at spawn (relative to the supplied size). */
    private static final float START_SCALE = 0.6f;
    /** Sprite scale at the end of the lifetime. */
    private static final float END_SCALE = 1.6f;

    private final Sprite sprite;
    /** Optional animated frames. When non-null, takes priority over the static texture. */
    private final Animation<TextureRegion> animation;
    private final int col;
    private final int row;
    private float elapsed = 0f;
    private final Color tint;

    /**
     * Positioned by tile, not by screen point: whoever spawns one is ticking,
     * and the tick has no projection. The screen position and size are resolved
     * per frame in {@link #render}, so this stays correct across a resize.
     *
     * @param texture starburst texture (transparent background expected)
     */
    public ClashEffect(Texture texture, Animation<TextureRegion> animation, Color tint,
                       int col, int row) {
        this.sprite = new Sprite(texture);
        this.animation = animation;
        this.tint = tint != null ? tint : Color.WHITE;
        this.col = col;
        this.row = row;
    }

    @Override
    public void update(float delta) {
        elapsed += delta;
    }

    @Override
    public float depth() { return Battlefield.floorZ(row); }

    @Override
    public RenderLayer layer() { return RenderLayer.BILLBOARD; }

    @Override
    public void render(RenderContext rc) {
        float t = MathUtils.clamp(elapsed / DURATION, 0f, 1f);
        float scale = MathUtils.lerp(START_SCALE, END_SCALE, t);

        float depth    = rc.tileDepthScale(row);
        float panelH   = rc.panelRenderHeight() * depth;
        float baseSize = Math.max(rc.panelWidth() * depth, panelH);
        var  tilePos   = rc.tileWorld(col, row);
        float centerX  = tilePos.x;
        float centerY  = tilePos.y + panelH * 0.5f;

        float w = baseSize * scale;
        float h = baseSize * scale;
        float alpha = (1f - t) * tint.a;
        if (animation != null) {
            TextureRegion frame = animation.getKeyFrame(elapsed, false);
            rc.batch.setColor(tint.r, tint.g, tint.b, alpha);
            rc.batch.draw(frame, centerX - w * 0.5f, centerY - h * 0.5f, w, h);
            rc.batch.setColor(1f, 1f, 1f, 1f);
        } else {
            sprite.setBounds(centerX - w * 0.5f, centerY - h * 0.5f, w, h);
            sprite.setColor(tint.r, tint.g, tint.b, alpha);
            sprite.draw(rc.batch);
            sprite.setColor(Color.WHITE);
        }
    }

    @Override
    public boolean isAlive() { return elapsed < DURATION; }
}
