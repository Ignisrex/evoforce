package com.silverignis.input;

import java.util.*;

public class InputManager {
    private final Map<InputDevice, EnumSet<GameAction>> deviceHeld = new EnumMap<>(InputDevice.class);
    private final EnumSet<GameAction> logicalHeld = EnumSet.noneOf(GameAction.class);
    private final ArrayList<InputEvent> pending = new ArrayList<>();
    private Set<GameAction> heldAtLastFrame = Set.of();

    public InputManager() {
        for (InputDevice device : InputDevice.values()) {
            deviceHeld.put(device, EnumSet.noneOf(GameAction.class));
        }
    }

    public void set(InputDevice device, GameAction action, boolean pressed) {
        EnumSet<GameAction> held = deviceHeld.get(device);
        boolean changed = pressed ? held.add(action) : held.remove(action);
        if(!changed) return;

        boolean nowHeld = false;
        for(EnumSet<GameAction> s : deviceHeld.values()) nowHeld = nowHeld | s.contains(action);

        if (nowHeld && logicalHeld.add(action)) {
            pending.add(new InputEvent(action, InputEvent.Type.PRESSED));
        } else if (!nowHeld && logicalHeld.remove(action)) {
            pending.add(new InputEvent(action, InputEvent.Type.RELEASED));
        }
    }

    public InputFrame nextFrame(long tick) {
        InputFrame frame = new InputFrame(tick, pending, heldAtLastFrame, logicalHeld);
        pending.clear();
        heldAtLastFrame = frame.held();
        return frame;
    }

    public boolean isHeld(GameAction action) {
        return logicalHeld.contains(action);
    }

}
