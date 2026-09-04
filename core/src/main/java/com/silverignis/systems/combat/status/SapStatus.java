package com.silverignis.systems.combat.status;

import com.silverignis.components.ManaPool;
import com.silverignis.entities.Battlefield;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.systems.combat.*;
import com.silverignis.systems.combat.event.HealEvent;

/** Nature leech: each tick feeds the caster's HP; drains the target's mana
 *  when it has a pool (today only the player does — null is fine). */
public class SapStatus extends Status {

    public static final int HEAL_PER_TICK = 15;

    private final Combatant healTarget;
    private final ManaPool targetPool; // nullable — mana-less victims still feed the heal

    public SapStatus(Combatant healTarget, ManaPool targetPool) {
        super(StatusType.SAP, 3f, 1f);
        this.healTarget = healTarget;
        this.targetPool = targetPool;
    }

    @Override
    protected void onTick(Combatant owner, DamageSystem dmg, TriggerBus bus, Battlefield battlefield, ParticleEngine particles) {
        if (healTarget.isAlive()) dmg.heal(new HealEvent(healTarget, HEAL_PER_TICK));
        if (targetPool != null) targetPool.trySpend(1);
    }
}
