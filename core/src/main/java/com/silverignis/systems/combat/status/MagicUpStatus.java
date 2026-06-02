package com.silverignis.systems.combat.status;

import com.silverignis.systems.combat.Status;
import com.silverignis.systems.combat.StatusType;

public class MagicUpStatus extends Status {
    public MagicUpStatus(float duration) {
        super(StatusType.MAGIC_UP, duration, 0f);
    }
}
