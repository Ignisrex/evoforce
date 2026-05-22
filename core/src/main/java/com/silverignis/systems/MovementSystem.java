package com.silverignis.systems;

import com.badlogic.gdx.math.MathUtils;
import com.silverignis.components.Direction;
import com.silverignis.components.GridBounds;
import com.silverignis.components.GridMovement;
import com.silverignis.components.GridPosition;
import com.silverignis.entities.Battlefield;
import com.silverignis.systems.combat.Combatant;

public final class MovementSystem {

    public boolean tryGridStep(Combatant combatant, Direction dir) {
        if (combatant.isInputLocked()) return false;
        if (combatant.getStatusContainer().blocksMovement()) return false;

        GridMovement gridMovement = combatant.getGridMovement();
        GridBounds bounds = gridMovement.getBounds();
        GridPosition pos = gridMovement.getPosition();

        int newCol = MathUtils.clamp(pos.getCol() + dir.dCol, bounds.minCol, bounds.maxCol);
        int newRow = MathUtils.clamp(pos.getRow() + dir.dRow, bounds.minRow, bounds.maxRow);
        if (newCol == pos.getCol() && newRow == pos.getRow()) return false;

        pos.setTile(newCol, newRow);
        return true;
    }

    public void forceGridTeleport(Combatant combatant, int col, int row){
        int c = MathUtils.clamp(col, 0, Battlefield.COLS - 1);
        int r = MathUtils.clamp(row, 0, Battlefield.ROWS - 1);
        combatant.getGridMovement().getPosition().setTile(c, r);
    }

    public void applyDisplacement(Combatant combatant, int tiles, Direction dir) {
        GridPosition pos = combatant.getGridMovement().getPosition();
        int newRow = pos.getRow() + (dir.dRow * tiles);
        int newCol = pos.getCol() + (dir.dCol * tiles);
        forceGridTeleport(combatant, newCol, newRow);
    }
}
