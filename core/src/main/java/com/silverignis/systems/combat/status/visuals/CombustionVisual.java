package com.silverignis.systems.combat.status.visuals;

import com.badlogic.gdx.graphics.Color;
import com.silverignis.particles.Anchor;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.particles.Vfx;
import com.silverignis.systems.combat.Combatant;

/** Volatile ember glow while combustion stacks build; the detonation itself is
 *  a one-shot the status fires. */
final class CombustionVisual extends AbstractStatusVisual {

    private static final Color EMBER = new Color(1f, 0.35f, 0.1f, 1f);

    @Override public void begin(Combatant owner, ParticleEngine particles) {
        play(particles, Vfx.burnSmolder(), Anchor.follow(owner));
    }

    @Override public void tint(Color inOut) { blend(inOut, EMBER, 0.3f); }
}
