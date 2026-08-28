package com.silverignis.skills.instances;

import com.silverignis.components.Direction;
import com.silverignis.components.Team;
import com.silverignis.entities.Battlefield;
import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillContext;
import com.silverignis.skills.SkillInstance;
import com.silverignis.skills.ZoneConfig;
import com.silverignis.skills.visuals.Phase;
import com.silverignis.systems.combat.Combatant;

public class ZoneInstance extends SkillInstance {

    private static final float APPEAR_TIME       = 0.15f;
    private static final float DEFAULT_ACTIVE    = 3.00f;
    private static final float FADE_TIME         = 0.25f;
    private static final float DEFAULT_TICK      = 0.33f;

    private float phaseTime = 0f;
    private float tickTimer = 0f;

    private final float activeTime;
    private final float tickInterval;
    private final boolean pull;

    private final int targetCol;
    private final int targetRow;

    public ZoneInstance(Skill def, Combatant combatant) {
        this(def, combatant,
             combatant.getCol() + (combatant.getTeam() == Team.PLAYER ? 1 : -1),
             combatant.getRow());
    }

    public ZoneInstance(Skill def, Combatant combatant, int targetCol, int targetRow) {
        super(def, combatant);
        this.targetCol = targetCol;
        this.targetRow = targetRow;

        ZoneConfig cfg = def.getShapeConfig() instanceof ZoneConfig ? (ZoneConfig) def.getShapeConfig() : null;
        this.activeTime   = cfg != null ? cfg.duration     : DEFAULT_ACTIVE;
        this.tickInterval = cfg != null ? cfg.tickInterval : DEFAULT_TICK;
        this.pull         = cfg != null && cfg.pull;
        visualState.bodyPos.set(Battlefield.floorX(targetCol), 0f, Battlefield.floorZ(targetRow));
    }

    @Override
    public void update(float delta, SkillContext ctx) {
        if (visualState.phase == null) setPhase(Phase.WINDUP, ctx);
        phaseTime += delta;

        switch(visualState.phase) {
            case WINDUP -> {
                visualState.phaseProgress = Math.min(phaseTime / APPEAR_TIME, 1f);
                if (phaseTime >= APPEAR_TIME) { phaseTime = 0f; setPhase(Phase.ACTIVE, ctx); }
            }
            case ACTIVE -> {
                visualState.phaseProgress = Math.min(phaseTime/ activeTime, 1f);
                tickTimer += delta;
                if (tickTimer >= tickInterval) { tickTimer -= tickInterval; applyTick(ctx); }
                if (phaseTime >= activeTime) { phaseTime = 0f; setPhase(Phase.RECOVERY, ctx); }
            }
            case RECOVERY -> {
                visualState.phaseProgress = Math.min(phaseTime/FADE_TIME, 1f);
                if (phaseTime >= FADE_TIME) finish();
            }
        }
    }

    private void applyTick(SkillContext ctx) {
        if (pull) {
            applyPull(ctx);
            return;
        }
        Combatant target = ctx.battleState.combatantAt(targetCol, targetRow);
        if (target == null) return;
        if (target.getTeam() == combatant.getTeam()) return;
        applyEffectsTo(target, ctx);
    }

    /**
     * Drag the nearest opposing combatant on each of the four cardinal rays one
     * tile toward the zone tile, and sap it. Diagonally-positioned combatants
     * share neither the zone's row nor column, so they are never affected.
     */
    private void applyPull(SkillContext ctx) {
        // A foe standing on the zone tile itself still gets sapped.
        Combatant onTile = ctx.battleState.combatantAt(targetCol, targetRow);
        if (onTile != null && onTile.getTeam() != combatant.getTeam()) {
            applyEffectsTo(onTile, ctx);
        }

        for (Direction dir : Direction.values()) {
            for (int dist = 1; ; dist++) {
                int col = targetCol + dir.dCol * dist;
                int row = targetRow + dir.dRow * dist;
                if (col < 0 || col >= Battlefield.COLS || row < 0 || row >= Battlefield.ROWS) break;

                Combatant c = ctx.battleState.combatantAt(col, row);
                if (c == null) continue;
                if (c.getTeam() == combatant.getTeam()) break; // friendly blocks the ray

                Direction inward = opposite(dir);
                int nc = col + inward.dCol;
                int nr = row + inward.dRow;
                // applyDisplacement bypasses the tryGridStep occupancy guard, so
                // only pull when the inward tile is free to avoid stacking.
                if (!ctx.battleState.tilesOccupied(nc, nr)) {
                    ctx.movementSystem.applyDisplacement(c, 1, inward);
                }
                applyEffectsTo(c, ctx);
                break; // only the nearest foe per ray
            }
        }
    }

    private static Direction opposite(Direction dir) {
        switch (dir) {
            case UP:    return Direction.DOWN;
            case DOWN:  return Direction.UP;
            case LEFT:  return Direction.RIGHT;
            case RIGHT: return Direction.LEFT;
            default:    return dir;
        }
    }

    @Override
    public void coveredTiles(TileSink sink) {
        if (visualState.phase == Phase.WINDUP) sink.tile(targetCol, targetRow);
    }

}
