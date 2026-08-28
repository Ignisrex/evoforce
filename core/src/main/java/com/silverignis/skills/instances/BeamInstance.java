package com.silverignis.skills.instances;

import com.silverignis.entities.Battlefield;
import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillContext;
import com.silverignis.skills.SkillInstance;
import com.silverignis.skills.visuals.Phase;
import com.silverignis.systems.combat.Combatant;

public class BeamInstance extends SkillInstance {

    private static final float CHARGE_TIME = 0.20f;
    private static final float FIRE_TIME   = 0.70f;
    private static final float FADE_TIME   = 0.25f;

    private float phaseTime = 0f;
    private boolean hitApplied = false;

    private final int row;
    private final int dir;

    public BeamInstance(Skill def, Combatant combatant) {
        super(def, combatant);
        this.row = originRow;
        this.dir = visualState.dir;
        visualState.nearCol = originCol + dir;
        visualState.farCol = dir > 0 ? Battlefield.COLS - 1 : 0;
        acquireInputLock();
    }

    @Override
    public void update(float delta, SkillContext ctx) {
        if (visualState.phase == null) setPhase(Phase.WINDUP, ctx);
        phaseTime += delta;


        switch (visualState.phase) {
            case WINDUP -> {
                visualState.phaseProgress = Math.min(phaseTime/CHARGE_TIME, 1f);
                if (phaseTime >= CHARGE_TIME) {
                    phaseTime = 0f; setPhase(Phase.ACTIVE, ctx); applyHit(ctx);
                }
            }
            case ACTIVE -> {
                visualState.phaseProgress = Math.min(phaseTime / FIRE_TIME, 1f);
                if (phaseTime >= FIRE_TIME) { phaseTime = 0f; setPhase(Phase.RECOVERY, ctx); releaseInputLock();}
            }
            case RECOVERY -> {
                visualState.phaseProgress = Math.min(phaseTime / FADE_TIME, 1f);
                if (phaseTime >= FADE_TIME) finish();
            }
        }
    }

    private void applyHit(SkillContext ctx) {
        if (hitApplied) return;
        hitApplied = true;
        for (Combatant target : ctx.battleState.opposingOnRow(combatant, row)) {
            // Only targets ahead of the caster in the beam's facing direction.
            if ((target.getCol() - originCol) * dir > 0) applyEffectsTo(target, ctx);
        }
    }

    @Override
    public void coveredTiles(TileSink sink) {
        if (visualState.phase != Phase.ACTIVE) return;
        for (int c = originCol + dir; c >= 0 && c < Battlefield.COLS; c += dir) sink.tile(c, row);
    }
}
