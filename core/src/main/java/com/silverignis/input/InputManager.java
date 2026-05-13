package com.silverignis.input;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Composite {@link InputSource} that fans out to one or more concrete sources
 * (typically keyboard + gamepad) and ORs their pressed state together so the
 * rest of the game can be written against a single device-agnostic API.
 *
 * <p>The composite keeps its own previous-frame snapshot so
 * {@link #isActionJustPressed(GameAction)} correctly reports a single edge
 * even when two sources are active at once (e.g. holding {@code W} on the
 * keyboard while tapping up on the D-pad).
 */
public class InputManager implements InputSource {

    private final List<InputSource> sources;

    private final Map<GameAction, Boolean> currentPressed = new EnumMap<>(GameAction.class);
    private final Map<GameAction, Boolean> previousPressed = new EnumMap<>(GameAction.class);

    /** Convenience: default keyboard + gamepad stack. */
    public static InputManager defaultSetup() {
        return new InputManager(new KeyboardInputSource(), new GamepadInputSource());
    }

    public InputManager(InputSource... sources) {
        this.sources = Arrays.asList(sources);
        for (GameAction action : GameAction.values()) {
            currentPressed.put(action, false);
            previousPressed.put(action, false);
        }
    }

    @Override
    public void update() {
        // Refresh each child first so their per-frame state is current.
        for (InputSource source : sources) {
            source.update();
        }

        // Snapshot previous, then recompute current by OR-ing children.
        previousPressed.putAll(currentPressed);
        for (GameAction action : GameAction.values()) {
            currentPressed.put(action, anySourcePressed(action));
        }
    }

    @Override
    public boolean isActionPressed(GameAction action) {
        return Boolean.TRUE.equals(currentPressed.get(action));
    }

    @Override
    public boolean isActionJustPressed(GameAction action) {
        return Boolean.TRUE.equals(currentPressed.get(action))
            && !Boolean.TRUE.equals(previousPressed.get(action));
    }

    private boolean anySourcePressed(GameAction action) {
        for (InputSource source : sources) {
            if (source.isActionPressed(action)) return true;
        }
        return false;
    }
}
