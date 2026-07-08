package com.silverignis.skills.instances;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.silverignis.components.Direction;
import com.silverignis.components.Team;
import com.silverignis.entities.Battlefield;
import com.silverignis.render.RenderLayer;
import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillInstance;
import com.silverignis.skills.ZoneConfig;
import com.silverignis.systems.BattleContext;
import com.silverignis.systems.combat.Combatant;

public class ZoneInstance extends SkillInstance {

    private static final float APPEAR_TIME       = 0.15f;
    private static final float DEFAULT_ACTIVE    = 1.00f;
    private static final float FADE_TIME         = 0.25f;
    private static final float DEFAULT_TICK      = 0.33f;

    private enum Phase { APPEAR, ACTIVE, FADE, DONE }

    private Phase phase = Phase.APPEAR;
    private float phaseTime = 0f;
    private float tickTimer = 0f;
    private float stateTime = 0f;

    private final float activeTime;
    private final float tickInterval;
    private final boolean pull;

    private final Sprite sprite;
    private final Animation<TextureRegion> animation;
    private final Color tint;
    private final int targetCol;
    private final int targetRow;
    private boolean rendersUnder = true;

    public ZoneInstance(Skill def, Combatant combatant, BattleContext ctx) {
        this(def, combatant,
             combatant.getCol() + (combatant.getTeam() == Team.PLAYER ? 1 : -1),
             combatant.getRow(), ctx);
    }

    public ZoneInstance(Skill def, Combatant combatant, int targetCol, int targetRow, BattleContext ctx) {
        super(def, combatant, ctx);
        this.targetCol = targetCol;
        this.targetRow = targetRow;

        ZoneConfig cfg = def.getShapeConfig() instanceof ZoneConfig ? (ZoneConfig) def.getShapeConfig() : null;
        this.activeTime   = cfg != null ? cfg.duration     : DEFAULT_ACTIVE;
        this.tickInterval = cfg != null ? cfg.tickInterval : DEFAULT_TICK;
        this.pull         = cfg != null && cfg.pull;

        this.sprite    = new Sprite(def.getVfxTexture());
        this.animation = def.getVfxAnimation();
        this.tint      = def.getVfxTint() != null ? def.getVfxTint() : Color.WHITE;
        sprite.setColor(tint);
    }

    public boolean isRenderUnder() { return rendersUnder; }

    @Override
    public void update(float delta) {
        phaseTime += delta;
        stateTime += delta;

        switch (phase) {
            case APPEAR:
                if (phaseTime >= APPEAR_TIME) enterActive();
                break;
            case ACTIVE:
                tickTimer += delta;
                if (tickTimer >= tickInterval) {
                    tickTimer -= tickInterval;
                    applyTick(battleContext());
                }
                if (phaseTime >= activeTime) enterFade();
                break;
            case FADE:
                if (phaseTime >= FADE_TIME) {
                    phase = Phase.DONE;
                    finish();
                }
                break;
            case DONE:
                break;
        }
    }

    private void enterActive() {
        phase = Phase.ACTIVE;
        phaseTime = 0f;
        playVfx(tileAnchor(targetCol, targetRow));   // layered particle effects on the zone tile
    }

    private void enterFade() {
        phase = Phase.FADE;
        phaseTime = 0f;
    }

    private void applyTick(BattleContext ctx) {
        if (pull) {
            applyPull(ctx);
            return;
        }
        Combatant target = ctx.combatantAt(targetCol, targetRow);
        if (target == null) return;
        if (target.getTeam() == combatant.getTeam()) return;
        applyEffectsTo(target);
    }

    /**
     * Drag the nearest opposing combatant on each of the four cardinal rays one
     * tile toward the zone tile, and sap it. Diagonally-positioned combatants
     * share neither the zone's row nor column, so they are never affected.
     */
    private void applyPull(BattleContext ctx) {
        // A foe standing on the zone tile itself still gets sapped.
        Combatant onTile = ctx.combatantAt(targetCol, targetRow);
        if (onTile != null && onTile.getTeam() != combatant.getTeam()) {
            applyEffectsTo(onTile);
        }

        for (Direction dir : Direction.values()) {
            for (int dist = 1; ; dist++) {
                int col = targetCol + dir.dCol * dist;
                int row = targetRow + dir.dRow * dist;
                if (col < 0 || col >= Battlefield.COLS || row < 0 || row >= Battlefield.ROWS) break;

                Combatant c = ctx.combatantAt(col, row);
                if (c == null) continue;
                if (c.getTeam() == combatant.getTeam()) break; // friendly blocks the ray

                Direction inward = opposite(dir);
                int nc = col + inward.dCol;
                int nr = row + inward.dRow;
                // applyDisplacement bypasses the tryGridStep occupancy guard, so
                // only pull when the inward tile is free to avoid stacking.
                if (ctx.combatantAt(nc, nr) == null) {
                    ctx.movementSystem.applyDisplacement(c, 1, inward);
                }
                applyEffectsTo(c);
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
    public void render(SpriteBatch batch, BattleContext ctx) {
        if (phase == Phase.DONE) return;

        float depthScale = ctx.tileDepthScale(targetRow);
        float panelW = ctx.battlefield.getPanelWidth() * depthScale;
        float panelH = ctx.battlefield.getPanelRenderHeight() * depthScale;
        Vector2 tilePos = ctx.projectedTileWorld(targetCol, targetRow);
        float tileX = tilePos.x;
        float tileY = tilePos.y;

        float scale;
        float alpha;

        switch (phase) {
            case APPEAR:
                // Edges expand inward: start oversized and shrink to tile bounds.
                scale = 1f + 0.3f * (1f - phaseTime / APPEAR_TIME);
                alpha = phaseTime / APPEAR_TIME;
                break;
            case ACTIVE:
                scale = 1f;
                alpha = 0.8f;
                break;
            case FADE:
                scale = 1f;
                alpha = 0.8f * (1f - phaseTime / FADE_TIME);
                break;
            default:
                return;
        }

        float w = panelW * scale;
        float h = panelH * scale;
        float cx = tileX;
        float cy = tileY;

        if (animation != null) {
            TextureRegion frame = animation.getKeyFrame(stateTime, true);
            batch.setColor(tint.r, tint.g, tint.b, tint.a * alpha);
            batch.draw(frame, cx - w * 0.5f, cy - h * 0.5f, w, h);
            batch.setColor(1f, 1f, 1f, 1f);
        } else {
            sprite.setBounds(cx - w * 0.5f, cy - h * 0.5f, w, h);
            sprite.setAlpha(alpha);
            sprite.draw(batch);
            sprite.setAlpha(1f);
        }
    }

    public RenderLayer layer() {
        return isRenderUnder() ? RenderLayer.GROUND : RenderLayer.BILLBOARD;
    }
}
