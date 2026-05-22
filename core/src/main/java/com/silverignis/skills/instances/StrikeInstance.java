package com.silverignis.skills.instances;

import com.badlogic.gdx.math.Vector2;
import com.silverignis.entities.ClashEffect;
import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillInstance;
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

    private final int strikeFromCol;
    private final int targetCol;
    private final int row;

    public StrikeInstance(Skill def, Combatant combatant) {
        super(def, combatant);
        this.strikeFromCol = originCol + 1;
        this.targetCol     = originCol + 2;
        this.row           = originRow;

        acquireInputLock();
    }

    @Override
    public void update(float delta, BattleContext ctx) {
        if (!primed) {
            ctx.movementSystem.forceGridTeleport(combatant, strikeFromCol, row);
            primed = true;
        }
        phaseTime += delta;

        switch (phase) {
            case DASH_FORWARD:
                if (phaseTime >= DASH_FORWARD_TIME) {
                    enterHit(ctx);
                }
                break;

            case HIT:
                if (phaseTime >= HIT_TIME) {
                    enterDashBack(ctx);
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

    private void enterDashBack(BattleContext ctx) {
        phase = Phase.DASH_BACK;
        phaseTime = 0f;
        ctx.movementSystem.forceGridTeleport(combatant, originCol, originRow);
    }

    private void spawnSlashVfx(BattleContext ctx) {
        // VFX lands on the target tile regardless of whether anyone's standing there.
        float depth  = ctx.tileDepthScale(row);
        float panelW = ctx.battlefield.getPanelWidth() * depth;
        float panelH = ctx.battlefield.getPanelRenderHeight() * depth;
        Vector2 tilePos = ctx.projectedTileWorld(targetCol, row);
        float cx = tilePos.x;
        float cy = tilePos.y + panelH * 0.5f;
        float size = Math.max(panelW, panelH);

        ctx.vfx.add(new ClashEffect(def.getVfxTexture(), cx, cy, size));
    }

    private void applyHit(BattleContext ctx) {
        Combatant target = ctx.combatantAt(targetCol, row);
        if (target == null) return;
        if (target.getTeam() == combatant.getTeam()) return;
        applyEffectsTo(target, ctx);
    }
}
