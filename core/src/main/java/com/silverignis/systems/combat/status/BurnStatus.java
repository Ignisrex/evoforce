package com.silverignis.systems.combat.status;

import com.silverignis.entities.Battlefield;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.systems.combat.*;
import com.silverignis.systems.combat.event.DamageEvent;

public class BurnStatus extends Status {

    private static final int DMG_PER_TICK = 5;

    public BurnStatus(float duration){
        super(StatusType.BURN, duration, 0.5f);
    }

    public void mergeFrom(Status incoming) {
        remaining += incoming.getRemaining();
    }

    @Override
    protected void onTick(Combatant owner, DamageSystem dmg, TriggerBus bus, Battlefield battlefield, ParticleEngine particles) {
        dmg.apply(new DamageEvent(null, owner, DMG_PER_TICK, DamageEvent.Source.STATUS, null));
    }
}
