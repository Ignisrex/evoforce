package com.silverignis.systems.combat.status;

import com.silverignis.systems.combat.Status;
import com.silverignis.systems.combat.StatusType;

public class GravityStatus extends Status {

    public GravityStatus(float duration) {
        super(StatusType.GRAVITY, duration, 0f);
    }

    @Override
    public boolean consumesMoveAttempt() {
        remaining = 0f;
        return true;
    }
}
