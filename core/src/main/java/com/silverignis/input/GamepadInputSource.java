package com.silverignis.input;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerAdapter;
import com.badlogic.gdx.controllers.ControllerMapping;
import com.badlogic.gdx.controllers.Controllers;

import java.util.EnumMap;
import java.util.Map;

/** Event-driven {@link InputSource} backed by gdx-controllers. */
public class GamepadInputSource extends ControllerAdapter implements InputSource {

    private static final float STICK_DEADZONE = 0.5f;
    private static final float TRIGGER_DEADZONE = 0.5f;

    /**
     * SDL_GameController axis codes for the analog triggers. gdx-controllers
     * doesn't expose named fields for these on {@link ControllerMapping}, so
     * we hardcode the standardized indices here. Triggers report 0..1.
     */
    private static final int AXIS_LEFT_TRIGGER  = 4;
    private static final int AXIS_RIGHT_TRIGGER = 5;

    private final Map<GameAction, Boolean> currentPressed = new EnumMap<>(GameAction.class);
    private final Map<GameAction, Boolean> previousPressed = new EnumMap<>(GameAction.class);

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

    public GamepadInputSource() {
        for (GameAction action : GameAction.values()) {
            currentPressed.put(action, false);
            previousPressed.put(action, false);
        }
        Controllers.addListener(this);
    }

    @Override
    public void update() {
        previousPressed.putAll(currentPressed);
        currentPressed.put(GameAction.MOVE_UP,              dpadUp    || stickUp);
        currentPressed.put(GameAction.MOVE_DOWN,            dpadDown  || stickDown);
        currentPressed.put(GameAction.MOVE_LEFT,            dpadLeft  || stickLeft);
        currentPressed.put(GameAction.MOVE_RIGHT,           dpadRight || stickRight);
        currentPressed.put(GameAction.ATTACK_BASIC,         attackA);
        currentPressed.put(GameAction.TRIGGER_LEFT,         triggerLeft);
        currentPressed.put(GameAction.TRIGGER_RIGHT,        triggerRight);
        currentPressed.put(GameAction.SKILL_SELECT_CONFIRM, confirmA);
        currentPressed.put(GameAction.SKILL_SELECT_CANCEL,  cancelBack);
        currentPressed.put(GameAction.SKILL_X,              slotX);
        currentPressed.put(GameAction.SKILL_Y,              slotY);
        currentPressed.put(GameAction.SKILL_B,              slotB);
    }

    @Override
    public boolean buttonDown(Controller controller, int buttonCode) {
        setButton(controller.getMapping(), buttonCode, true);
        return false;
    }

    @Override
    public boolean buttonUp(Controller controller, int buttonCode) {
        setButton(controller.getMapping(), buttonCode, false);
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
        return false;
    }

    @Override
    public void disconnected(Controller controller) {
        dpadUp = dpadDown = dpadLeft = dpadRight = attackA = false;
        stickUp = stickDown = stickLeft = stickRight = false;
        triggerLeft = triggerRight = false;
        confirmA = cancelBack = false;
        slotX = slotY = slotB = false;
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
        else if (buttonCode == map.buttonL2)        triggerLeft  = pressed;
        else if (buttonCode == map.buttonR2)        triggerRight = pressed;
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
}
