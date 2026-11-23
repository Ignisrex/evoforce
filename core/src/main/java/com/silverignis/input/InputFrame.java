package com.silverignis.input;

import java.util.List;
import java.util.Set;

public record InputFrame (long tick, List<InputEvent> events, Set<GameAction> heldBefore, Set<GameAction> held) {

    public InputFrame {
        events = List.copyOf(events);
        heldBefore = Set.copyOf(heldBefore);
        held =  Set.copyOf(held);
    }

    public boolean pressed(GameAction action) {
        for (InputEvent e : events){
            if (e.action() == action && e.type() == InputEvent.Type.PRESSED) return true;
        }
        return false;
    }

    public boolean isHeld(GameAction action) {
        return held.contains(action);
    }
}
