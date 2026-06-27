package com.silverignis.animation;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

public final class AnimController {

    private static final float MOVE_DURATION = 0.15f;
    private static final float HURT_STROBE_INTERVAL = 0.05f;

    private final AnimSet animSet;

    private AnimState current = AnimState.IDLE;
    private AnimState previous = AnimState.IDLE;
    private float stateTime = 0f;
    private boolean locked = false;

    private float renderX, renderY;
    private float moveFromX, moveFromY, moveToX, moveToY;

    public AnimController(AnimSet animSet, float startX, float startY) {
        this.animSet = animSet;
        this.renderX = startX;
        this.renderY = startY;
        this.moveFromX = this.moveToX = startX;
        this.moveFromY = this.moveToY = startY;
    }

    //State entry api
    public void enterIdle() { setState(AnimState.IDLE);}
    public void enterAttack() { setState(AnimState.ATTACK);}
    public void enterCast() { setState(AnimState.CAST);}
    public void enterHurt() { setState(AnimState.HURT);}
    public void enterDeath() { setState(AnimState.DEATH);}
    public void enterMove(float fromX, float fromY, float toX, float toY) {
        if (!canTransitionTo(AnimState.MOVE)) return;
        beginState(AnimState.MOVE);
        moveFromX = fromX;
        moveFromY = fromY;
        moveToX = toX;
        moveToY = toY;
    }

    public void snapTo(float x, float y){
        renderX = moveFromX = moveToX = x;
        renderY = moveFromY = moveToY = y;
    }

    private void setState(AnimState s){
        if (!canTransitionTo(s)) return;
        if (current == AnimState.MOVE && s != AnimState.MOVE) {
            renderX = moveToX;
            renderY = moveToY;
        }
        beginState(s);
    }

    private void beginState(AnimState state) {
        previous = current.loops() ? current : AnimState.IDLE;
        current = state;
        stateTime = 0f;
        locked = !state.loops() || state == AnimState.MOVE;
    }

    private boolean canTransitionTo(AnimState target) {
        if (current == AnimState.DEATH) return false;
        if (target.priority() >= current.priority()) return true;
        return !locked;
    }

    //per frame tick
    public void update(float delta) {
        stateTime += delta;

        if (current == AnimState.MOVE) {
            float t = Math.min(stateTime / MOVE_DURATION, 1f);
            renderX = moveFromX + (moveToX - moveFromX) * t;
            renderY = moveFromY + (moveToY - moveFromY) * t;
        }

        if (!locked) return;

        float duration = (current == AnimState.MOVE) ? MOVE_DURATION : animSet.get(current).duration();

        //override duration for hit state
        duration = (current == AnimState.HURT ) ? 1 : animSet.get(current).duration();
        if (stateTime < duration) return;

        if(current == AnimState.DEATH) {
            locked = false;
            return;
        }

        AnimState ret = previous.loops() ? previous : AnimState.IDLE;
        current = ret;
        stateTime = 0f;
        locked = false;
    }

    //render reads
    public AnimState getState() { return current; }
    public TextureRegion currentFrame() {
        return animSet.get(current).frame(stateTime);
    }

    public float getRenderX() { return renderX; }
    public float getRenderY() { return renderY; }

    /** True on alternating intervals during HURT — drives the skip-draw strobe. */
    public boolean isHurtHidden() {
        if (current != AnimState.HURT) return false;
        return ((int) (stateTime/ HURT_STROBE_INTERVAL)) % 2 == 1;
    }

    public float getRenderAlpha() {
        if (current != AnimState.DEATH) return 1f;
        float duration = animSet.get(AnimState.DEATH).duration();
        if( duration <= 0f) return 0f;
        return Math.max(0f, 1f - stateTime/duration);

    }

    public boolean isDeathComplete() {
        return current == AnimState.DEATH && getRenderAlpha()<= 0f;
    }
}
