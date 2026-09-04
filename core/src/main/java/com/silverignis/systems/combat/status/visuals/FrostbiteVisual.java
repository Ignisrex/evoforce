package com.silverignis.systems.combat.status.visuals;

import com.badlogic.gdx.graphics.Color;

/** Pale chill cast — weaker than freeze's full ice-blue. */
final class FrostbiteVisual extends AbstractStatusVisual {

    private static final Color PALE = new Color(0.65f, 0.85f, 1f, 1f);

    @Override public void tint(Color inOut) { blend(inOut, PALE, 0.25f); }
}
