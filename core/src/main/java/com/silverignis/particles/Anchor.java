package com.silverignis.particles;

import com.badlogic.gdx.math.Vector3;
import com.silverignis.systems.combat.Combatant;

@FunctionalInterface
public interface Anchor {

    void point(Vector3 out);

    static Anchor at(float x, float y, float z) { return out -> out.set(x, y, x); }
    static Anchor follow(Combatant c) { return c::worldPos; }
}
