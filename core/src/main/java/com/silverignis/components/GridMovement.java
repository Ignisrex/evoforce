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

    private final GridPosition position;
    private final GridBounds bounds;

    /** Where the body was when the current step began; float so that
     *  interrupting a glide resumes from the exact point reached. */
    private float prevCol, prevRow;
    private float stepElapsed = STEP_DURATION;   // starts settled

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
        stepElapsed = STEP_DURATION;
    }

    public void update(float delta) {
        stepElapsed = Math.min(stepElapsed + delta, STEP_DURATION);
    }

    /** 0 at the start of a step, 1 once settled. Drives move i-frames. */
    public float stepProgress() { return stepElapsed / STEP_DURATION; }

    // Interpolating toward position.getCol() rather than a stored copy is what
    // makes drift impossible: the target is read live from the authority, so the
    // body always converges on the real tile within STEP_DURATION.
    public float visualCol() { return prevCol + (position.getCol() - prevCol) * stepProgress(); }
    public float visualRow() { return prevRow + (position.getRow() - prevRow) * stepProgress(); }
}
