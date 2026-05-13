package com.silverignis.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Common contract for short-lived visual effects drawn on the battlefield.
 * {@code PlayState} owns a {@code List<BattleVfx>} and culls entries whose
 * {@link #isAlive()} returns {@code false} each frame.
 */
public interface BattleVfx {

    void update(float delta);

    void render(SpriteBatch batch);

    boolean isAlive();
}
