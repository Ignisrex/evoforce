package com.silverignis.systems.combat.status;

import com.silverignis.entities.Battlefield;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.systems.combat.*;
import com.silverignis.systems.combat.event.DamageEvent;

public class PoisonStatus extends Status {

    private static final int DMG_PER_STACK = 3;

    private int stacks;

    public PoisonStatus(int stacks) {
        super(StatusType.POISON, stacks, 1.0f);
        this.stacks = stacks;
    }

    @Override
    public void mergeFrom(Status incoming) {
        stacks += incoming.stacks();
        remaining = stacks;
    }

    @Override
    public int stacks() {
        return stacks;
    }

    @Override
    public boolean isExpired() {
        return stacks <= 0;
    }

    @Override
    protected void onTick(Combatant owner, DamageSystem dmg, TriggerBus bus, Battlefield battlefield, ParticleEngine particles) {
        dmg.apply(new DamageEvent(null, owner, DMG_PER_STACK * stacks, DamageEvent.Source.STATUS, null));
        stacks--;
    }
}
