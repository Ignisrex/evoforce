package com.silverignis.input;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.silverignis.input.GameAction.*;
import static com.silverignis.input.InputDevice.GAMEPAD;
import static com.silverignis.input.InputDevice.KEYBOARD;
import static com.silverignis.input.InputEvent.Type.PRESSED;
import static com.silverignis.input.InputEvent.Type.RELEASED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputManagerTest {

    private final InputManager input = new InputManager();

    private static InputEvent pressed(GameAction a)  { return new InputEvent(a, PRESSED); }
    private static InputEvent released(GameAction a) { return new InputEvent(a, RELEASED); }

    @Test
    void pressThenReleaseAcrossFrames() {
        input.set(KEYBOARD, SKILL_X, true);
        InputFrame first = input.nextFrame(7);

        assertEquals(7, first.tick());
        assertEquals(List.of(pressed(SKILL_X)), first.events());
        assertEquals(Set.of(), first.heldBefore());
        assertEquals(Set.of(SKILL_X), first.held());
        assertTrue(first.pressed(SKILL_X));

        InputFrame empty = input.nextFrame(8);
        assertTrue(empty.events().isEmpty());
        assertEquals(Set.of(SKILL_X), empty.heldBefore());
        assertTrue(empty.isHeld(SKILL_X));
        assertFalse(empty.pressed(SKILL_X));

        input.set(KEYBOARD, SKILL_X, false);
        InputFrame last = input.nextFrame(9);
        assertEquals(List.of(released(SKILL_X)), last.events());
        assertEquals(Set.of(SKILL_X), last.heldBefore());
        assertEquals(Set.of(), last.held());
    }

    @Test
    void repeatedSetIsIgnored() {
        input.set(KEYBOARD, MOVE_UP, true);
        input.set(KEYBOARD, MOVE_UP, true);
        input.set(KEYBOARD, MOVE_DOWN, false);

        assertEquals(List.of(pressed(MOVE_UP)), input.nextFrame(0).events());
    }

    @Test
    void tapInsideOneFrameIsKept() {
        input.set(GAMEPAD, ATTACK_BASIC, true);
        input.set(GAMEPAD, ATTACK_BASIC, false);
        InputFrame frame = input.nextFrame(0);

        assertEquals(List.of(pressed(ATTACK_BASIC), released(ATTACK_BASIC)), frame.events());
        assertTrue(frame.pressed(ATTACK_BASIC));
        assertFalse(frame.isHeld(ATTACK_BASIC));
    }

    @Test
    void arrivalOrderIsPreserved() {
        input.set(KEYBOARD, SKILL_Y, true);
        input.set(GAMEPAD, TRIGGER_RIGHT, true);
        input.set(KEYBOARD, SKILL_X, true);

        assertEquals(List.of(pressed(SKILL_Y), pressed(TRIGGER_RIGHT), pressed(SKILL_X)),
                     input.nextFrame(0).events());
    }

    @Test
    void actionHeldOnTwoDevicesReleasesOnlyWhenBothLetGo() {
        input.set(KEYBOARD, MOVE_UP, true);
        input.set(GAMEPAD, MOVE_UP, true);
        input.set(KEYBOARD, MOVE_UP, false);
        InputFrame frame = input.nextFrame(0);

        assertEquals(List.of(pressed(MOVE_UP)), frame.events());
        assertTrue(frame.isHeld(MOVE_UP));
        assertTrue(input.isHeld(MOVE_UP));

        input.set(GAMEPAD, MOVE_UP, false);
        assertEquals(List.of(released(MOVE_UP)), input.nextFrame(1).events());
        assertFalse(input.isHeld(MOVE_UP));
    }

    @Test
    void frameDoesNotChangeAfterItIsTaken() {
        input.set(KEYBOARD, SKILL_B, true);
        InputFrame frame = input.nextFrame(0);
        input.set(KEYBOARD, SKILL_B, false);
        input.set(KEYBOARD, SKILL_X, true);

        assertEquals(List.of(pressed(SKILL_B)), frame.events());
        assertEquals(Set.of(SKILL_B), frame.held());
    }
}
