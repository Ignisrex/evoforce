package com.silverignis.skills;

import com.silverignis.entities.BattleVfx;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.systems.BattleState;
import com.silverignis.systems.CombatSystem;
import com.silverignis.systems.MovementSystem;
import com.silverignis.systems.combat.DamageSystem;
import com.silverignis.systems.combat.TriggerBus;

import java.util.List;

/**
 * Everything a running skill needs, minus any renderer.
 *
 * Handed to {@code SkillInstance.update} per tick rather than stored on the
 * instance: a skill can only reach the simulation while it is being ticked, and
 * has no route to one while it is being drawn. That is the whole point of the
 * split — the old BattleContext carried the camera and was held for life, so
 * "what can a skill touch" was unbounded in both directions.
 */
public final class SkillContext {

    public final BattleState    battleState;
    public final DamageSystem   damageSystem;
    public final TriggerBus     triggerBus;
    public final MovementSystem movementSystem;

    /** Lob spawns its own landing Zone; nothing else spawns children today. */
    public final CombatSystem   combatSystem;

    // ponytail: these two are presentation and do not belong here — they are the
    // last places the tick pushes into rendering. Both go when instances emit
    // events the scene subscribes to instead. They are at least projection-free
    // now: a skill names a tile, never a screen point.
    public final ParticleEngine   particleEngine;
    public final List<BattleVfx>  vfxSink;

    public SkillContext(BattleState battleState,
                        DamageSystem damageSystem,
                        TriggerBus triggerBus,
                        MovementSystem movementSystem,
                        CombatSystem combatSystem,
                        ParticleEngine particleEngine,
                        List<BattleVfx> vfxSink) {
        this.battleState    = battleState;
        this.damageSystem   = damageSystem;
        this.triggerBus     = triggerBus;
        this.movementSystem = movementSystem;
        this.combatSystem   = combatSystem;
        this.particleEngine = particleEngine;
        this.vfxSink        = vfxSink;
    }
}
