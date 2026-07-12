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
import com.silverignis.skills.ZoneConfig;
import com.silverignis.systems.BattleContext;
import com.silverignis.systems.combat.Combatant;

/** Arcing (lobbed) projectile: flies a fixed-time parabola to the target tile,
 *  hits whatever stands there, then leaves a lingering {@link ZoneInstance} cloud.
 *  Straight projectiles live in {@link ProjectileInstance}; lobs never clash mid-air. */
public class LobInstance extends SkillInstance {

    private static final float LOB_FLIGHT_TIME = 0.50f;

    private final ProjectileConfig config;
    private final Sprite sprite;
    private final int   row;
    private final int   dir;       // +1 = player (rightward); -1 = enemy (leftward)

    /** Set on the first update tick once {@link BattleContext} is available. */
    private boolean sized = false;

    private float posX;
    private float posY;
    private final float startX, startY;
    private float endX, endY;
    private final float arcHeight;
    private float flightElapsed = 0f;
    private boolean landed = false;

    public LobInstance(Skill def, Combatant combatant, BattleContext ctx) {
        super(def, combatant, ctx);
        this.row = originRow;
        this.dir = combatant.getTeam() == Team.PLAYER ? 1 : -1;
        this.config = (ProjectileConfig) def.getShapeConfig();

        this.sprite = new Sprite(def.getVfxTexture());
        if (dir < 0) this.sprite.setFlip(true, false);

        this.startX    = combatant.getVisualX();
        this.startY    = combatant.getVisualY();
        this.arcHeight = config.getArcHeight();
        combatant.getAnimController().enterAttack();
    }

    @Override
    public void update(float delta) {
        BattleContext ctx = battleContext();
        if (!sized) {
            // 1:1 — panel render height would squash the ball
            float s = ctx.battlefield.getPanelWidth() * 0.55f;
            sprite.setSize(s, s);
            sized = true;
            // A projectile's vfx list is its travel trail: the anchor follows the sprite
            // in flight and base onFinish() stops emission at impact.
            playVfx(this::trailPoint);
        }

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

    private void applyLandingDamage(BattleContext ctx) {
        int landCol = originCol + config.getTargetRange() * dir;
        Combatant target = ctx.combatantAt(landCol, row);

        if (target == null) return;
        if (target.getTeam() == combatant.getTeam()) return;
        applyEffectsTo(target);
    }

    private void spawnLandingEffect(BattleContext ctx) {
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
                .description("Lingering cloud.")
                .icon(def.getIcon())
                .shape(Skill.Shape.ZONE)
                .element(def.getElement())
                .effects(def.getEffects())
                .cooldown(0f)
                .vfxTexture(def.getZoneTexture() != null ? def.getZoneTexture() : def.getVfxTexture())
                .vfxTint(def.getVfxTint())
                .vfx(def.getVfx())
                .shapeConfig(new ZoneConfig(false, config.getZoneDuration(), config.getZoneTickInterval()))
                .build();
        ZoneInstance cloud = new ZoneInstance(zoneDef, combatant, landCol, row, ctx);
        ctx.combatSystem.spawn(cloud);
    }

    /** The sprite's current center converted to grid-world space — the anchor for the travel
     *  trail. Uses SceneCamera's unproject helpers (the inverse of the billboard convention),
     *  so lob arc height falls out naturally. */
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
}
