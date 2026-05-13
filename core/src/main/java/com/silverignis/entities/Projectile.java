package com.silverignis.entities;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/**
 * A lightweight world-space projectile. Not tied to the battlefield grid —
 * it just advances by its velocity each frame until it leaves the viewport,
 * at which point it is flagged dead and the owning screen removes it.
 */
public class Projectile implements Collider {

    /**
     * Fraction of the sprite's width / height shaved off each side when
     * computing the hitbox. {@code 0.25} keeps the inner 50% of the
     * sprite as the hittable area, which feels tighter than a full-cell
     * hitbox.
     */
    private static final float HITBOX_INSET = 0.25f;

    private final Vector2 position;
    private final Vector2 velocity;
    private final Sprite sprite;
    private final Team team;
    /** Reused per call to avoid per-frame allocations. See {@link Collider#getBounds()}. */
    private final Rectangle bounds = new Rectangle();
    private boolean alive = true;

    public Projectile(Vector2 position, Vector2 velocity, Sprite sprite, Team team){
        this.position = position;
        this.velocity = velocity;
        this.sprite = sprite;
        this.team = team;
        this.sprite.setPosition(position.x, position.y);
    }

    /**
     * Advance the projectile. Velocity is in world-units-per-second, so it
     * is scaled by {@code delta}. Once fully off either edge of the viewport
     * the projectile is marked dead.
     */
    public void update(float delta, float worldWidth){
        position.x += velocity.x * delta;
        position.y += velocity.y * delta;
        sprite.setPosition(position.x, position.y);

        if (position.x > worldWidth || position.x + sprite.getWidth() < 0f) {
            alive = false;
        }
    }

    public void render(SpriteBatch spriteBatch){
        sprite.draw(spriteBatch);
    }

    /** Flag the projectile dead so the owning screen removes it on its next cull pass. */
    public void kill() { alive = false; }

    @Override
    public Rectangle getBounds() {
        float w = sprite.getWidth();
        float h = sprite.getHeight();
        float ix = w * HITBOX_INSET;
        float iy = h * HITBOX_INSET;
        return bounds.set(sprite.getX() + ix, sprite.getY() + iy,
                          w - 2f * ix, h - 2f * iy);
    }

    @Override
    public Team getTeam() { return team; }

    @Override
    public boolean isAlive()     { return alive; }
    public Vector2 getPosition() { return position; }
    public Sprite getSprite()    { return sprite; }
}
