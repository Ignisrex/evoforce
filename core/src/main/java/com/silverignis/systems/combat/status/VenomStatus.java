package com.silverignis.systems.combat.status;

import com.silverignis.entities.Battlefield;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.systems.combat.*;
import com.silverignis.systems.combat.event.DamageEvent;

public class VenomStatus extends Status {

    public static final int DMG_PER_CAST = 6;

    private TriggerBus.Subscription subscription;
    private int pendingCasts;

    public VenomStatus(float duration) {
        super(StatusType.VENOM, duration, 0.1f);
    }

    @Override
    protected void onApply(Combatant owner, TriggerBus bus) {
        subscription = bus.subscribe(Trigger.ON_SKILL_RESOLVED, owner, ev -> pendingCasts++);
    }

    @Override
    protected void onTick(Combatant owner, DamageSystem dmg, TriggerBus bus, Battlefield battlefield, ParticleEngine particles) {
        if (pendingCasts == 0) return;
        dmg.apply(new DamageEvent(null, owner, DMG_PER_CAST * pendingCasts, DamageEvent.Source.STATUS, null));
        pendingCasts = 0;
    }

    @Override
    protected void onExpire(Combatant owner, TriggerBus bus) {
        if (subscription != null) subscription.unsubscribe();
    }
}
