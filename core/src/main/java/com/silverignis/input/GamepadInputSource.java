package com.silverignis.input;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerAdapter;
import com.badlogic.gdx.controllers.ControllerMapping;

public class GamepadInputSource extends ControllerAdapter {

    private static final float STICK_DEADZONE = 0.5f;
    private static final float TRIGGER_DEADZONE = 0.5f;

    /**
     * SDL_GameController axis codes for the analog triggers. gdx-controllers
     * doesn't expose named fields for these on {@link ControllerMapping}, so
     * we hardcode the standardized indices here. Triggers report 0..1.
     */
    private static final int AXIS_LEFT_TRIGGER  = 4;
    private static final int AXIS_RIGHT_TRIGGER = 5;

    private boolean dpadUp, dpadDown, dpadLeft, dpadRight, attackA;
    private boolean stickUp, stickDown, stickLeft, stickRight;
    private boolean triggerLeft, triggerRight;
    private boolean confirmA, cancelBack;
    /**
     * Slot buttons. Dual-purpose by GameScreenState: in PlayState they fire
     * the front of the matching slot; in SkillSelectState they assign the
     * highlighted hand skill into the matching slot. Mirrors the
     * keyboard NUM_1/2/3 bindings.
     */
    private boolean slotX, slotY, slotB;
    private boolean bumperL, bumperR;

    private final InputManager manager;

    public GamepadInputSource(InputManager manager) {
        this.manager = manager;
    }

    private void publish() {
        manager.set(InputDevice.GAMEPAD, GameAction.MOVE_UP,              dpadUp    || stickUp);
        manager.set(InputDevice.GAMEPAD, GameAction.MOVE_DOWN,            dpadDown  || stickDown);
        manager.set(InputDevice.GAMEPAD, GameAction.MOVE_LEFT,            dpadLeft  || stickLeft);
        manager.set(InputDevice.GAMEPAD, GameAction.MOVE_RIGHT,           dpadRight || stickRight);
        manager.set(InputDevice.GAMEPAD, GameAction.ATTACK_BASIC,         attackA);
        manager.set(InputDevice.GAMEPAD, GameAction.TRIGGER_LEFT,         triggerLeft);
        manager.set(InputDevice.GAMEPAD, GameAction.TRIGGER_RIGHT,        triggerRight);
        manager.set(InputDevice.GAMEPAD, GameAction.SKILL_SELECT_CONFIRM, confirmA);
        manager.set(InputDevice.GAMEPAD, GameAction.SKILL_SELECT_CANCEL,  cancelBack);
        manager.set(InputDevice.GAMEPAD, GameAction.SKILL_SELECT_UNDO,    bumperL);
        manager.set(InputDevice.GAMEPAD, GameAction.SKILL_SELECT_TUCK,    bumperR);
        manager.set(InputDevice.GAMEPAD, GameAction.SKILL_X,              slotX);
        manager.set(InputDevice.GAMEPAD, GameAction.SKILL_Y,              slotY);
        manager.set(InputDevice.GAMEPAD, GameAction.SKILL_B,              slotB);
    }

    @Override
    public boolean buttonDown(Controller controller, int buttonCode) {
        setButton(controller.getMapping(), buttonCode, true);
        publish();
        return false;
    }

    @Override
    public boolean buttonUp(Controller controller, int buttonCode) {
        setButton(controller.getMapping(), buttonCode, false);
        publish();
        return false;
    }

    @Override
    public boolean axisMoved(Controller controller, int axisCode, float value) {
        ControllerMapping map = controller.getMapping();
        if (axisCode == map.axisLeftX) {
            stickLeft  = value < -STICK_DEADZONE;
            stickRight = value >  STICK_DEADZONE;
        } else if (axisCode == map.axisLeftY) {
            stickUp    = value < -STICK_DEADZONE;
            stickDown  = value >  STICK_DEADZONE;
        } else if (axisCode == AXIS_LEFT_TRIGGER) {
            // Triggers arrive as an analog axis on most pads; threshold to a boolean.
            triggerLeft = value > TRIGGER_DEADZONE;
        } else if (axisCode == AXIS_RIGHT_TRIGGER) {
            triggerRight = value > TRIGGER_DEADZONE;
        }

        publish();
        return false;
    }

    @Override
    public void disconnected(Controller controller) {
        dpadUp = dpadDown = dpadLeft = dpadRight = attackA = false;
        stickUp = stickDown = stickLeft = stickRight = false;
        triggerLeft = triggerRight = false;
        confirmA = cancelBack = false;
        slotX = slotY = slotB = false;
        bumperL = bumperR = false;

        publish();
    }

    private void setButton(ControllerMapping map, int buttonCode, boolean pressed) {
        if (buttonCode == map.buttonDpadUp)         dpadUp       = pressed;
        else if (buttonCode == map.buttonDpadDown)  dpadDown     = pressed;
        else if (buttonCode == map.buttonDpadLeft)  dpadLeft     = pressed;
        else if (buttonCode == map.buttonDpadRight) dpadRight    = pressed;
        else if (buttonCode == map.buttonA) {
            // A is shared: attacks while playing, confirms in skill select.
            attackA  = pressed;
            confirmA = pressed;
        }
        // X / Y / B drive the SKILL_X / SKILL_Y / SKILL_B slot actions.
        // Cancel is moved off B onto the Back/Select button so that pressing
        // B inside the staging menu only assigns to slot B (no double-fire
        // with cancel).
        else if (buttonCode == map.buttonX)         slotX        = pressed;
        else if (buttonCode == map.buttonY)         slotY        = pressed;
        else if (buttonCode == map.buttonB)         slotB        = pressed;
        else if (buttonCode == map.buttonBack)      cancelBack   = pressed;
        else if (buttonCode == map.buttonL1)        bumperL      = pressed;
        else if (buttonCode == map.buttonR1)        bumperR      = pressed;
        else if (buttonCode == map.buttonL2)        triggerLeft  = pressed;
        else if (buttonCode == map.buttonR2)        triggerRight = pressed;
    }
}
