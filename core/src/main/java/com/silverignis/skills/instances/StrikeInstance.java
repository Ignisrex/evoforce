package com.silverignis.skills.instances;

import com.silverignis.entities.Battlefield;
import com.silverignis.skills.visuals.Phase;
import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillContext;
import com.silverignis.skills.SkillInstance;
import com.silverignis.skills.StrikeConfig;
import com.silverignis.systems.combat.Combatant;

public class StrikeInstance extends SkillInstance {

    private static final float DASH_FORWARD_TIME = 0.10f;
    private static final float HIT_TIME          = 0.20f;
    private static final float DASH_BACK_TIME    = 0.10f;

    private float phaseTime = 0f;
    private boolean primed = false;

    private final int dir;
    private final int strikeFromCol;
    private final int firstTargetCol;
    private final int hitTiles;
    private final int row;

    public StrikeInstance(Skill def, Combatant combatant) {
        super(def, combatant);
        this.dir = visualState.dir;

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
    public void update(float delta, SkillContext ctx) {
        if (!primed) {
            ctx.movementSystem.forceGridTeleport(combatant, strikeFromCol, row);
            setPhase(Phase.WINDUP, ctx);
            primed = true;
        }
        phaseTime += delta;

        switch (visualState.phase) {
            case WINDUP -> {
                visualState.phaseProgress = Math.min(phaseTime / DASH_FORWARD_TIME, 1f);
                if (phaseTime >= DASH_FORWARD_TIME) enterHit(ctx);
            }
            case ACTIVE -> {
                visualState.phaseProgress = Math.min(phaseTime/ HIT_TIME, 1f);
                if (phaseTime >= HIT_TIME) enterDashBack(ctx);
            }
            case RECOVERY -> {
                visualState.phaseProgress = Math.min(phaseTime/ DASH_BACK_TIME, 1f);
                if (phaseTime >= DASH_BACK_TIME) finish();
            }
        }
    }

    private void enterHit(SkillContext ctx) {
        phaseTime = 0f;
        setPhase(Phase.ACTIVE, ctx);
        visualState.hitTiles.clear();
        for(int i = 0; i < hitTiles; i++) {
            int col = firstTargetCol + dir * i;
            visualState.hitTiles.add(col);
            fireImpact(Battlefield.floorX(col), 0f, Battlefield.floorZ(row), ctx);
            applyHit(ctx, col);
        }
    }

    private void enterDashBack(SkillContext ctx) {
        phaseTime = 0f;
        setPhase(Phase.RECOVERY, ctx);
        ctx.movementSystem.forceGridTeleport(combatant, originCol, originRow);
    }

    private void applyHit(SkillContext ctx, int col) {
        Combatant target = ctx.battleState.combatantAt(col, row);
        if (target == null) return;
        if (target.getTeam() == combatant.getTeam()) return;
        applyEffectsTo(target, ctx);
    }

    public void coveredTiles(TileSink sink) {
        if (visualState.phase == Phase.WINDUP || visualState.phase == Phase.ACTIVE) {
            for (int i = 0; i < hitTiles; i++) sink.tile(firstTargetCol + dir * i, row);
        }

    }
}
