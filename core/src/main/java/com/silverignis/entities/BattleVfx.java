package com.silverignis.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.silverignis.render.SceneRenderable;

/**
 * Common contract for short-lived visual effects drawn on the battlefield.
 * {@code PlayState} owns a {@code List<BattleVfx>} and culls entries whose
 * {@link #isAlive()} returns {@code false} each frame.
 */
public interface BattleVfx extends SceneRenderable {

    void update(float delta);

    boolean isAlive();
}
