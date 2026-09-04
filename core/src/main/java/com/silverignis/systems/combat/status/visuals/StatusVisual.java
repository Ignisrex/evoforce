package com.silverignis.systems.combat.status.visuals;

import com.badlogic.gdx.graphics.Color;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.systems.combat.Combatant;

/** One status's on-combatant look: created when the status appears, ended when
 *  it expires. Render-side only — the sim never sees these. */
public interface StatusVisual {

    void begin(Combatant owner, ParticleEngine particles);

    default void update(float delta, Combatant owner) {}

    /** The status is gone: stop emitters. May linger (isDone false) to fade. */
    void end();

    boolean isDone();

    /** Blend the sprite color toward this status's hue. Constant blend amounts
     *  keep the tint steady frame to frame; preserve alpha. */
    default void tint(Color inOut) {}
}
