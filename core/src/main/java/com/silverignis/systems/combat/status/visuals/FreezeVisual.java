package com.silverignis.systems.combat.status.visuals;

import com.badlogic.gdx.graphics.Color;

/** Icy tint while frozen (was a hardcoded branch in Enemy.render). */
final class FreezeVisual extends AbstractStatusVisual {

    private static final Color ICE = new Color(0.5f, 0.7f, 1f, 1f);

    @Override public void tint(Color inOut) { blend(inOut, ICE, 1f); }
}
