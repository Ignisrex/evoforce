package com.silverignis.skills.instances;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.silverignis.entities.Battlefield;
import com.silverignis.entities.Enemy;
import com.silverignis.entities.Player;
import com.silverignis.skills.ProjectileConfig;
import com.silverignis.skills.ProjectileConfig.MovementType;
import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillInstance;
import com.silverignis.systems.BattleContext;

public class ProjectileInstance extends SkillInstance {

    private static final float DEFAULT_SPEED = 8f;
    private static final float LOB_FLIGHT_TIME = 0.50f;

    private final ProjectileConfig config;
    private final Sprite sprite;
    private final int row;

    // Straight movement
    private float posX;
    private float posY;

    // Lob movement
    private float startX, startY;
    private float endX, endY;
    private float arcHeight;
    private float flightElapsed = 0f;
    private boolean landed = false;

    public ProjectileInstance(Skill def, Player caster) {
        super(def, caster);
        this.row = originRow;

        this.config = def.getShapeConfig() instanceof ProjectileConfig
                ? (ProjectileConfig) def.getShapeConfig()
                : ProjectileConfig.straight(DEFAULT_SPEED);

        this.sprite = new Sprite(def.getVfxTexture());
        this.sprite.setSize(
                caster.getSprite().getWidth(),
                caster.getSprite().getHeight());

        if (config.getMovementType() == MovementType.LOB) {
            initLob(caster);
        } else {
            this.posX = caster.getVisualX() + caster.getSprite().getWidth() * 0.5f;
            this.posY = caster.getVisualY();
        }
    }

    private void initLob(Player caster) {
        this.startX = caster.getVisualX() - caster.getSprite().getWidth() * 0.5f;
        this.startY = caster.getVisualY();
        this.arcHeight = config.getArcHeight();
    }

    @Override
    public void update(float delta, BattleContext ctx) {
        if (config.getMovementType() == MovementType.LOB) {
            updateLob(delta, ctx);
        } else {
            updateStraight(delta, ctx);
        }
    }

    private void updateStraight(float delta, BattleContext ctx) {
        posX += config.getSpeed() * delta;

        Vector2 lastTile = ctx.projectedTileWorld(Battlefield.COLS - 1, row);
        float gridRight = lastTile.x + ctx.battlefield.getPanelWidth() * ctx.tileDepthScale(row) * 0.5f;
        if (posX > gridRight) {
            finish();
            return;
        }

        checkHitStraight(ctx);
    }

    private void updateLob(float delta, BattleContext ctx) {
        if (landed) return;

        // Lazily resolve end position on first update (needs ctx).
        if (flightElapsed == 0f) {
            int landCol = Math.min(originCol + config.getTargetRange(), Battlefield.COLS - 1);
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
        Enemy target = ctx.enemy;
        if (target == null || !target.isAlive()) return;
        if (target.getRow() != row) return;

        float targetX = ctx.projectedTileWorld(target.getCol(), row).x;
        float halfW   = ctx.battlefield.getPanelWidth() * ctx.tileDepthScale(row) * 0.5f;
        float projCenter = posX + sprite.getWidth() * 0.5f;

        if (projCenter >= targetX - halfW && projCenter <= targetX + halfW) {
            applyDamage(ctx.enemy);
            finish();
        }
    }

    private void applyLandingDamage(BattleContext ctx) {
        Enemy target = ctx.enemy;
        if (target == null || !target.isAlive()) return;
        int landCol = originCol + config.getTargetRange();
        if (target.getCol() != landCol || target.getRow() != row) return;
        applyDamage(target);
    }

    private void spawnLandingEffect(BattleContext ctx) {
        if (config.getMovementType() != MovementType.LOB) return;
        if (ctx.combatSystem == null) return;

        int landCol = originCol + config.getTargetRange();
        landCol = Math.min(landCol, Battlefield.COLS - 1);

        // Build a zone skill that lingers on the landing tile.
        Skill zoneDef = new Skill(
                def.getId() + "_cloud",
                def.getDisplayName() + " Cloud",
                "Lingering toxic cloud.",
                null,
                Skill.Shape.ZONE,
                def.getElement(),
                def.getEffects(),
                0f,
                def.getVfxTexture()
        );
        ZoneInstance cloud = new ZoneInstance(zoneDef, caster, landCol, row);
        ctx.combatSystem.spawn(cloud);
    }

    private void applyDamage(Enemy target) {
        applyEffectsTo(target);
    }

    @Override
    public void render(SpriteBatch batch, BattleContext ctx) {
        sprite.setPosition(posX, posY);
        sprite.draw(batch);
    }
}
