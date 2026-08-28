package com.silverignis.skills.instances;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.silverignis.components.Team;
import com.silverignis.entities.Battlefield;
import com.silverignis.render.RenderContext;
import com.silverignis.skills.ProjectileConfig;
import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillContext;
import com.silverignis.skills.SkillInstance;
import com.silverignis.skills.visuals.Phase;
import com.silverignis.systems.combat.Combatant;

/** Straight-flying projectile: travels along its row until it hits an opposing
 *  combatant or leaves the grid. Lobbed (arcing) projectiles are {@link LobInstance}.
 *
 *  Position is {@link #colPos} — a continuous column coordinate, not a screen
 *  coordinate. Nothing here reads the camera or the viewport, so where the
 *  projectile *is* stays independent of how it is drawn. */
public class ProjectileInstance extends SkillInstance {

    private static final float DEFAULT_SPEED = 8f;

    /** Skill JSON carries speed in world units/sec against the legacy 1.25-wide
     *  panel. Converting once here keeps those authored values meaningful. */
    private static final float LEGACY_PANEL_WIDTH = 1.25f;

    /** Height above the floor the travel trail is emitted at — projectiles fly
     *  at sprite height, not on the ground. */
    private static final float FLIGHT_HEIGHT = 0.45f;

    private final int   row;
    private final int   dir;       // +1 = player (rightward); -1 = enemy (leftward)
    private final float colsPerSecond;

    /** Continuous column position of the projectile's center. */
    private float colPos;

    public ProjectileInstance(Skill def, Combatant combatant) {
        super(def, combatant);
        this.row = originRow;
        this.dir = visualState.dir;

        ProjectileConfig config = def.getShapeConfig() instanceof ProjectileConfig
                ? (ProjectileConfig) def.getShapeConfig()
                : ProjectileConfig.straight(DEFAULT_SPEED);
        this.colsPerSecond = config.getSpeed() / LEGACY_PANEL_WIDTH;

        // Spawns one tile ahead of the caster so it clears their own sprite.
        this.colPos = originCol + dir;
        writeBodyPos();
    }

    @Override
    public void update(float delta, SkillContext ctx) {
        if (visualState.phase == null) setPhase(Phase.ACTIVE, ctx);

        colPos += colsPerSecond * dir * delta;
        writeBodyPos();

        // Half a tile past either end of the grid — symmetric in both directions.
        if (colPos > Battlefield.COLS - 0.5f || colPos < -0.5f) {
            finish();
            return;
        }

        checkHit(ctx);
    }

    private void checkHit(SkillContext ctx) {
        int col = currentCol();
        if (col < 0 || col >= Battlefield.COLS) return;

        Combatant target = ctx.battleState.combatantAt(col, row);
        if (target == null || target.getTeam() == combatant.getTeam()) return;
        fireImpact(visualState.bodyPos.x, visualState.bodyPos.y, visualState.bodyPos.z, ctx);
        applyEffectsTo(target, ctx);
        finish();
    }

    private void writeBodyPos() {
        visualState.bodyPos.set(Battlefield.floorX(colPos), FLIGHT_HEIGHT, Battlefield.floorZ(row));
    }


    /** The tile the projectile currently occupies. */
    private int currentCol() { return Math.round(colPos); }

    @Override
    public void coveredTiles(TileSink sink) {
        int col = currentCol();
        if (col >= 0 && col < Battlefield.COLS) sink.tile(col, row);
    }

    public int   getRow()    { return row; }
    public float getColPos() { return colPos; }
}
