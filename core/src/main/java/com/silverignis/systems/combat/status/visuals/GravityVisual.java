package com.silverignis.systems.combat.status.visuals;

import com.silverignis.particles.Anchor;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.particles.Vfx;
import com.silverignis.systems.combat.Combatant;

/** Violet streaks dragging the lower body toward the floor. */
final class GravityVisual extends AbstractStatusVisual {

    @Override public void begin(Combatant owner, ParticleEngine particles) {
        play(particles, Vfx.gravityStreaks(), Anchor.follow(owner));
    }
}
