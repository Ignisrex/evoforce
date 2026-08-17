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

    // Visual position is in *grid* coordinates, not screen coordinates: the
    // tween runs from one tile to another and rendering projects the result.
    // Keeping it logical means a resize or camera change mid-step stays correct,
    // and nothing has to push a projected target in every frame.
    private float visualCol, visualRow;
    private float moveFromCol, moveFromRow, moveToCol, moveToRow;

    public AnimController(AnimSet animSet, int startCol, int startRow) {
        this.animSet = animSet;
        this.visualCol = this.moveFromCol = this.moveToCol = startCol;
        this.visualRow = this.moveFromRow = this.moveToRow = startRow;
    }

    //State entry api
    public void enterIdle() { setState(AnimState.IDLE);}
    public void enterAttack() { setState(AnimState.ATTACK);}
    public void enterCast() { setState(AnimState.CAST);}
    public void enterHurt() { setState(AnimState.HURT);}
    public void enterDeath() { setState(AnimState.DEATH);}
    /** Tween from the tile the entity was on to the one it just stepped to. */
    public void enterMove(int fromCol, int fromRow, int toCol, int toRow) {
        if (!canTransitionTo(AnimState.MOVE)) return;
        beginState(AnimState.MOVE);
        moveFromCol = fromCol;
        moveFromRow = fromRow;
        moveToCol = toCol;
        moveToRow = toRow;
    }

    /** Jump straight to a tile with no tween — teleports and spawns. */
    public void snapTo(int col, int row) {
        visualCol = moveFromCol = moveToCol = col;
        visualRow = moveFromRow = moveToRow = row;
    }

    private void setState(AnimState s){
        if (!canTransitionTo(s)) return;
        if (current == AnimState.MOVE && s != AnimState.MOVE) {
            visualCol = moveToCol;
            visualRow = moveToRow;
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
            visualCol = moveFromCol + (moveToCol - moveFromCol) * t;
            visualRow = moveFromRow + (moveToRow - moveFromRow) * t;
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

    /** Continuous grid position — integral when standing, fractional mid-step.
     *  Rendering projects this; nothing here knows where it lands on screen. */
    public float getVisualCol() { return visualCol; }
    public float getVisualRow() { return visualRow; }

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

    public float moveProgress() {
        if (current != AnimState.MOVE) return 1f;
        return Math.min(stateTime / MOVE_DURATION, 1f);
    }
}
