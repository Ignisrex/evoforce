package com.silverignis.systems;

import com.silverignis.components.Caster;
import com.silverignis.entities.BattleVfx;
import com.silverignis.entities.Battlefield;
import com.silverignis.particles.Anchor;
import com.silverignis.particles.Channel;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.particles.Vfx;
import com.silverignis.render.WorldRenderer;
import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillContext;
import com.silverignis.skills.SkillFactory;
import com.silverignis.skills.SkillInstance;
import com.silverignis.skills.elements.Element;
import com.silverignis.skills.instances.ProjectileInstance;
import com.silverignis.systems.combat.Combatant;
import com.silverignis.systems.combat.DamageSystem;
import com.silverignis.systems.combat.TriggerBus;

import java.util.ArrayList;
import java.util.List;

public class CombatSystem {

    private final BattleState battleState;
    private final ParticleEngine particleEngine;
    private final SkillContext ctx;

    private final List<SkillInstance> active = new ArrayList<>();

    public CombatSystem(BattleState battleState,
                        DamageSystem damageSystem,
                        TriggerBus triggerBus,
                        MovementSystem movementSystem,
                        ParticleEngine particleEngine,
                        List<BattleVfx> vfxSink) {
        this.battleState    = battleState;
        this.particleEngine = particleEngine;
        // Lob needs to spawn a child Zone, so the context has to name this
        // system. Building it here keeps that cycle immutable — it used to be a
        // mutable field set from outside after construction.
        this.ctx = new SkillContext(battleState, damageSystem, triggerBus,
                                    movementSystem, this, particleEngine, vfxSink);
    }

    /** Create and spawn a fresh instance for {@code skill}, cast by {@code combatant}. */
    public void spawn(Skill skill, Combatant combatant){
        active.add(SkillFactory.create(skill, combatant));
    }

    /** Spawn an already-built instance — for derived/synthetic instances (e.g. a lingering cloud). */
    public void spawn(SkillInstance instance){
        active.add(instance);
    }

    public void update(float delta) {
        // Iterate over a snapshot index so an instance that spawns another
        // mid-update doesn't get double-ticked this frame.
        for (int i = 0, n = active.size(); i < n; i++){
            active.get(i).update(delta, ctx);
        }

        resolveProjectileClashes();

        active.removeIf(SkillInstance::isFinished);
    }

    public void tickStatuses(float delta){
        if (battleState.player.isAlive()){
            battleState.player.getStatusContainer().update(delta, ctx.damageSystem, ctx.triggerBus);
        }

        for (var enemy : battleState.enemies){
            if(!enemy.isAlive()) continue;
            enemy.getStatusContainer().update(delta, ctx.damageSystem, ctx.triggerBus);
        }
    }

    private void resolveProjectileClashes() {
        for (int i = 0; i < active.size(); i++) {
            SkillInstance ai = active.get(i);
            if (ai.isFinished() || !(ai instanceof ProjectileInstance)) continue;
            ProjectileInstance a = (ProjectileInstance) ai;

            for (int j = i + 1; j < active.size(); j++) {
                SkillInstance bi = active.get(j);
                if (bi.isFinished() || !(bi instanceof ProjectileInstance)) continue;
                ProjectileInstance b = (ProjectileInstance) bi;
                if (a.getCaster().getTeam() == b.getCaster().getTeam()) continue;
                if (a.getRow() != b.getRow()) continue;

                // Within half a tile of each other, measured in columns — so the
                // clash window is the same on every row.
                if (Math.abs(a.getColPos() - b.getColPos()) > 0.5f) continue;

                spawnClash(a, b);
                a.finish();
                b.finish();
                break;
            }
        }
    }

    /** How high above the floor the impact burst centers — projectiles fly at sprite height. */
    private static final float IMPACT_HEIGHT = 0.35f;

    private void spawnClash(ProjectileInstance a, ProjectileInstance b) {
        float midCol = (a.getColPos() + b.getColPos()) * 0.5f;
        Vfx.impact(Element.NONE).play(particleEngine,
            Anchor.at(Battlefield.floorX(midCol), IMPACT_HEIGHT, Battlefield.floorZ(a.getRow())),
            Channel.COMBAT);
    }

    public void submitRenderables(WorldRenderer renderer) {
        for (SkillInstance s : active) {
            if(s.isFinished()) continue;
            renderer.submit(s);
        }
    }

    public void finishAll() {
        for (SkillInstance inst : active) inst.finish();
        active.clear();
        particleEngine.clear(Channel.COMBAT);
    }

    /**
     * The one definition of "cast a skill": gate, start the cooldown, spawn.
     * Player input and enemy AI both route through here — each used to carry its
     * own copy of these checks, and the copies disagreed.
     *
     * @return whether the skill actually fired.
     */
    public boolean tryCast(Combatant combatant, Skill skill) {
        if (!combatant.canUse(skill)) return false;
        cast(combatant, skill);
        return true;
    }

    /**
     * Cast that skips the gate. Staged slot fires and released bursts use this:
     * the card was filtered against cooldowns when it was staged and has already
     * been popped off the slot by now, so re-gating would silently swallow it
     * instead of refusing it.
     */
    public void cast(Combatant combatant, Skill skill) {
        combatant.getCaster().getDeck().onUsed(skill);
        spawn(skill, combatant);
    }

    public void loadSkill(Combatant combatant, Skill skill) {
        combatant.getCaster().loadSkill(skill);
    }

    public void resolveLoadedSkills(Combatant combatant) {
        Caster caster = combatant.getCaster();
        List<Skill> loaded = caster.releaseLoadedSkills();

        //handle advance skill check or rapid cast
        Skill advanced = null; //matchRecipe(loaded) -> future impl
        if(advanced != null) {
            cast(combatant, advanced);
        } else {
            for (Skill s : loaded) cast(combatant, s);
        }
    }

    public void collectCoveredTiles(SkillInstance.TileSink sink) {
        for (SkillInstance s : active) {
            if (!s.isFinished()) s.coveredTiles(sink);
        }
    }

    public boolean hasActive(){
        return !active.isEmpty();
    }

}
