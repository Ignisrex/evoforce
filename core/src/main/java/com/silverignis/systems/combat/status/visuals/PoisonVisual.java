package com.silverignis.systems.combat.status.visuals;

import com.silverignis.particles.Anchor;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.particles.Vfx;
import com.silverignis.systems.combat.Combatant;

/** Poison: sick green bubbles floating off the body. No sprite tint. */
final class PoisonVisual extends AbstractStatusVisual {

    @Override public void begin(Combatant owner, ParticleEngine particles) {
        play(particles, Vfx.poisonBubbles(), Anchor.follow(owner));
    }
}
