package com.silverignis.animation;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Pure pose state machine: which clip is showing, for how long, and what may
 * interrupt what. It owns no position — where the body *is* belongs to
 * GridMovement, which derives it from the authoritative tile.
 *
 * That separation is deliberate. While this class owned the visual position,
 * the move tween rode the pose clock, so a pose transition refused by priority
 * also refused the position change and stranded the body off its real tile.
 * A pose decision can no longer affect where anything is.
 */
public final class AnimController {

    private static final float HURT_STROBE_INTERVAL = 0.05f;

    private final AnimSet animSet;

    private AnimState current = AnimState.IDLE;
    private AnimState previous = AnimState.IDLE;
    private float stateTime = 0f;
    private boolean locked = false;

    public AnimController(AnimSet animSet) {
        this.animSet = animSet;
    }

    //State entry api
    public void enterIdle()   { setState(AnimState.IDLE); }
    public void enterMove()   { setState(AnimState.MOVE); }
    public void enterAttack() { setState(AnimState.ATTACK); }
    public void enterCast()   { setState(AnimState.CAST); }
    public void enterHurt()   { setState(AnimState.HURT); }
    public void enterDeath()  { setState(AnimState.DEATH); }

    private void setState(AnimState s){
        if (!canTransitionTo(s)) return;
        beginState(s);
    }

    private void beginState(AnimState state) {
        previous = current.loops() ? current : AnimState.IDLE;
        current = state;
        stateTime = 0f;
        locked = !state.loops();
    }

    private boolean canTransitionTo(AnimState target) {
        if (current == AnimState.DEATH) return false;
        if (target.priority() >= current.priority()) return true;
        return !locked;
    }

    //per frame tick
    public void update(float delta) {
        stateTime += delta;

        if (!locked) return;

        // HURT is held longer than its clip so the strobe reads; everything else
        // runs for exactly its clip length.
        float duration = (current == AnimState.HURT) ? 1f : animSet.get(current).duration();
        if (stateTime < duration) return;

        if (current == AnimState.DEATH) {
            locked = false;
            return;
        }

        current = previous.loops() ? previous : AnimState.IDLE;
        stateTime = 0f;
        locked = false;
    }

    //render reads
    public AnimState getState() { return current; }
    public TextureRegion currentFrame() {
        return animSet.get(current).frame(stateTime);
    }

    /** True on alternating intervals during HURT — drives the skip-draw strobe. */
    public boolean isHurtHidden() {
        if (current != AnimState.HURT) return false;
        return ((int) (stateTime / HURT_STROBE_INTERVAL)) % 2 == 1;
    }

    public float getRenderAlpha() {
        if (current != AnimState.DEATH) return 1f;
        float duration = animSet.get(AnimState.DEATH).duration();
        if (duration <= 0f) return 0f;
        return Math.max(0f, 1f - stateTime / duration);
    }

    public boolean isDeathComplete() {
        return current == AnimState.DEATH && getRenderAlpha() <= 0f;
    }
}
