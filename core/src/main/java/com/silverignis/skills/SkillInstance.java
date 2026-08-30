package com.silverignis.skills;

import com.badlogic.gdx.math.MathUtils;
import com.silverignis.components.Caster;
import com.silverignis.components.GridPosition;
import com.silverignis.components.Stats;
import com.silverignis.components.Team;
import com.silverignis.entities.Battlefield;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.render.RenderContext;
import com.silverignis.render.RenderLayer;
import com.silverignis.render.SceneRenderable;
import com.silverignis.skills.effects.Effect;
import com.silverignis.skills.visuals.Phase;
import com.silverignis.skills.visuals.SkillVisual;
import com.silverignis.skills.visuals.SkillVisuals;
import com.silverignis.skills.visuals.Trigger;
import com.silverignis.skills.visuals.VisualState;
import com.silverignis.systems.combat.Combatant;
import com.silverignis.systems.combat.StatusContainer;
import com.silverignis.systems.combat.StatusFactory;
import com.silverignis.systems.combat.StatusType;
import com.silverignis.systems.combat.event.DamageEvent;
import com.silverignis.systems.combat.event.HealEvent;

/**
 * One execution of a skill. Origin is snapshotted at construction, so the
 * fire-time tile stays valid even if the caster moves.
 *
 * Holds no context: the simulation arrives as a {@link SkillContext} parameter
 * on {@link #update}, and rendering gets a {@link RenderContext} on
 * {@link #render}. An instance therefore cannot reach the camera while ticking,
 * nor the battle while drawing.
 *
 * The instance is pure sim. It writes a {@link VisualState} (anchors, phase,
 * progress) and fires {@link Trigger}s; the {@link SkillVisual} resolved from
 * the skill id owns everything about how that looks.
 */
public abstract class SkillInstance implements SceneRenderable {

    protected final Skill def;
    protected final Combatant combatant;
    protected final Caster caster;
    protected final GridPosition pos;

    /** Logical caster position at the moment the skill was fired. */
    protected final int originCol;
    protected final int originRow;
    protected final float worldZ;

    private boolean resolved  = false;
    private boolean lockTaken = false;

    protected final SkillVisual visual;
    protected final VisualState visualState = new VisualState();
    private boolean castFired = false;
    //cached for end state
    private ParticleEngine engine;

    protected SkillInstance(Skill def, Combatant combatant) {
        this.def       = def;
        this.combatant = combatant;
        this.caster    = this.combatant.getCaster();
        this.pos       = this.combatant.getGridPosition();
        this.originCol = pos.getCol();
        this.originRow = pos.getRow();
        this.worldZ    = Battlefield.floorZ(originRow);

        this.visual = SkillVisuals.create(def.getId());
        visualState.dir = combatant.getTeam() == Team.PLAYER ? 1 : -1;
        visualState.row = originRow;
        visualState.element = def.getElement();
        visualState.pose = combatant.getAnimController();
    }

    public Skill        getDef()       { return def; }
    public Combatant    getCombatant() { return combatant; }
    public Caster       getCaster()    { return caster; }
    public GridPosition getPos()       { return pos; }

    protected final void acquireInputLock() {
        if (!lockTaken && caster.getInputLock().lock(this)) {
            lockTaken = true;
        }
    }

    protected final void releaseInputLock() {
        if (lockTaken) {
            caster.getInputLock().unlock(this);
            lockTaken = false;
        }
    }

    /** Gameplay is over: locks released, END fired so the visual stops its
     *  emitters. The instance stays until the visual is done too. */
    public final void finish() {
        if (resolved) return;
        resolved = true;
        releaseInputLock();
        if (engine != null) visual.onTrigger(Trigger.END, visualState, engine);
    }

    /** Gameplay done: locks released, effects applied. What sim queries consult. */
    public final boolean isResolved() { return resolved; }

    /** Removable: gameplay done AND the visual has nothing left to show. */
    public final boolean isFinished() { return resolved && visual.isDone(); }

    protected void applyEffectsTo(Combatant target, SkillContext ctx) {
        if (target == null || !target.isAlive()) return;

        boolean landed = true;
        for (Effect e : def.getEffects()) {
            switch (e.getType()) {
                case DAMAGE:
                    Stats casterStats = combatant.getStats();
                    StatusContainer casterStatus = combatant.getStatusContainer();
                    float powerMul = casterStatus.has(StatusType.POWER_UP) ? 3f : 1f;
                    float magicMul = casterStatus.has(StatusType.MAGIC_UP) ? 3f : 1f;
                    int scaledBase = e.getValue()
                        + Math.round(casterStats.getPower() * powerMul * def.getPowerScale())
                        + Math.round(casterStats.getMagic() * magicMul * def.getMagicScale());
                    landed = ctx.damageSystem.apply(new DamageEvent(combatant, target, scaledBase, DamageEvent.Source.SKILL, def));
                    break;
                case HEAL:
                    ctx.damageSystem.heal(new HealEvent(target, e.getValue()));
                    break;
                case APPLY_STATUS:
                    if (!landed) break;
                    if (MathUtils.random(99) < e.getChance()) {
                        target.getStatusContainer().apply(
                            StatusFactory.create(e.getStatusType(), e.getDuration(), e.getValue()),
                            ctx.triggerBus
                        );
                    }
                    break;
                case KNOCKBACK:
                    if (!landed) break;
                    //reserved
                    break;
            }
        }
    }

    public interface TileSink { void tile(int col, int row); }

    public void coveredTiles(TileSink sink){}

    /** The one entry point CombatSystem calls each frame: gameplay sim while
     *  unresolved, visual clock always — a visual may outlive its gameplay. */
    public final void tick(float delta, SkillContext ctx) {
        if (engine == null) engine = ctx.particleEngine;
        if (!castFired) {
            castFired = true;
            writeCasterPos();
            visual.onTrigger(Trigger.CAST, visualState, ctx.particleEngine);
        }
        if (!resolved) update(delta, ctx);
        visualState.elapsed += delta;
        writeCasterPos();
        visual.update(delta);
    }

    /** Enter a phase: resets progress and fires the phase's trigger once.
     *  Subclasses keep ownership of durations and write phaseProgress each tick. */
    protected final void setPhase(Phase phase, SkillContext ctx) {
        visualState.phase = phase;
        visualState.phaseProgress = 0f;
        visual.onTrigger(phase.trigger, visualState, ctx.particleEngine);
    }

    protected final void fireImpact(float x, float y, float z, SkillContext ctx) {
        visualState.impactPos.set(x, y, z);
        visual.onTrigger(Trigger.IMPACT, visualState, ctx.particleEngine);
    }

    /** CLASH comes from CombatSystem, which has the engine but no context. */
    public final void fireClash(float x, float y, float z, ParticleEngine engine) {
        visualState.impactPos.set(x, y, z);
        visual.onTrigger(Trigger.CLASH, visualState, engine);
    }

    public abstract void update(float delta, SkillContext ctx);

    public float depth() { return worldZ; }

    public RenderLayer layer() { return visual.layer(); }

    @Override
    public void render(RenderContext rc) {
        // An instance spawned mid-tick (the lob's cloud) is submitted for
        // render before its first tick: no CAST yet, no phase, nothing to draw.
        if (castFired) visual.render(rc, visualState);
    }

    /** Caster anchor from the tweened visual tile, so a look riding the caster
     *  (aura, halo) follows a mid-step dash instead of snapping. */
    private void writeCasterPos() {
        visualState.casterPos.set(Battlefield.floorX(combatant.getVisualCol()), 0f,
                                  Battlefield.floorZ(combatant.getVisualRow()));
    }
}
