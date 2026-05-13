package com.silverignis.input;

/**
 * A pollable source of {@link GameAction} state.
 *
 * <p>The API deliberately mirrors libGDX's {@code isKeyPressed} /
 * {@code isKeyJustPressed} style so that call sites are familiar.
 *
 * <p>Implementations must call {@link #update()} exactly once per frame,
 * typically via an owning {@link InputManager}, so that
 * {@link #isActionJustPressed(GameAction)} can perform edge detection.
 */
public interface InputSource {

    /** @return true while {@code action} is held this frame. */
    boolean isActionPressed(GameAction action);

    /**
     * @return true on the single frame {@code action} transitions from
     *         released to pressed.
     */
    boolean isActionJustPressed(GameAction action);

    /**
     * Refreshes any internal edge-detection state. Must be called exactly
     * once per frame, before any {@code isAction*} queries.
     */
    void update();
}
