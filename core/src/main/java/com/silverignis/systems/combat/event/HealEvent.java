package com.silverignis.systems.combat.event;

import com.silverignis.systems.combat.Combatant;

public final class HealEvent {

    public final Combatant target;
    public  int amount;

    public HealEvent(Combatant target, int amount){
        this.target = target;
        this.amount = amount;
    }
}
