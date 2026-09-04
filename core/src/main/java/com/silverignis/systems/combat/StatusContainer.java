package com.silverignis.systems.combat;

import com.silverignis.entities.Battlefield;
import com.silverignis.particles.ParticleEngine;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Predicate;

public class StatusContainer {

    private final Combatant owner;
    private final EnumMap<StatusType, Status> byType = new EnumMap<>(StatusType.class);

    public StatusContainer(Combatant owner){
        this.owner = owner;
    }

    public Combatant getOwner(){ return owner; }

    public Status apply(Status s, TriggerBus bus) {
        Status existing = byType.get(s.getType());
        if(existing != null){
            existing.mergeFrom(s);
            return existing;
        }

        byType.put(s.getType(), s);
        s.onApply(owner, bus);
        return s;
    }

    public boolean has(StatusType t){ return byType.containsKey(t) && !byType.get(t).isExpired(); }
    public Status get(StatusType t){ return byType.get(t); }
    public  void remove(StatusType t){ byType.remove(t);}

    public boolean blocksMovement() {return anyAlive(Status::blocksMovement); }
    public boolean blocksAttack() {return  anyAlive(Status::blocksAttack); }
    public boolean blocksCasting() {return anyAlive(Status::blocksCasting); }

    public float recoveryFactor() {
        float f = 1f;
        for (Status s : byType.values()) if (!s.isExpired()) f *= s.recoveryFactor();
        return f;
    }

    private boolean anyAlive(Predicate<Status> p){
        for (Status s: byType.values()){
            if (!s.isExpired() && p.test(s)) return true;
        }
        return false;
    }

    /** First live status that consumes the attempt wins; the step then fails. */
    public boolean consumeMoveAttempt(){
        for (Status s : byType.values()) {
            if (!s.isExpired() && s.consumesMoveAttempt()) return true;
        }
        return false;
    }

    public void update(float delta, DamageSystem dmg, TriggerBus bus, Battlefield battlefield, ParticleEngine particles){
        // Snapshot: an onTick may publish damage that kills the owner; an
        // onExpire may fire triggers that mutate the container indirectly.
        List<Status> snap = new ArrayList<>(byType.values());
        for (Status s: snap) s.tick(delta, owner, dmg,  bus, battlefield, particles);
        byType.values().removeIf(Status::isExpired);
    }
}
