package com.silverignis.skills;

import com.silverignis.skills.slots.SlotKey;

public record CastInfo(SlotKey fromSlot, int sequenceIndex) {

    public static final CastInfo DIRECT = new CastInfo(null, -1);

    public boolean fromSequence() { return sequenceIndex >= 0; }
}
