package com.silverignis.components;

import com.silverignis.entities.Battlefield;

/**
 * Logical-only position component: which tile, and the perspective depth scale
 * for the row. Render position lives on {@code AnimController} now.
 *
 * <p>{@link #setTile(int, int)} is unclamped — the owning entity's
 * {@code GridBounds} is enforced by {@code MovementSystem.tryGridStep}.
 */
public class GridPosition {

    private final Battlefield battlefield;
    private int col;
    private int row;
    private float depthScale = 1f;

    public GridPosition(Battlefield battlefield, int col, int row) {
        this.battlefield = battlefield;
        this.col = col;
        this.row = row;
    }

    public int   getCol()        { return col; }
    public int   getRow()        { return row; }
    public float getDepthScale() { return depthScale; }

    public void setDepthScale(float s) { this.depthScale = s; }

    /** Set grid cell directly; no clamping. Caller enforces movement rules. */
    public void setTile(int newCol, int newRow) {
        this.col = newCol;
        this.row = newRow;
    }

    public float getWorldZ() { return battlefield.floorZ(row); }
}
