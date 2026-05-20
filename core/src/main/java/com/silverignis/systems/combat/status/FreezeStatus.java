package com.silverignis.systems.combat.status;

import com.silverignis.systems.combat.Status;
import com.silverignis.systems.combat.StatusType;

public class FreezeStatus extends Status {

    public FreezeStatus(float duration) {
        super(StatusType.FREEZE, duration, 0F);
    }

    @Override
    public boolean blocksCasting() {
        return true;
    }

    @Override
    public boolean blocksAttack() {
        return true;
    }

    @Override
    public boolean blocksMovement() {
        return true;
    }
}
