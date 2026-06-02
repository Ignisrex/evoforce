package com.silverignis.systems.combat;

import com.silverignis.systems.combat.status.*;

public class StatusFactory {

    private StatusFactory() {}

    public static Status create(StatusType type, float duration, int dotMag){
        return switch (type) {
            case FREEZE -> new FreezeStatus(duration);
            case BURN -> new BurnStatus(duration, dotMag);
            case POISON -> new PoisonStatus(duration, dotMag);
            case STUN -> new StunStatus(duration);
            case SHIELD -> new ShieldStatus(duration);
            case REGEN -> new RegenStatus(duration, dotMag);
            case POWER_UP -> new PowerUpStatus(duration);
            case MAGIC_UP -> new MagicUpStatus(duration);
        };
    }
}
