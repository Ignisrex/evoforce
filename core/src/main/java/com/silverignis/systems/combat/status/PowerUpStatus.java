package com.silverignis.systems.combat.status;

import com.silverignis.systems.combat.Status;
import com.silverignis.systems.combat.StatusType;

public class PowerUpStatus extends Status {
    public PowerUpStatus(float duration) {
        super(StatusType.POWER_UP, duration, 0f);
    }
}
