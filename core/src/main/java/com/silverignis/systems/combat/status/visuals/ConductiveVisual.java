package com.silverignis.systems.combat.status.visuals;

import com.silverignis.particles.Anchor;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.particles.Vfx;
import com.silverignis.systems.combat.Combatant;

/** Static sparks crawling the body while the charge waits for a thunder hit. */
final class ConductiveVisual extends AbstractStatusVisual {

    @Override public void begin(Combatant owner, ParticleEngine particles) {
        play(particles, Vfx.conductiveStatic(), Anchor.follow(owner));
    }
}
