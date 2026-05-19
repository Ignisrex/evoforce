package com.silverignis.skills.instances;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.silverignis.components.Caster;
import com.silverignis.components.GridPosition;
import com.silverignis.components.Team;
import com.silverignis.entities.Battlefield;
import com.silverignis.entities.Enemy;
import com.silverignis.entities.Player;
import com.silverignis.skills.ProjectileConfig;
import com.silverignis.skills.ProjectileConfig.MovementType;
import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillInstance;
import com.silverignis.systems.BattleContext;

public class ProjectileInstance extends SkillInstance {

    private static final float DEFAULT_SPEED   = 8f;
    private static final float LOB_FLIGHT_TIME = 0.50f;

    private final ProjectileConfig config;
    private final Sprite sprite;
    private final int   row;
    private final int   dir;       // +1 = player (rightward); -1 = enemy (leftward)

    /** Set on the first update tick once {@link BattleContext} is available. */
    private boolean sized = false;

    // Straight movement
    private float posX;
    private float posY;

    // Lob movement
    private float startX, startY;
    private float endX, endY;
    private float arcHeight;
    private float flightElapsed = 0f;
    private boolean landed = false;

    public ProjectileInstance(Skill def, Caster caster, GridPosition pos) {
        super(def, caster, pos);
        this.row = originRow;
        this.dir = caster.getTeam() == Team.PLAYER ? 1 : -1;

        this.config = def.getShapeConfig() instanceof ProjectileConfig
                ? (ProjectileConfig) def.getShapeConfig()
                : ProjectileConfig.straight(DEFAULT_SPEED);

        this.sprite = new Sprite(def.getVfxTexture());
        if (dir < 0) this.sprite.setFlip(true, false);

        if (config.getMovementType() == MovementType.LOB) {
            this.startX    = pos.getVisualX();
            this.startY    = pos.getVisualY();
            this.arcHeight = config.getArcHeight();
        } else {
            this.posX = pos.getVisualX();
            this.posY = pos.getVisualY();
        }
    }

    @Override
    public void update(float delta, BattleContext ctx) {
        if (!sized) {
            float w = ctx.battlefield.getPanelWidth();
            float h = ctx.battlefield.getPanelRenderHeight();
            sprite.setSize(w, h);

            //adjusted to deal with enemy fire projectile
            if (config.getMovementType() == MovementType.STRAIGHT) {
                posX = pos.getVisualX() - w * 0.5f + w * dir;
            }
            sized = true;
        }

        if (config.getMovementType() == MovementType.LOB) {
            updateLob(delta, ctx);
        } else {
            updateStraight(delta, ctx);
        }
    }

    private void updateStraight(float delta, BattleContext ctx) {
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

        checkHitStraight(ctx);
    }

    private void updateLob(float delta, BattleContext ctx) {
        if (landed) return;

        // Lazily resolve end position on first update (needs ctx).
        if (flightElapsed == 0f) {
            int landCol = Math.min(originCol + config.getTargetRange() * dir, Battlefield.COLS - 1);
            landCol = Math.max(landCol, 0);
            Vector2 landPos = ctx.projectedTileWorld(landCol, row);
            endX = landPos.x - sprite.getWidth() * 0.5f;
            endY = landPos.y;
        }

        flightElapsed += delta;
        float t = Math.min(flightElapsed / LOB_FLIGHT_TIME, 1f);

        posX = startX + (endX - startX) * t;
        posY = startY + (endY - startY) * t + arcHeight * 4f * t * (1f - t);

        if (t >= 1f) {
            landed = true;
            posX = endX;
            posY = endY;
            applyLandingDamage(ctx);
            spawnLandingEffect(ctx);
            finish();
        }
    }

    private void checkHitStraight(BattleContext ctx) {
        if (caster.getTeam() == Team.PLAYER) {
            Enemy target = ctx.enemy;
            if (target == null || !target.isAlive()) return;
            if (target.getRow() != row) return;

            float targetX = ctx.projectedTileWorld(target.getCol(), row).x;
            float halfW   = ctx.battlefield.getPanelWidth() * ctx.tileDepthScale(row) * 0.5f;
            float projCenter = posX + sprite.getWidth() * 0.5f;

            if (projCenter >= targetX - halfW && projCenter <= targetX + halfW) {
                applyEffectsTo(target);
                finish();
            }
        } else {
            Player target = ctx.player;
            if (target == null || !target.isAlive()) return;
            if (target.getRow() != row) return;

            float targetX = ctx.projectedTileWorld(target.getCol(), row).x;
            float halfW   = ctx.battlefield.getPanelWidth() * ctx.tileDepthScale(row) * 0.5f;
            float projCenter = posX + sprite.getWidth() * 0.5f;

            if (projCenter >= targetX - halfW && projCenter <= targetX + halfW) {
                applyEffectsTo(target);
                finish();
            }
        }
    }

    private void applyLandingDamage(BattleContext ctx) {
        // LOB lands on a tile; currently only the player fires LOB skills, so target the enemy.
        Enemy target = ctx.enemy;
        if (target == null || !target.isAlive()) return;
        int landCol = originCol + config.getTargetRange() * dir;
        if (target.getCol() != landCol || target.getRow() != row) return;
        applyEffectsTo(target);
    }

    private void spawnLandingEffect(BattleContext ctx) {
        if (config.getMovementType() != MovementType.LOB) return;
        if (ctx.combatSystem == null) return;

        int landCol = originCol + config.getTargetRange() * dir;
        landCol = Math.min(landCol, Battlefield.COLS - 1);
        landCol = Math.max(landCol, 0);

        // Build a zone skill that lingers on the landing tile. This is a
        // runtime-only synthetic — it never enters the library or the staging
        // menu, so it borrows the parent's icon just to satisfy the builder.
        Skill zoneDef = Skill.builder()
                .id(def.getId() + "_cloud")
                .displayName(def.getDisplayName() + " Cloud")
                .description("Lingering toxic cloud.")
                .icon(def.getIcon())
                .shape(Skill.Shape.ZONE)
                .element(def.getElement())
                .effects(def.getEffects())
                .cooldown(0f)
                .vfxTexture(def.getVfxTexture())
                .build();
        ZoneInstance cloud = new ZoneInstance(zoneDef, caster, pos, landCol, row);
        ctx.combatSystem.spawn(cloud);
    }

    @Override
    public void render(SpriteBatch batch, BattleContext ctx) {
        sprite.setPosition(posX, posY);
        sprite.draw(batch);
    }
}
