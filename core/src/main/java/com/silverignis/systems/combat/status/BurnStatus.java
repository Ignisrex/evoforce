package com.silverignis.systems.combat.status;

import com.silverignis.systems.combat.*;
import com.silverignis.systems.combat.event.DamageEvent;

public class BurnStatus extends Status {

    private final int dmgPerTick;

    public BurnStatus(float duration, int dmgPerTick){
        super(StatusType.BURN, duration, 0.5f);
        this.dmgPerTick = dmgPerTick;
    }

    @Override
    protected void onTick(Combatant owner, DamageSystem dmg, TriggerBus bus) {
        dmg.apply(new DamageEvent(null, owner, dmgPerTick, DamageEvent.Source.STATUS, null));
    }
}
