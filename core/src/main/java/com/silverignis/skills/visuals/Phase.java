package com.silverignis.skills.visuals;

public enum Phase {
    WINDUP(Trigger.WINDUP),
    ACTIVE(Trigger.ACTIVE),
    RECOVERY(Trigger.RECOVERY);

    public final Trigger trigger;

    Phase(Trigger trigger) { this.trigger = trigger; }
}
