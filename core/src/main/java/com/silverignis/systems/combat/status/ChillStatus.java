package com.silverignis.systems.combat.status;

import com.silverignis.systems.combat.Status;
import com.silverignis.systems.combat.StatusType;

public class ChillStatus extends Status {

    private int stacks;

    //TODO:dont like using a max float for duration so i think ill add something where 0 means it always stays
    public ChillStatus(int stacks) {
        super(StatusType.CHILL, Float.MAX_VALUE, 0f);
        this.stacks = stacks;
    }

    @Override
    public void mergeFrom(Status incoming) {
        stacks += incoming.stacks();
    }

    @Override
    public int stacks() {
        return stacks;
    }

    @Override
    public boolean isExpired() { return stacks <= 0; }

    public void consume(int n) { stacks = Math.max(0, stacks - n); }
}
