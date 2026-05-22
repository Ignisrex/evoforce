package com.silverignis.systems.combat.status;

import com.silverignis.systems.combat.*;
import com.silverignis.systems.combat.event.HealEvent;

public class RegenStatus extends Status {

    private final int healPerTick;

    public RegenStatus(float duration, int healPerTick) {
        super(StatusType.REGEN, duration, 0.5f);
        this.healPerTick = healPerTick;
    }

    @Override
    protected void onTick(Combatant owner, DamageSystem dmg, TriggerBus bus) {
        dmg.heal(new HealEvent(owner, healPerTick));
    }
}
