package com.silverignis.skills.instances;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.silverignis.components.Team;
import com.silverignis.entities.Battlefield;
import com.silverignis.skills.ProjectileConfig;
import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillInstance;
import com.silverignis.systems.BattleContext;
import com.silverignis.systems.combat.Combatant;

/** Straight-flying projectile: travels along its row until it hits an opposing
 *  combatant or leaves the grid. Lobbed (arcing) projectiles are {@link LobInstance}. */
public class ProjectileInstance extends SkillInstance {

    private static final float DEFAULT_SPEED = 8f;

    private final ProjectileConfig config;
    private final Sprite sprite;
    private final int   row;
    private final int   dir;       // +1 = player (rightward); -1 = enemy (leftward)

    /** Set on the first update tick once {@link BattleContext} is available. */
    private boolean sized = false;

    private float posX;
    private float posY;

    public ProjectileInstance(Skill def, Combatant combatant, BattleContext ctx) {
        super(def, combatant, ctx);
        this.row = originRow;
        this.dir = combatant.getTeam() == Team.PLAYER ? 1 : -1;

        this.config = def.getShapeConfig() instanceof ProjectileConfig
                ? (ProjectileConfig) def.getShapeConfig()
                : ProjectileConfig.straight(DEFAULT_SPEED);

        this.sprite = new Sprite(def.getVfxTexture());
        if (dir < 0) this.sprite.setFlip(true, false);

        Vector2 origin = ctx.projectedTileWorld(originCol, row);
        this.posX = origin.x;
        this.posY = origin.y;
        combatant.getAnimController().enterAttack();
    }

    @Override
    public void update(float delta) {
        BattleContext ctx = battleContext();
        if (!sized) {
            float w = ctx.battlefield.getPanelWidth();
            float h = ctx.battlefield.getPanelRenderHeight();
            sprite.setSize(w, h);

            //adjusted to deal with enemy fire projectile
            posX = ctx.projectedTileWorld(originCol, row).x - w * 0.5f + w * dir;
            sized = true;
            // A projectile's vfx list is its travel trail: the anchor follows the sprite
            // in flight and base onFinish() stops emission at impact/edge.
            playVfx(this::trailPoint);
        }

        posX += config.getSpeed() * dir * delta;

        if (dir > 0) {
            Vector2 lastTile = ctx.projectedTileWorld(Battlefield.COLS - 1, row);
            float gridRight = lastTile.x + ctx.battlefield.getPanelWidth() * ctx.tileDepthScale(row) * 0.5f;
            if (posX > gridRight) { finish(); return; }
        } else {
            Vector2 firstTile = ctx.projectedTileWorld(0, row);
            float gridLeft = firstTile.x - ctx.battlefield.getPanelWidth() * ctx.tileDepthScale(row) * 0.5f;
            if (posX + sprite.getWidth() < gridLeft) { finish(); return; }
        }

        checkHit(ctx);
    }

    private void checkHit(BattleContext ctx) {
        int col = ctx.colAtX(getCenterX(), row);
        if (col<0) return;

        Combatant target = ctx.combatantAt(col, row);
        if (target==null || target.getTeam() == combatant.getTeam()) return;
        applyEffectsTo(target);
        finish();
    }

    /** The sprite's current center converted to grid-world space — the anchor for the travel
     *  trail. Uses SceneCamera's unproject helpers (the inverse of the billboard convention). */
    private void trailPoint(Vector3 out) {
        BattleContext ctx = battleContext();
        float z  = ctx.battlefield.floorZ(row);
        float cx = posX + sprite.getWidth()  * 0.5f;
        float cy = posY + sprite.getHeight() * 0.5f;
        float wx = ctx.environment.unprojectX(cx, z);
        out.set(wx, ctx.environment.unprojectHeight(cy, wx, z), z);
    }

    @Override
    public void render(SpriteBatch batch, BattleContext ctx) {
        sprite.setPosition(posX, posY);
        sprite.draw(batch);
    }

    public int getRow()         { return row; }
    public float getCenterX()   { return posX + sprite.getWidth() * 0.5f; }
}
