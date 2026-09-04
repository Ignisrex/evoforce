package com.silverignis.systems.combat.status;

import com.silverignis.components.ManaPool;
import com.silverignis.entities.Battlefield;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.systems.combat.*;

public class MetabolizeStatus extends Status {

    public static final int MANA_PER_TICK = 1;

    private final ManaPool mana;
    private int stacks;

    public MetabolizeStatus(int stacks, ManaPool mana) {
        super(StatusType.METABOLIZE, stacks, 1.0f);
        this.stacks = stacks;
        this.mana = mana;
    }

    @Override
    public void mergeFrom(Status incoming) {
        stacks += incoming.stacks();
        remaining = stacks;
    }

    @Override
    public int stacks() { return stacks; }

    @Override
    public boolean isExpired() { return stacks <= 0; }

    @Override
    protected void onTick(Combatant owner, DamageSystem dmg, TriggerBus bus, Battlefield battlefield, ParticleEngine particles) {
        stacks--;
        if (mana != null) mana.recover(MANA_PER_TICK);
    }
}
