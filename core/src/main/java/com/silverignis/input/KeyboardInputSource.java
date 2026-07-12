package com.silverignis.input;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;

import java.util.EnumMap;
import java.util.Map;

/**
 * {@link InputSource} backed by the keyboard via {@link Gdx#input}.
 *
 * <p>Edge detection ({@link #isActionJustPressed}) is delegated to libGDX,
 * which already tracks just-pressed state per key, so this class only needs
 * to translate {@link GameAction}s into key codes.
 */
public class KeyboardInputSource implements InputSource {

    /** Default bindings. Grouped here so rebinding later is a one-line change. */
    private final Map<GameAction, Integer> bindings = new EnumMap<>(GameAction.class);

    public KeyboardInputSource() {
        bindings.put(GameAction.MOVE_UP,              Keys.W);
        bindings.put(GameAction.MOVE_DOWN,            Keys.S);
        bindings.put(GameAction.MOVE_LEFT,            Keys.A);
        bindings.put(GameAction.MOVE_RIGHT,           Keys.D);

        bindings.put(GameAction.ATTACK_BASIC,         Keys.J);

        bindings.put(GameAction.TRIGGER_LEFT,         Keys.Q);
        bindings.put(GameAction.TRIGGER_RIGHT,        Keys.E);

        // Slot buttons: number row, classic ARPG layout.
        bindings.put(GameAction.SKILL_X,              Keys.NUM_1);
        bindings.put(GameAction.SKILL_Y,              Keys.NUM_2);
        bindings.put(GameAction.SKILL_B,              Keys.NUM_3);

        bindings.put(GameAction.SKILL_SELECT_CONFIRM, Keys.ENTER);
        bindings.put(GameAction.SKILL_SELECT_CANCEL,  Keys.ESCAPE);
        bindings.put(GameAction.SKILL_SELECT_UNDO,    Keys.BACKSPACE);
        bindings.put(GameAction.SKILL_SELECT_TUCK,    Keys.TAB);
    }

    @Override
    public boolean isActionPressed(GameAction action) {
        Integer key = bindings.get(action);
        return key != null && Gdx.input.isKeyPressed(key);
    }

    @Override
    public boolean isActionJustPressed(GameAction action) {
        Integer key = bindings.get(action);
        return key != null && Gdx.input.isKeyJustPressed(key);
    }

    @Override
    public void update() {
        // No-op: libGDX manages per-frame just-pressed state internally.
    }
}
