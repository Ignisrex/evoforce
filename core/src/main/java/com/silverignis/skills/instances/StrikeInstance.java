package com.silverignis.skills.instances;

import com.badlogic.gdx.math.Vector2;
import com.silverignis.components.Team;
import com.silverignis.entities.ClashEffect;
import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillInstance;
import com.silverignis.skills.StrikeConfig;
import com.silverignis.systems.BattleContext;
import com.silverignis.systems.combat.Combatant;

public class StrikeInstance extends SkillInstance {

    private static final float DASH_FORWARD_TIME = 0.10f;
    private static final float HIT_TIME          = 0.20f;
    private static final float DASH_BACK_TIME    = 0.10f;

    private enum Phase { DASH_FORWARD, HIT, DASH_BACK, DONE }

    private Phase phase = Phase.DASH_FORWARD;
    private float phaseTime = 0f;
    private boolean primed = false;

    private final int dir;
    private final int strikeFromCol;
    private final int firstTargetCol;
    private final int hitTiles;
    private final int row;

    public StrikeInstance(Skill def, Combatant combatant, BattleContext ctx) {
        super(def, combatant, ctx);
        this.dir = combatant.getTeam() == Team.PLAYER ? 1 : -1;

        StrikeConfig cfg = def.getShapeConfig() instanceof StrikeConfig
            ? (StrikeConfig) def.getShapeConfig()
            : new StrikeConfig(1, 1);

        this.strikeFromCol  = originCol + dir * cfg.getDashTiles();
        this.firstTargetCol = strikeFromCol + dir;
        this.hitTiles       = cfg.getHitTiles();
        this.row            = originRow;

        acquireInputLock();
    }

    @Override
    public void update(float delta) {
        if (!primed) {
            battleContext().movementSystem.forceGridTeleport(combatant, strikeFromCol, row);
            primed = true;
        }
        phaseTime += delta;

        switch (phase) {
            case DASH_FORWARD:
                if (phaseTime >= DASH_FORWARD_TIME) {
                    enterHit(battleContext());
                }
                break;

            case HIT:
                if (phaseTime >= HIT_TIME) {
                    enterDashBack(battleContext());
                }
                break;

            case DASH_BACK:
                if (phaseTime >= DASH_BACK_TIME) {
                    phase = Phase.DONE;
                    finish();
                }
                break;

            case DONE:
                break;
        }
    }

    private void enterHit(BattleContext ctx) {
        phase = Phase.HIT;
        phaseTime = 0f;
        combatant.getAnimController().enterAttack();

        for (int i = 0; i < hitTiles; i++) {
            int col = firstTargetCol + dir * i;
            spawnSlashVfx(ctx, col);
            applyHit(ctx, col);
        }
    }

    private void enterDashBack(BattleContext ctx) {
        phase = Phase.DASH_BACK;
        phaseTime = 0f;
        ctx.movementSystem.forceGridTeleport(combatant, originCol, originRow);
    }

    private void spawnSlashVfx(BattleContext ctx, int col) {
        // VFX lands on the target tile regardless of whether anyone's standing there.
        float depth  = ctx.tileDepthScale(row);
        float panelW = ctx.battlefield.getPanelWidth() * depth;
        float panelH = ctx.battlefield.getPanelRenderHeight() * depth;
        Vector2 tilePos = ctx.projectedTileWorld(col, row);
        float cx = tilePos.x;
        float cy = tilePos.y + panelH * 0.5f;
        float size = Math.max(panelW, panelH);

        ctx.vfx.add(new ClashEffect(def.getVfxTexture(), def.getVfxAnimation(), def.getVfxTint(),
                                    cx, cy, size, worldZ));
    }

    private void applyHit(BattleContext ctx, int col) {
        Combatant target = ctx.combatantAt(col, row);
        if (target == null) return;
        if (target.getTeam() == combatant.getTeam()) return;
        applyEffectsTo(target);
    }
}
