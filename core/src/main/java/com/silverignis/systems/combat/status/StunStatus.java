package com.silverignis.systems.combat.status;

import com.silverignis.systems.combat.Status;
import com.silverignis.systems.combat.StatusType;

public class StunStatus extends Status {

    public StunStatus(float duration){
        super(StatusType.STUN, duration, 0f);
    }

    @Override
    public boolean blocksMovement() {
        return true;
    }

    @Override
    public boolean blocksAttack() {
        return true;
    }

    /** A stun stops everything. Stated explicitly now that casting is gated on
     *  its own predicate — before, blocksMovement was doing this job by accident. */
    @Override
    public boolean blocksCasting() {
        return true;
    }
}
