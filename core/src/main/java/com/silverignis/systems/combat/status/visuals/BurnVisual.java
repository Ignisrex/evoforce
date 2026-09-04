package com.silverignis.systems.combat.status.visuals;

import com.badlogic.gdx.graphics.Color;
import com.silverignis.particles.Anchor;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.particles.Vfx;
import com.silverignis.systems.combat.Combatant;

/** Flames smoldering on the body while burning, plus a faint warm cast. */
final class BurnVisual extends AbstractStatusVisual {

    private static final Color WARM = new Color(1f, 0.55f, 0.3f, 1f);

    @Override public void begin(Combatant owner, ParticleEngine particles) {
        play(particles, Vfx.burnSmolder(), Anchor.follow(owner));
    }

    @Override public void tint(Color inOut) { blend(inOut, WARM, 0.2f); }
}
