package com.silverignis.systems.combat.status;

import com.silverignis.entities.Battlefield;
import com.silverignis.particles.Anchor;
import com.silverignis.particles.Channel;
import com.silverignis.particles.Drive;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.particles.Vfx;
import com.silverignis.systems.combat.*;
import com.silverignis.systems.combat.event.DamageEvent;

public class CombustionStatus extends Status {

    public static final int DETONATE_THRESHOLD = 3;
    public static final float STACK_LIFETIME = 4f;
    public static final int EXPLOSION_DMG = 50;

    private int stacks;

    public CombustionStatus(int stacks) {
        super(StatusType.COMBUSTION, STACK_LIFETIME, 0.1f);
        this.stacks = stacks;
    }

    @Override
    public void mergeFrom(Status incoming) {
        stacks += incoming.stacks();
        remaining = STACK_LIFETIME;
    }

    @Override
    public int stacks() {
        return stacks;
    }

    @Override
    protected void onTick(Combatant owner, DamageSystem dmg, TriggerBus bus, Battlefield battlefield, ParticleEngine particles) {
        if (stacks < DETONATE_THRESHOLD) return;
        stacks = 0;
        remaining = 0f;
        int col = owner.getCol(), row = owner.getRow();
        dmg.apply(new DamageEvent(null, owner, EXPLOSION_DMG, DamageEvent.Source.STATUS, null));
        battlefield.setPanel(col, row, Battlefield.PanelType.FIRE);
        Vfx.combustionExplosion().play(particles,
            Anchor.at(Battlefield.floorX(col), 0.3f, Battlefield.floorZ(row)),
            Drive.FULL, Channel.COMBAT);
    }
}
