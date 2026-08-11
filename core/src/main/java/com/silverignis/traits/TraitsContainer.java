package com.silverignis.traits;

import com.silverignis.systems.combat.Combatant;
import com.silverignis.systems.combat.DamageSystem;
import com.silverignis.systems.combat.TriggerBus;

import java.util.*;

public class TraitsContainer {

    private final Map<String, Trait> byId = new LinkedHashMap<>();

    public void add(Trait trait) { byId.putIfAbsent(trait.getId(), trait); }

    public boolean has(String id) { return byId.containsKey(id); }

    public boolean isEmpty() { return byId.isEmpty(); }

    public List<Trait> all() { return Collections.unmodifiableList(new ArrayList<>(byId.values())); }

    public int slotCapacityBonus() {
        int bonus = 0;
        for (Trait t : byId.values()) bonus += t.getEffect().slotCapacityBonus();
        return  bonus;
    }

    public void applyBattleHooks(Combatant owner, TriggerBus bus, DamageSystem dmg) {
        for (Trait t : byId.values()) t.getEffect().onBattleStart(owner, bus, dmg);
    }


}
