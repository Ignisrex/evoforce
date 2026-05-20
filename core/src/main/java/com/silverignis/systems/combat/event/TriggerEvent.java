package com.silverignis.systems.combat.event;

import com.silverignis.systems.combat.Combatant;
import com.silverignis.systems.combat.Trigger;
import com.silverignis.systems.combat.TriggerListener;

/**
 * Payload passed to {@link TriggerListener#onTrigger}.
 *
 * {@code combatant} is the *subject* of the trigger:
 *   - ON_HIT_LAND / ON_KILL → the attacker
 *   - ON_DAMAGE_TAKEN(_PRE) / ON_HEAL / ON_DEATH → the target
 *   - ON_TICK → the owner of the status or aura that ticked
 *
 * {@code payload} is the contextual event:
 *   - DamageEvent for ON_HIT_LAND, ON_DAMAGE_TAKEN_PRE, ON_DAMAGE_TAKEN, ON_DEATH, ON_KILL
 *   - HealEvent for ON_HEAL
 *   - null for ON_TICK
 *
 * Listeners cast {@code payload} to the type they expect for the trigger they
 * subscribed to. This is intentionally untyped (Object) to keep the bus small —
 * the trigger-to-payload mapping is part of the contract, not the type system.
 */

public final class TriggerEvent {

    public final Trigger trigger;
    public final Combatant combatant;
    public final Object payload;

    public TriggerEvent(Trigger trigger, Combatant combatant, Object payload){
        this.trigger = trigger;
        this.combatant = combatant;
        this.payload = payload;
    }
}
