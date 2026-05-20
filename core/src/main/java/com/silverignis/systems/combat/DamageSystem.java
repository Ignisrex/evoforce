package com.silverignis.systems.combat;

import com.silverignis.systems.combat.event.DamageEvent;
import com.silverignis.systems.combat.event.HealEvent;
import com.silverignis.systems.combat.event.TriggerEvent;

/**
 * Single entry point for HP mutation. No other code path touches Health
 * directly — Player and Enemy expose getHealth() for reads only.
 *
 * Defense uses percent reduction: mitigated = raw * 100 / (100 + defense),
 * floored at 1. See {@link #applyDefense} for the contract.
 */

public class DamageSystem {
    private final TriggerBus bus;

    public DamageSystem(TriggerBus bus){
        this.bus = bus;
    }

    public void apply(DamageEvent ev){
        System.out.println("[dmg] target=" + ev.target + " amount=" + ev.amount
            + " alive=" + (ev.target != null && ev.target.isAlive()));
        if(ev.target == null || !ev.target.isAlive())  return;
        if(ev.amount <= 0) return;

        // 1. Defense calc — percent reduction with diminishing returns.
        //    mitigated = max(1, raw * 100 / (100 + defense))
        int defense = ev.target.getStats().getDefense();
        ev.amount = applyDefense(ev.amount, defense);

        // 2. Pre-damage hook. Shields/parries can zero ev.amount here.
        bus.fire(new TriggerEvent(Trigger.ON_DAMAGE_TAKEN_PRE, ev.target, ev));
        if (ev.amount <= 0) return;

        //3.Mutation
        ev.target.getHealth().damage(ev.amount);
        ev.target.onHitFlash();

        // 4. Trigger fan-out. Order: defenders'reaction first then attacker's
        bus.fire(new TriggerEvent(Trigger.ON_DAMAGE_TAKEN, ev.target, ev));
        if (ev.source != null){
            bus.fire(new TriggerEvent(Trigger.ON_HIT_LAND, ev.source, ev));
        }

        // 5. Death detection
        if (ev.target.getHealth().getCurrent() <= 0){
            bus.fire(new TriggerEvent(Trigger.ON_DEATH, ev.target, ev));
            if(ev.source != null){
                bus.fire(new TriggerEvent(Trigger.ON_KILL, ev.source, ev));
            }
            ev.target.onDeath();
        }

    }

    public void heal(HealEvent ev){
        if (ev.target == null || !ev.target.isAlive()) return;
        if (ev.amount <= 0) return;

        ev.target.getHealth().heal(ev.amount);
        bus.fire(new TriggerEvent(Trigger.ON_HEAL, ev.target, ev));
    }

    private static int applyDefense(int rawDamage, int defense){
        if (defense <= 0) return rawDamage;
        return Math.max(1, (rawDamage * 100) / (100 + defense));
    }
}
