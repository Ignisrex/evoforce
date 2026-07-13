package com.silverignis.systems;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.silverignis.animation.AnimController;
import com.silverignis.components.*;
import com.silverignis.entities.Battlefield;
import com.silverignis.systems.combat.Combatant;

public final class MovementSystem {

    private BattleContext ctx;

    public void setBattleContext(BattleContext ctx){ this.ctx = ctx; }

    public boolean tryGridStep(Combatant combatant, Direction dir) {
        if (combatant.isInputLocked()) return false;
        if (combatant.getStatusContainer().blocksMovement()) return false;

        GridMovement gridMovement = combatant.getGridMovement();
        GridBounds bounds = gridMovement.getBounds();
        GridPosition pos = gridMovement.getPosition();

        int newCol = MathUtils.clamp(pos.getCol() + dir.dCol, bounds.minCol, bounds.maxCol);
        int newRow = MathUtils.clamp(pos.getRow() + dir.dRow, bounds.minRow, bounds.maxRow);
        if (newCol == pos.getCol() && newRow == pos.getRow()) return false;

        if (this.ctx.tilesOccupied(newCol, newRow)) return false;

        pos.setTile(newCol, newRow);
        AnimController ac = combatant.getAnimController();
        Vector2 to = ctx.projectedTileWorld(newCol, newRow);
        ac.enterMove(ac.getRenderX(), ac.getRenderY(), to.x, to.y);
        return true;
    }

    public void forceGridTeleport(Combatant combatant, int col, int row){
        int c = MathUtils.clamp(col, 0, Battlefield.COLS - 1);
        int r = MathUtils.clamp(row, 0, Battlefield.ROWS - 1);
        combatant.getGridMovement().getPosition().setTile(c, r);
        AnimController ac = combatant.getAnimController();
        Vector2 to = ctx.projectedTileWorld(c,r);
        ac.snapTo(to.x, to.y);
    }

    public void applyDisplacement(Combatant combatant, int tiles, Direction dir) {
        GridPosition pos = combatant.getGridMovement().getPosition();
        int newRow = pos.getRow() + (dir.dRow * tiles);
        int newCol = pos.getCol() + (dir.dCol * tiles);
        forceGridTeleport(combatant, newCol, newRow);
    }

    public void applyFreeInput(FreePosition free, float dx, float dy, float delta) {
        Rectangle b = free.getBounds();
        float nx = MathUtils.clamp(free.getX() + dx * free.getSpeed() * delta, b.x, b.x + b.width);
        float ny = MathUtils.clamp(free.getY() + dy * free.getSpeed() * delta, b.y, b.y + b.height);
        free.set(nx, ny);
    }
}
