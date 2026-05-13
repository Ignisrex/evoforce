package com.silverignis.input;

/**
 * Device-agnostic actions the game understands. Any concrete input device
 * (keyboard, gamepad, ...) maps its raw buttons/axes onto one of these.
 *
 * <p>The {@code SKILL_X / SKILL_Y / SKILL_B} actions are dual-purpose by
 * design:
 * <ul>
 *   <li>In {@code PlayState} they fire the front skill of the
 *       corresponding slot.</li>
 *   <li>In {@code SkillSelectState} they assign the highlighted hand skill
 *       to the corresponding slot.</li>
 * </ul>
 * Binding once and switching meaning by state keeps the muscle-memory
 * consistent for the player.
 */
public enum GameAction {
    MOVE_UP,
    MOVE_DOWN,
    MOVE_LEFT,
    MOVE_RIGHT,

    ATTACK_BASIC,

    /** Held together: open the skill staging menu. */
    TRIGGER_LEFT,
    TRIGGER_RIGHT,

    /** Slot buttons. Meaning depends on current GameScreenState. */
    SKILL_X,
    SKILL_Y,
    SKILL_B,

    /** Close the staging menu and commit the loaded slots (consumes charge). */
    SKILL_SELECT_CONFIRM,

    /** Close the staging menu without committing (slots revert, charge kept). */
    SKILL_SELECT_CANCEL
}
