package com.silverignis.systems.combat.status;

import com.silverignis.systems.combat.Status;
import com.silverignis.systems.combat.StatusType;

/** Dark rot that slows resource recovery — mana regen on the player,
 *  cooldown recovery on enemies — by 25%. */
public class MiasmaStatus extends Status {

    public static final float RECOVERY_FACTOR = 0.75f;

    public MiasmaStatus(float duration) {
        super(StatusType.MIASMA, duration, 0f);
    }

    @Override
    public float recoveryFactor() { return RECOVERY_FACTOR; }
}
