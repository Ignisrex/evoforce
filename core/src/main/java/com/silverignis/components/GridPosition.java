package com.silverignis.components;

import com.badlogic.gdx.math.Vector3;
import com.silverignis.entities.Battlefield;

/**
 * Logical-only position component: which tile the entity occupies. Render
 * position lives on {@code AnimController} (also logical), and the perspective
 * depth scale is resolved by the render pass from the row — it is not stored
 * here any more, so nothing has to push it in every frame.
 *
 * <p>{@link #setTile(int, int)} is unclamped — the owning entity's
 * {@code GridBounds} is enforced by {@code MovementSystem.tryGridStep}.
 */
public class GridPosition {

    private int col;
    private int row;

    public GridPosition(int col, int row) {
        this.col = col;
        this.row = row;
    }

    public int getCol() { return col; }
    public int getRow() { return row; }

    /** Set grid cell directly; no clamping. Caller enforces movement rules. */
    public void setTile(int newCol, int newRow) {
        this.col = newCol;
        this.row = newRow;
    }

    public float getWorldZ() { return Battlefield.floorZ(row); }
    public float getWorldX() { return Battlefield.floorX(col); }

    /** Tile-snapped ground point (worldX, y=0, worldZ) written into {@code out}. */
    public void worldPos(Vector3 out) { out.set(getWorldX(), 0f, getWorldZ()); }
}
