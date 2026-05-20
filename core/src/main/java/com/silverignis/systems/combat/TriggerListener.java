package com.silverignis.systems.combat;

import com.silverignis.systems.combat.event.TriggerEvent;

@FunctionalInterface
public interface TriggerListener {
    void onTrigger(TriggerEvent ev);
}
