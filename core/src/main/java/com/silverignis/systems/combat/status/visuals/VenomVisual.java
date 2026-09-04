package com.silverignis.systems.combat.status.visuals;

import com.badlogic.gdx.graphics.Color;

/** Sickly green cast while venom punishes every skill use. */
final class VenomVisual extends AbstractStatusVisual {

    private static final Color SICK = new Color(0.45f, 0.9f, 0.35f, 1f);

    @Override public void tint(Color inOut) { blend(inOut, SICK, 0.25f); }
}
