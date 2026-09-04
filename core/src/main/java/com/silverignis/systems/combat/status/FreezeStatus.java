package com.silverignis.systems.combat.status;

import com.silverignis.systems.combat.Status;
import com.silverignis.systems.combat.StatusType;

public class FreezeStatus extends Status {

    public static final int FREEZE_THRESHOLD = 3;
    public static final float STACK_LIFETIME = 4F;
    public static final float FROZEN_TIME = 3f;

    private int stacks;
    private boolean frozen;

    public FreezeStatus(int stacks) {
        super(StatusType.FREEZE, STACK_LIFETIME, 0F);
        this.stacks = stacks;
        checkFrozen();
    }

    public void mergeFrom(Status incoming) {
        if (frozen) return;
        stacks += incoming.stacks();
        remaining = STACK_LIFETIME;
        checkFrozen();
    }

    private void checkFrozen() {
        if (stacks >= FREEZE_THRESHOLD) {
            frozen = true;
            stacks = 0;
            remaining = FROZEN_TIME;
        }
    }

    @Override
    public int stacks() {
        return stacks;
    }

    @Override
    public boolean blocksCasting() {
        return frozen;
    }

    @Override
    public boolean blocksAttack() {
        return frozen;
    }

    @Override
    public boolean blocksMovement() {
        return frozen;
    }
}
