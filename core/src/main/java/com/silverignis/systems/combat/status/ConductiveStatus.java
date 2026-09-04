package com.silverignis.systems.combat.status;

import com.silverignis.entities.Battlefield;
import com.silverignis.particles.Anchor;
import com.silverignis.particles.Channel;
import com.silverignis.particles.Drive;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.particles.Vfx;
import com.silverignis.skills.elements.Element;
import com.silverignis.systems.combat.*;
import com.silverignis.systems.combat.event.DamageEvent;

public class ConductiveStatus extends Status {

    public static final int BONUS_DMG = 15;

    private TriggerBus.Subscription subscription;
    private boolean primed;

    public ConductiveStatus(float duration) {
        super(StatusType.CONDUCTIVE, duration, 0.1f);
    }

    @Override
    protected void onApply(Combatant owner, TriggerBus bus) {
        subscription = bus.subscribe(Trigger.ON_DAMAGE_TAKEN, owner, ev -> {
            if (primed) return;
            if (!(ev.payload instanceof DamageEvent d)) return;
            if (d.sourceTag != DamageEvent.Source.SKILL) return;
            if (d.originalSkill == null || d.originalSkill.getElement() != Element.LIGHTNING) return;
            primed = true;
        });
    }

    @Override
    protected void onTick(Combatant owner, DamageSystem dmg, TriggerBus bus, Battlefield battlefield, ParticleEngine particles) {
        if (!primed) return;
        remaining = 0f;
        int col = owner.getCol(), row = owner.getRow();
        dmg.apply(new DamageEvent(null, owner, BONUS_DMG, DamageEvent.Source.STATUS, null));
        battlefield.setPanel(col, row, Battlefield.PanelType.LIGHTNING);
        Vfx.conductiveZap().play(particles,
            Anchor.at(Battlefield.floorX(col), 0.4f, Battlefield.floorZ(row)),
            Drive.FULL, Channel.COMBAT);
    }

    @Override
    protected void onExpire(Combatant owner, TriggerBus bus) {
        if (subscription != null) subscription.unsubscribe();
    }
}
