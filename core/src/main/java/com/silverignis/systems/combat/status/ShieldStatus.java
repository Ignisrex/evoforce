package com.silverignis.systems.combat.status;

import com.silverignis.systems.combat.*;
import com.silverignis.systems.combat.event.DamageEvent;

public class ShieldStatus extends Status {

    private TriggerBus.Subscription subscription;

    public ShieldStatus(float duration){
        super(StatusType.SHIELD,duration, 0f);
    }

    @Override
    protected void onApply(Combatant owner, TriggerBus bus) {
        this.subscription = bus.subscribe(Trigger.ON_DAMAGE_TAKEN_PRE, owner, ev -> {
            DamageEvent dmg = (DamageEvent) ev.payload;
            if(dmg.sourceTag == DamageEvent.Source.STATUS || dmg.sourceTag == DamageEvent.Source.PANEL) return;
            dmg.amount = 0;
            this.remaining = 0f;

        });
    }

    @Override
    protected void onExpire(Combatant owner, TriggerBus bus) {
        if( subscription != null){
            subscription.unsubscribe();
            subscription = null;
        }
    }
}
