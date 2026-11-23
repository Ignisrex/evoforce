package com.silverignis.input;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;

import java.util.HashMap;
import java.util.Map;

public class KeyboardInputSource extends InputAdapter {

    private final InputManager manager;
    private final Map<Integer, GameAction> bindings = new HashMap<>();

    public KeyboardInputSource(InputManager manager) {
        this.manager = manager;

        bindings.put(Keys.W, GameAction.MOVE_UP);
        bindings.put(Keys.S, GameAction.MOVE_DOWN);
        bindings.put(Keys.A, GameAction.MOVE_LEFT);
        bindings.put(Keys.D, GameAction.MOVE_RIGHT);

        bindings.put(Keys.J, GameAction.ATTACK_BASIC);

        bindings.put(Keys.Q, GameAction.TRIGGER_LEFT);
        bindings.put(Keys.E, GameAction.TRIGGER_RIGHT);

        bindings.put(Keys.NUM_1, GameAction.SKILL_X);
        bindings.put(Keys.NUM_2, GameAction.SKILL_Y);
        bindings.put(Keys.NUM_3, GameAction.SKILL_B);

        bindings.put(Keys.ENTER,     GameAction.SKILL_SELECT_CONFIRM);
        bindings.put(Keys.ESCAPE,    GameAction.SKILL_SELECT_CANCEL);
        bindings.put(Keys.BACKSPACE, GameAction.SKILL_SELECT_UNDO);
        bindings.put(Keys.TAB,       GameAction.SKILL_SELECT_TUCK);
    }

    @Override
    public boolean keyDown(int keycode) {
        return report(keycode, true);
    }

    @Override
    public boolean keyUp(int keycode) {
        return report(keycode, false);
    }

    private boolean report(int keycode, boolean pressed) {
        GameAction action = bindings.get(keycode);
        if (action != null) manager.set(InputDevice.KEYBOARD, action, pressed);
        return false;
    }

}
