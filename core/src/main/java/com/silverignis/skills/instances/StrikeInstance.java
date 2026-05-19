package com.silverignis.skills.instances;

import com.silverignis.components.Caster;
import com.silverignis.components.GridPosition;
import com.silverignis.entities.ClashEffect;
import com.silverignis.entities.Enemy;
import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillInstance;
import com.silverignis.systems.BattleContext;

public class StrikeInstance extends SkillInstance {

    private static final float DASH_FORWARD_TIME = 0.10f;
    private static final float HIT_TIME          = 0.20f;
    private static final float DASH_BACK_TIME    = 0.10f;

    private enum Phase { DASH_FORWARD, HIT, DASH_BACK, DONE }

    private Phase phase = Phase.DASH_FORWARD;
    private float phaseTime = 0f;

    private final int strikeFromCol;
    private final int targetCol;
    private final int row;

    public StrikeInstance(Skill def, Caster caster, GridPosition pos) {
        super(def, caster, pos);
        this.strikeFromCol = originCol + 1;
        this.targetCol     = originCol + 2;
        this.row           = originRow;

        acquireInputLock();
        pos.setTile(strikeFromCol, row);
    }

    @Override
    public void update(float delta, BattleContext ctx) {
        phaseTime += delta;

        switch (phase) {
            case DASH_FORWARD:
                if (phaseTime >= DASH_FORWARD_TIME) {
                    enterHit(ctx);
                }
                break;

            case HIT:
                if (phaseTime >= HIT_TIME) {
                    enterDashBack();
                }
                break;

            case DASH_BACK:
                if (phaseTime >= DASH_BACK_TIME) {
                    phase = Phase.DONE;
                    finish();
                }
                break;

            case DONE:
                // Nothing left to do; CombatSystem will reap us next pass.
                break;
        }
    }

    private void enterHit(BattleContext ctx) {
        phase = Phase.HIT;
        phaseTime = 0f;

        spawnSlashVfx(ctx);
        applyHit(ctx);
    }

    private void enterDashBack() {
        phase = Phase.DASH_BACK;
        phaseTime = 0f;
        pos.setTile(originCol, originRow);
    }

    private void spawnSlashVfx(BattleContext ctx) {
        float panelW = ctx.battlefield.getPanelWidth() * ctx.enemy.getDepthScale();
        float panelH = ctx.battlefield.getPanelRenderHeight() * ctx.enemy.getDepthScale();
        // Use visual (projected) position so VFX lands on the enemy sprite
        float cx = ctx.enemy.getVisualX();
        float cy = ctx.enemy.getVisualY() + panelH * 0.5f;
        float size = Math.max(panelW, panelH);

        ctx.vfx.add(new ClashEffect(def.getVfxTexture(), cx, cy, size));
    }

    private void applyHit(BattleContext ctx) {
        Enemy target = ctx.enemy;
        if (target == null || !target.isAlive()) return;
        if (target.getCol() != targetCol || target.getRow() != row) return;
        applyEffectsTo(target);
    }
}
