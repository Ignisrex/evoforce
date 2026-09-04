package com.silverignis.systems;

import com.badlogic.gdx.utils.ObjectFloatMap;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.silverignis.systems.combat.Combatant;

/** Per-battle scratch state for skill keywords, keyed (combatant, skill, variable).
 *  Owned by the Encounter, so every battle starts clean by construction. */
public final class SkillVars {

    private record Key(Combatant who, String skillId, String var) {}

    private final ObjectIntMap<Key> ints = new ObjectIntMap<>();

    private final ObjectFloatMap<Key> floats = new ObjectFloatMap<>();

    public int inc(Combatant who, String skillId, String  var){
        return ints.getAndIncrement(new Key(who, skillId, var), 0, 1) + 1;
    }

    public int get(Combatant who, String skillId, String var, int defaultValue) {
        return ints.get(new Key(who, skillId, var), defaultValue);
    }

    public void set(Combatant who, String skillId, String var, int value) {
        ints.put(new Key(who, skillId, var), value);
    }

    public void accrue(Combatant who, String skillId, String var, float delta) {
        floats.getAndIncrement(new Key(who, skillId, var), 0f, delta);
    }

    //read and clear
    public float take(Combatant who, String skillId, String var) {
        return floats.remove(new Key(who, skillId, var), 0f);
    }
}
