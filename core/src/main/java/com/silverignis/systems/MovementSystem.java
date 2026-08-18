package com.silverignis.systems;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.silverignis.components.*;
import com.silverignis.entities.Battlefield;
import com.silverignis.systems.combat.Combatant;

/**
 * Sole owner of entity position writes. Input steps go through
 * {@link #tryGridStep} (input-lock, movement-blocking status, per-entity
 * {@link GridBounds}); skill-driven dashes go through
 * {@link #forceGridTeleport}, which clamps only to the global grid edge so a
 * Strike's HIT phase can drive the caster into enemy territory.
 *
 * Knows nothing about the camera: it hands the animation controller tiles, and
 * the render pass turns those into screen positions.
 */
public final class MovementSystem {

    private final BattleState battleState;

    public MovementSystem(BattleState battleState) {
        this.battleState = battleState;
    }

    public boolean tryGridStep(Combatant combatant, Direction dir) {
        if (combatant.isInputLocked()) return false;
        if (combatant.getStatusContainer().blocksMovement()) return false;

        GridMovement gridMovement = combatant.getGridMovement();
        GridBounds bounds = gridMovement.getBounds();
        GridPosition pos = gridMovement.getPosition();

        int fromCol = pos.getCol();
        int fromRow = pos.getRow();
        int newCol = MathUtils.clamp(fromCol + dir.dCol, bounds.minCol, bounds.maxCol);
        int newRow = MathUtils.clamp(fromRow + dir.dRow, bounds.minRow, bounds.maxRow);
        if (newCol == fromCol && newRow == fromRow) return false;

        if (battleState.tilesOccupied(newCol, newRow)) return false;

        // One call updates tile and tween together; the pose is a separate,
        // refusable concern that can no longer affect where the body is.
        gridMovement.stepTo(newCol, newRow);
        combatant.getAnimController().enterMove();
        return true;
    }

    public void forceGridTeleport(Combatant combatant, int col, int row){
        int c = MathUtils.clamp(col, 0, Battlefield.COLS - 1);
        int r = MathUtils.clamp(row, 0, Battlefield.ROWS - 1);
        combatant.getGridMovement().teleportTo(c, r);
    }

    public void applyDisplacement(Combatant combatant, int tiles, Direction dir) {
        GridPosition pos = combatant.getGridMovement().getPosition();
        int newRow = pos.getRow() + (dir.dRow * tiles);
        int newCol = pos.getCol() + (dir.dCol * tiles);
        forceGridTeleport(combatant, newCol, newRow);
    }

    /** Free (non-grid) movement — the overworld avatar. Static because it touches
     *  no battle state: there is no roster to collide with out there. */
    public static void applyFreeInput(FreePosition free, float dx, float dy, float delta) {
        Rectangle b = free.getBounds();
        float nx = MathUtils.clamp(free.getX() + dx * free.getSpeed() * delta, b.x, b.x + b.width);
        float ny = MathUtils.clamp(free.getY() + dy * free.getSpeed() * delta, b.y, b.y + b.height);
        free.set(nx, ny);
    }
}
