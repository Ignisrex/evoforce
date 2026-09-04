package com.silverignis.systems.combat.status;

import com.silverignis.entities.Battlefield;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.systems.combat.*;
import com.silverignis.systems.combat.event.DamageEvent;

public class FrostbiteStatus extends Status {

    public static final int DMG_PER_MOVE = 4;

    private TriggerBus.Subscription subscription;
    private int pendingMoves;

    public FrostbiteStatus(float duration) {
        super(StatusType.FROSTBITE, duration, 0.1f);
    }

    @Override
    protected void onApply(Combatant owner, TriggerBus bus) {
        subscription = bus.subscribe(Trigger.ON_MOVE, owner, ev -> pendingMoves++);
    }

    @Override
    protected void onTick(Combatant owner, DamageSystem dmg, TriggerBus bus, Battlefield battlefield, ParticleEngine particles) {
        if (pendingMoves == 0) return;
        dmg.apply(new DamageEvent(null, owner, DMG_PER_MOVE * pendingMoves, DamageEvent.Source.STATUS, null));
        pendingMoves = 0;
    }

    @Override
    protected void onExpire(Combatant owner, TriggerBus bus) {
        if (subscription != null) subscription.unsubscribe();
    }
}
