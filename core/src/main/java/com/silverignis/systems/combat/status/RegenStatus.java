package com.silverignis.systems.combat.status;

import com.silverignis.entities.Battlefield;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.systems.combat.*;
import com.silverignis.systems.combat.event.HealEvent;

public class RegenStatus extends Status {

    private final int healPerTick;

    public RegenStatus(float duration, int healPerTick) {
        super(StatusType.REGEN, duration, 0.5f);
        this.healPerTick = healPerTick;
    }

    @Override
    protected void onTick(Combatant owner, DamageSystem dmg, TriggerBus bus, Battlefield battlefield, ParticleEngine particles) {
        dmg.heal(new HealEvent(owner, healPerTick));
    }
}
