package com.silverignis.systems.combat;

import com.silverignis.systems.combat.status.BurnStatus;
import com.silverignis.systems.combat.status.FreezeStatus;
import com.silverignis.systems.combat.status.PoisonStatus;
import com.silverignis.systems.combat.status.StunStatus;

public class StatusFactory {

    private StatusFactory() {}

    public static Status create(StatusType type, float duration, int dotMag){
        return switch (type) {
            case FREEZE -> new FreezeStatus(duration);
            case BURN -> new BurnStatus(duration, dotMag);
            case POISON -> new PoisonStatus(duration, dotMag);
            case STUN -> new StunStatus(duration);
            case REGEN, SHIELD -> throw new IllegalArgumentException("StatusType " + type + " has no concrete implementation yet");
        };
    }
}
