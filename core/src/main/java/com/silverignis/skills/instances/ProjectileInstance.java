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

    private final int   row;       // base of the covered span
    private final int   rowSpan;
    private final int   dir;       // +1 = player (rightward); -1 = enemy (leftward)
    private final float colsPerSecond;

    /** Continuous column position of the projectile's center. */
    private float colPos;

    public ProjectileInstance(Skill def, Combatant combatant) {
        super(def, combatant);
        this.dir = visualState.dir;

        ProjectileConfig config = def.getShapeConfig() instanceof ProjectileConfig
                ? (ProjectileConfig) def.getShapeConfig()
                : ProjectileConfig.straight(DEFAULT_SPEED);
        this.colsPerSecond = config.getSpeed() / LEGACY_PANEL_WIDTH;
        // A wide projectile keeps its whole span on the grid: cast from the
        // last row it covers upward from there instead.
        this.rowSpan = config.getRowSpan();
        this.row = Math.min(originRow, Battlefield.ROWS - rowSpan);

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

        // A wide projectile detonates on the first column with any opposing
        // body in its span, hitting everyone it covers there.
        boolean hit = false;
        for (int r = row; r < row + rowSpan; r++) {
            Combatant target = ctx.battleState.combatantAt(col, r);
            if (target == null || target.getTeam() == combatant.getTeam()) continue;
            if (!hit) fireImpact(visualState.bodyPos.x, visualState.bodyPos.y, visualState.bodyPos.z, ctx);
            hit = true;
            applyEffectsTo(target, ctx);
        }
        if (hit) finish();
    }

    private void writeBodyPos() {
        float z = (Battlefield.floorZ(row) + Battlefield.floorZ(row + rowSpan - 1)) * 0.5f;
        visualState.bodyPos.set(Battlefield.floorX(colPos), FLIGHT_HEIGHT, z);
    }


    /** The tile the projectile currently occupies. */
    private int currentCol() { return Math.round(colPos); }

    @Override
    public void coveredTiles(TileSink sink) {
        int col = currentCol();
        if (col < 0 || col >= Battlefield.COLS) return;
        for (int r = row; r < row + rowSpan; r++) sink.tile(col, r);
    }

    // ponytail: clash matching compares this single row, so a wide projectile
    // only clashes against projectiles in its base row.
    public int   getRow()    { return row; }
    public float getColPos() { return colPos; }
}
