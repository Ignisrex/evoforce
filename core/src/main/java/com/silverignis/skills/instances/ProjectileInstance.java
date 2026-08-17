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

    private final Sprite sprite;
    private final int   row;
    private final int   dir;       // +1 = player (rightward); -1 = enemy (leftward)
    private final float colsPerSecond;

    /** Continuous column position of the projectile's center. */
    private float colPos;

    private boolean trailStarted = false;

    public ProjectileInstance(Skill def, Combatant combatant) {
        super(def, combatant);
        this.row = originRow;
        this.dir = combatant.getTeam() == Team.PLAYER ? 1 : -1;

        ProjectileConfig config = def.getShapeConfig() instanceof ProjectileConfig
                ? (ProjectileConfig) def.getShapeConfig()
                : ProjectileConfig.straight(DEFAULT_SPEED);
        this.colsPerSecond = config.getSpeed() / LEGACY_PANEL_WIDTH;

        this.sprite = new Sprite(def.getVfxTexture());
        if (dir < 0) this.sprite.setFlip(true, false);

        // Spawns one tile ahead of the caster so it clears their own sprite.
        this.colPos = originCol + dir;
        combatant.getAnimController().enterAttack();
    }

    @Override
    public void update(float delta, SkillContext ctx) {
        if (!trailStarted) {
            trailStarted = true;
            // A projectile's vfx list is its travel trail: the anchor follows the
            // projectile in flight and base onFinish() stops emission at impact/edge.
            playVfx(this::trailPoint, ctx);
        }

        colPos += colsPerSecond * dir * delta;

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
        applyEffectsTo(target, ctx);
        finish();
    }

    /** The tile the projectile currently occupies. */
    private int currentCol() { return Math.round(colPos); }

    /** Trail anchor in grid-world space, straight off the logical position —
     *  no unprojection, because the position was never in screen space. */
    private void trailPoint(Vector3 out) {
        out.set(Battlefield.floorX(colPos), FLIGHT_HEIGHT, Battlefield.floorZ(row));
    }

    @Override
    public void coveredTiles(TileSink sink) {
        int col = currentCol();
        if (col >= 0 && col < Battlefield.COLS) sink.tile(col, row);
    }

    @Override
    public void render(RenderContext rc) {
        float w = rc.panelWidth();
        float h = rc.panelRenderHeight();
        sprite.setSize(w, h);

        Vector2 p = rc.tileWorld(colPos, row);
        sprite.setPosition(p.x - w * 0.5f, p.y);
        sprite.draw(rc.batch);
    }

    public int   getRow()    { return row; }
    public float getColPos() { return colPos; }
}
