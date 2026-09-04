package com.silverignis.systems.combat;

import com.silverignis.systems.combat.status.*;

public class StatusFactory {

    private StatusFactory() {}

    public static Status create(StatusType type, float duration, int dotMag){
        return switch (type) {
            case FREEZE -> new FreezeStatus(Math.max(1, dotMag));
            case BURN -> new BurnStatus(duration);
            case POISON -> new PoisonStatus(dotMag);
            case STUN -> new StunStatus(duration);
            case SHIELD -> new ShieldStatus(duration);
            case REGEN -> new RegenStatus(duration, dotMag);
            case POWER_UP -> new PowerUpStatus(duration);
            case MAGIC_UP -> new MagicUpStatus(duration);
            case COMBUSTION -> new CombustionStatus(Math.max(1, dotMag));
            case FROSTBITE -> new FrostbiteStatus(duration);
            case VENOM -> new VenomStatus(duration);
            case CONDUCTIVE -> new ConductiveStatus(duration);
            case GRAVITY -> new GravityStatus(duration);
            case METABOLIZE -> new MetabolizeStatus(Math.max(1, dotMag), null);
            case CHILL -> new ChillStatus(Math.max(1, dotMag));
            case MIASMA -> new MiasmaStatus(duration);
            // Not null-injected like METABOLIZE: a null heal target is a latent
            // NPE, not a harmless no-op. Sap comes from skill code only.
            case SAP -> throw new IllegalStateException("SAP is capability-injected — apply it from skill code");
        };
    }
}
