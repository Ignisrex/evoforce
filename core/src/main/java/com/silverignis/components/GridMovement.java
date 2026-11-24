package com.silverignis.components;

/**
 * An entity's body on the grid: which tile it holds, where it may legally go,
 * and how it reads visually while stepping between tiles.
 *
 * The visual position is **derived, never stored** — it is a pure function of
 * the previous point, the authoritative tile, and how long ago the step began.
 * There is consequently no second position that can fall out of sync with the
 * first. An earlier design stored the visual position on AnimController and
 * kept it in step by convention; a pose transition refused by animation
 * priority silently dropped a position change and stranded the body.
 *
 * This is also the single writer: {@link #stepTo} and {@link #teleportTo}
 * update tile and tween together, so they cannot come apart.
 */
public class GridMovement {

    /** Tile-to-tile glide time for a voluntary step. */
    public static final float STEP_DURATION = 0.15f;

    private static final float SETTLED = STEP_DURATION * 2f;

    private final GridPosition position;
    private final GridBounds bounds;

    /** Where the body was when the current step began; float so that
     *  interrupting a glide resumes from the exact point reached. */
    private float prevCol, prevRow;
    private float stepElapsed = SETTLED;   // starts settled

    public GridMovement(GridPosition position, GridBounds bounds){
        this.position = position;
        this.bounds = bounds;
        this.prevCol = position.getCol();
        this.prevRow = position.getRow();
    }

    public GridPosition getPosition() { return position; }
    public GridBounds getBounds() { return bounds; }

    /** Voluntary step — glides from wherever the body currently reads. */
    public void stepTo(int col, int row) {
        prevCol = visualCol();
        prevRow = visualRow();
        position.setTile(col, row);
        stepElapsed = 0f;
    }

    /** Forced move (skill dash, displacement) — lands immediately, no glide. */
    public void teleportTo(int col, int row) {
        position.setTile(col, row);
        prevCol = col;
        prevRow = row;
        stepElapsed = SETTLED;
    }

    public void update(float delta) {
        stepElapsed = Math.min(stepElapsed + delta, SETTLED);
    }

    /** 0 at the start of a step, 1 once settled. Drives move i-frames. */
    public float stepProgress() { return progressAt(0f); }

    public float progressAt(float secondsAgo) {
        return Math.clamp(stepElapsed - secondsAgo, 0f, STEP_DURATION) / STEP_DURATION;
    }

    public float visualCol() { return visualCol(0f); }
    public float visualRow() { return visualRow(0f); }

    /** Where the body read {@code secondsAgo} before the last tick — for drawing between ticks. */
    public float visualCol(float secondsAgo) { return prevCol + (position.getCol() - prevCol) * progressAt(secondsAgo); }
    public float visualRow(float secondsAgo) { return prevRow + (position.getRow() - prevRow) * progressAt(secondsAgo); }
}
