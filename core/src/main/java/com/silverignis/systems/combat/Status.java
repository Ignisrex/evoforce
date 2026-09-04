package com.silverignis.systems.combat;

import com.silverignis.entities.Battlefield;
import com.silverignis.particles.ParticleEngine;

public abstract class Status {

    protected final StatusType type;
    protected float remaining; //seconds until expiry
    protected final float tickInterval; // 0 = no periodic tick
    protected float tickAcc;
    private boolean expiredFired;

    protected Status(StatusType type, float duration, float tickInterval){
        this.type = type;
        this.remaining = duration;
        this.tickInterval = tickInterval;
    }

    public StatusType getType() { return type; }
    public float  getRemaining() { return remaining; }
    public boolean isExpired() { return remaining <= 0f; }

    public void refresh(float duration){
        if (duration > remaining) remaining = duration;
    }

    public void mergeFrom(Status incoming) {
        refresh(incoming.getRemaining());
    }

    public int stacks() { return 0; }

    public void tick(float delta, Combatant owner, DamageSystem dmg, TriggerBus bus, Battlefield battlefield, ParticleEngine particles){
        remaining -= delta;
        if (tickInterval > 0f) {
            tickAcc += delta;
            while( tickAcc >= tickInterval){
                tickAcc -= tickInterval;
                onTick(owner, dmg, bus, battlefield, particles);
            }
        }
        if(isExpired() && !expiredFired) {
            expiredFired = true;
            onExpire(owner, bus);
        }
    }

    /*called when a status is first applied, not when the same type is refreshed*/
    protected void onApply(Combatant owner, TriggerBus bus){}
    protected void onTick(Combatant owner, DamageSystem dmg, TriggerBus bus, Battlefield battlefield, ParticleEngine particles){}
    protected void onExpire(Combatant owner, TriggerBus bus){};

    /** Scales the owner's resource recovery — mana regen for the player,
     *  cooldown recovery for enemies. 1 = unaffected. */
    public float recoveryFactor() { return 1f; }

    public boolean blocksMovement(){ return false; }
    public boolean blocksAttack(){ return false; }
    public boolean blocksCasting(){ return false; }

    /** Consume the owner's voluntary move attempt (the attempt fails). Return
     *  true to consume; the status decides its own cost (e.g. self-expire). */
    public boolean consumesMoveAttempt(){ return false; }
}
