package com.silverignis.components;

import com.silverignis.entities.Battlefield;
import com.silverignis.util.PositionSmoother;

/**
 * Position component for entities living on the 8×4 panel grid. Owns the
 * logical tile (col, row), the world-space {@link PositionSmoother} that
 * tweens between tile centers, the projected target the smoother chases each
 * frame, and the perspective-depth scale that {@code PlayState} pushes in.
 *
 * <p>Passive data + smoothing. {@link #setTile(int, int)} is unclamped — the
 * owning entity is responsible for whatever movement rules apply (half-grid
 * clamp for the player, etc.).
 */
public class GridPosition {

    private final Battlefield battlefield;
    private int col;
    private int row;
    private final PositionSmoother smoother;

    private float projectedTargetX = Float.NaN;
    private float projectedTargetY = Float.NaN;
    private float depthScale = 1f;

    public GridPosition(Battlefield battlefield, int col, int row, float smoothSpeed) {
        this.battlefield = battlefield;
        this.col = col;
        this.row = row;
        this.smoother = new PositionSmoother(smoothSpeed,
                battlefield.tileCenterX(col), battlefield.tileCenterY(row));
    }

    public int   getCol()        { return col; }
    public int   getRow()        { return row; }
    public float getVisualX()    { return smoother.getX(); }
    public float getVisualY()    { return smoother.getY(); }
    public float getDepthScale() { return depthScale; }

    public void setDepthScale(float s) { this.depthScale = s; }
    public void setProjectedTarget(float x, float y) {
        this.projectedTargetX = x;
        this.projectedTargetY = y;
    }

    /** Set grid cell directly; no clamping. Caller enforces movement rules. */
    public void setTile(int newCol, int newRow) {
        this.col = newCol;
        this.row = newRow;
    }

    /** Smooths toward the projected target (or tile center, if no target yet). */
    public void update(float delta) {
        float tx = Float.isNaN(projectedTargetX) ? battlefield.tileCenterX(col) : projectedTargetX;
        float ty = Float.isNaN(projectedTargetY) ? battlefield.tileCenterY(row) : projectedTargetY;
        smoother.update(delta, tx, ty);
    }

    public float getWorldZ(){
        return this.battlefield.floorZ(row);
    }
}
