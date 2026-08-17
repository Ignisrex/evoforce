package com.silverignis.skills.instances;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.silverignis.components.Team;
import com.silverignis.entities.Battlefield;
import com.silverignis.render.RenderContext;
import com.silverignis.skills.ProjectileConfig;
import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillContext;
import com.silverignis.skills.SkillInstance;
import com.silverignis.skills.ZoneConfig;
import com.silverignis.systems.combat.Combatant;

/** Arcing (lobbed) projectile: flies a fixed-time parabola to the target tile,
 *  hits whatever stands there, then leaves a lingering {@link ZoneInstance} cloud.
 *  Straight projectiles live in {@link ProjectileInstance}; lobs never clash mid-air.
 *
 *  Travels in column units like {@link ProjectileInstance}; the arc is a world
 *  height above the floor, so both are camera-independent. */
public class LobInstance extends SkillInstance {

    private static final float LOB_FLIGHT_TIME = 0.50f;

    /** Height the ball travels at before the arc is added — it is thrown, not rolled. */
    private static final float BASE_HEIGHT = 0.45f;

    private final ProjectileConfig config;
    private final Sprite sprite;
    private final int   row;
    private final int   dir;       // +1 = player (rightward); -1 = enemy (leftward)
    private final int   landCol;
    private final float arcHeight;

    private float colPos;
    private float flightElapsed = 0f;

    private boolean trailStarted = false;

    public LobInstance(Skill def, Combatant combatant) {
        super(def, combatant);
        this.row = originRow;
        this.dir = combatant.getTeam() == Team.PLAYER ? 1 : -1;
        this.config = (ProjectileConfig) def.getShapeConfig();

        this.sprite = new Sprite(def.getVfxTexture());
        if (dir < 0) this.sprite.setFlip(true, false);

        // Resolved once: the landing tile is what both the damage and the cloud
        // key off, and they disagreed when each clamped it separately.
        this.landCol = MathUtils.clamp(originCol + config.getTargetRange() * dir,
                                       0, Battlefield.COLS - 1);
        this.colPos    = originCol;
        this.arcHeight = config.getArcHeight();
        combatant.getAnimController().enterAttack();
    }

    @Override
    public void update(float delta, SkillContext ctx) {
        if (!trailStarted) {
            trailStarted = true;
            // A projectile's vfx list is its travel trail: the anchor follows the
            // ball in flight and base onFinish() stops emission at impact.
            playVfx(this::trailPoint, ctx);
        }

        flightElapsed += delta;
        float t = progress();
        colPos = originCol + (landCol - originCol) * t;

        if (t >= 1f) {
            applyLandingDamage(ctx);
            spawnLandingEffect(ctx);
            finish();
        }
    }

    /** 0 at launch, 1 at landing. */
    private float progress() { return Math.min(flightElapsed / LOB_FLIGHT_TIME, 1f); }

    /** Parabola peaking mid-flight, in world units above the floor. */
    private float arcOffset(float t) { return arcHeight * 4f * t * (1f - t); }

    private void applyLandingDamage(SkillContext ctx) {
        Combatant target = ctx.battleState.combatantAt(landCol, row);
        if (target == null) return;
        if (target.getTeam() == combatant.getTeam()) return;
        applyEffectsTo(target, ctx);
    }

    private void spawnLandingEffect(SkillContext ctx) {
        if (ctx.combatSystem == null) return;

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
        ZoneInstance cloud = new ZoneInstance(zoneDef, combatant, landCol, row);
        ctx.combatSystem.spawn(cloud);
    }

    /** Trail anchor in grid-world space, straight off the logical position —
     *  no unprojection, because the position was never in screen space. */
    private void trailPoint(Vector3 out) {
        out.set(Battlefield.floorX(colPos), BASE_HEIGHT + arcOffset(progress()), Battlefield.floorZ(row));
    }

    @Override
    public void coveredTiles(TileSink sink) {
        if (flightElapsed <= 0f) return;
        int col = Math.round(colPos);
        if (col >= 0 && col < Battlefield.COLS) sink.tile(col, row);
    }

    @Override
    public void render(RenderContext rc) {
        // 1:1 — panel render height would squash the ball
        float s = rc.panelWidth() * 0.55f;
        sprite.setSize(s, s);

        Vector2 p = rc.tileWorld(colPos, row);
        // Billboard convention: drawn y = floor y + height * depthScale.
        float y = p.y + arcOffset(progress()) * rc.tileDepthScale(row);
        sprite.setPosition(p.x - s * 0.5f, y);
        sprite.draw(rc.batch);
    }
}
