package com.silverignis.skills.instances;

import com.silverignis.components.Team;
import com.silverignis.entities.ClashEffect;
import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillContext;
import com.silverignis.skills.SkillInstance;
import com.silverignis.skills.StrikeConfig;
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

    public StrikeInstance(Skill def, Combatant combatant) {
        super(def, combatant);
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
    public void update(float delta, SkillContext ctx) {
        if (!primed) {
            ctx.movementSystem.forceGridTeleport(combatant, strikeFromCol, row);
            primed = true;
        }
        phaseTime += delta;

        switch (phase) {
            case DASH_FORWARD:
                if (phaseTime >= DASH_FORWARD_TIME) enterHit(ctx);
                break;

            case HIT:
                if (phaseTime >= HIT_TIME) enterDashBack(ctx);
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

    private void enterHit(SkillContext ctx) {
        phase = Phase.HIT;
        phaseTime = 0f;
        combatant.getAnimController().enterAttack();

        for (int i = 0; i < hitTiles; i++) {
            int col = firstTargetCol + dir * i;
            spawnSlashVfx(ctx, col);
            applyHit(ctx, col);
        }
        playVfx(tileAnchor(firstTargetCol, row), ctx);   // layered particle effects at the struck tile
    }

    private void enterDashBack(SkillContext ctx) {
        phase = Phase.DASH_BACK;
        phaseTime = 0f;
        ctx.movementSystem.forceGridTeleport(combatant, originCol, originRow);
    }

    /** VFX lands on the target tile regardless of whether anyone is standing there.
     *  Named by tile — the effect resolves its own screen position when drawn. */
    private void spawnSlashVfx(SkillContext ctx, int col) {
        ctx.vfxSink.add(new ClashEffect(def.getVfxTexture(), def.getVfxAnimation(),
                                        def.getVfxTint(), col, row));
    }

    private void applyHit(SkillContext ctx, int col) {
        Combatant target = ctx.battleState.combatantAt(col, row);
        if (target == null) return;
        if (target.getTeam() == combatant.getTeam()) return;
        applyEffectsTo(target, ctx);
    }

    public void coveredTiles(TileSink sink) {
        if (phase != Phase.DASH_FORWARD && phase != Phase.HIT) return;
        for (int i = 0; i < hitTiles; i++) sink.tile(firstTargetCol + dir * i, row);
    }
}
