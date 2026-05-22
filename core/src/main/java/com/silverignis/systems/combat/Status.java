package com.silverignis.systems.combat;

public abstract class Status {

    protected final StatusType type;
    protected float remaining; //seconds until expiry
    protected final float tickInterval; // 0 = no periodic tick
    protected float tickAcc;

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

    public void tick(float delta, Combatant owner, DamageSystem dmg, TriggerBus bus){
        remaining -= delta;
        if (tickInterval > 0f) {
            tickAcc += delta;
            while( tickAcc >= tickInterval){
                tickAcc -= tickInterval;
                onTick(owner, dmg, bus);
            }
        }
        if(isExpired()) onExpire(owner, bus);
    }

    /*called when a status is first applied, not when the same type is refreshed*/
    protected void onApply(Combatant owner, TriggerBus bus){}
    protected void onTick(Combatant owner, DamageSystem dmg, TriggerBus bus){}
    protected void onExpire(Combatant owner, TriggerBus bus){};

    public boolean blocksMovement(){ return false; }
    public boolean blocksAttack(){ return false; }
    public boolean blocksCasting(){ return false; }
}
