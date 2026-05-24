package com.silverignis.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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
    private final float baseSize;
    private final float centerX;
    private final float centerY;
    private float elapsed = 0f;
    private final float worldZ;

    /**
     * @param texture starburst texture (transparent background expected)
     * @param centerX world-space X of the clash midpoint
     * @param centerY world-space Y of the clash midpoint
     * @param size    target sprite size at scale 1.0 (typically one panel cell)
     */
    public ClashEffect(Texture texture, float centerX, float centerY, float size, float worldZ) {
        this.sprite = new Sprite(texture);
        this.baseSize = size;
        this.centerX = centerX;
        this.centerY = centerY;
        this.worldZ = worldZ;
    }

    @Override
    public void update(float delta) {
        elapsed += delta;
    }

    @Override
    public float depth() { return worldZ; }

    @Override
    public RenderLayer layer() { return RenderLayer.BILLBOARD; }

    @Override
    public void render(RenderContext rc) {
        float t = MathUtils.clamp(elapsed / DURATION, 0f, 1f);
        float scale = MathUtils.lerp(START_SCALE, END_SCALE, t);
        float w = baseSize * scale;
        float h = baseSize * scale;
        sprite.setBounds(centerX - w * 0.5f, centerY - h * 0.5f, w, h);
        sprite.setColor(1f, 1f, 1f, 1f - t);
        sprite.draw(rc.batch);
        sprite.setColor(Color.WHITE);
    }

    @Override
    public boolean isAlive() { return elapsed < DURATION; }
}
