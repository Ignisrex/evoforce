package com.silverignis.systems.combat.status.visuals;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.silverignis.systems.combat.Combatant;

/** Slight yellow pulse while stunned.
 *  ponytail: tint only — HURT strobes and auto-exits after ~1s, so it can't
 *  hold a stun; add an AnimState.STUN pose here once stun strips exist. */
final class StunVisual extends AbstractStatusVisual {

    private static final Color YELLOW   = new Color(1f, 0.95f, 0.3f, 1f);
    private static final float PULSE_HZ = 2.5f;

    private float t;

    @Override public void update(float delta, Combatant owner) { t += delta; }

    @Override public void tint(Color inOut) {
        float k = 0.15f + 0.1f * (0.5f + 0.5f * MathUtils.sin(t * PULSE_HZ * MathUtils.PI2));
        blend(inOut, YELLOW, k);
    }
}
