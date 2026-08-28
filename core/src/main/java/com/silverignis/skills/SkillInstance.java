package com.silverignis.skills;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.silverignis.components.Caster;
import com.silverignis.components.GridPosition;
import com.silverignis.components.Stats;
import com.silverignis.components.Team;
import com.silverignis.entities.Battlefield;
import com.silverignis.particles.Anchor;
import com.silverignis.particles.Channel;
import com.silverignis.particles.Drive;
import com.silverignis.particles.EmitterHandle;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.particles.VfxFactory;
import com.silverignis.render.RenderContext;
import com.silverignis.render.RenderLayer;
import com.silverignis.render.SceneRenderable;
import com.silverignis.skills.effects.Effect;
import com.silverignis.skills.visuals.*;
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

    /** Handles for the skill's layered particle effects (from {@code def.getVfx()}), stopped on finish. */
    private final Array<EmitterHandle> vfxHandles = new Array<>(false, 4);

    protected SkillInstance(Skill def, Combatant combatant) {
        this.def       = def;
        this.combatant = combatant;
        this.caster    = this.combatant.getCaster();
        this.pos       = this.combatant.getGridPosition();
        this.originCol = pos.getCol();
        this.originRow = pos.getRow();
        this.worldZ    = Battlefield.floorZ(originRow);

        this.visual = SkillVisuals.has(def.getId()) ? SkillVisuals.create(def.getId()) : null;
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

    public final void finish() {
        if (resolved) return;
        resolved = true;
        releaseInputLock();
        if (visual != null && engine != null) visual.onTrigger(Trigger.END, visualState, engine);
        onFinish();
    }

    /** Base stops the layered particle emitters (spawning halts; live particles age out).
     *  Subclasses that override MUST call {@code super.onFinish()}. */
    protected void onFinish() {
        for (EmitterHandle h : vfxHandles) if (h != null) h.stop();
        vfxHandles.clear();
    }

    /** Plays every effect in {@code def.getVfx()}, layered, at {@code anchor}, driven by {@code drive}.
     *  Call once at the shape's trigger moment; handles are stopped in {@link #onFinish()}. */
    protected void playVfx(Anchor anchor, Drive drive, SkillContext ctx) {
        ParticleEngine engine = ctx.particleEngine;
        if (engine == null || def.getVfx().isEmpty()) return;
        int dir = combatant.getTeam() == Team.PLAYER ? 1 : -1;
        for (VfxFactory f : def.getVfx()) {
            vfxHandles.add(f.create(def.getElement(), def.getVfxTint(), dir)
                            .play(engine, anchor, drive, Channel.COMBAT));
        }
    }

    protected void playVfx(Anchor anchor, SkillContext ctx) { playVfx(anchor, Drive.FULL, ctx); }

    /** Ground point at the center of a grid tile — the natural anchor for tile-targeted shapes. */
    protected static Anchor tileAnchor(int col, int row) {
        return out -> out.set(Battlefield.floorX(col), 0f, Battlefield.floorZ(row));
    }

    public final boolean isResolved() { return resolved; }

    public final boolean isFinished() {
        return resolved && (visual == null || visual.isDone());
    }

    protected void applyEffectsTo(Combatant target, SkillContext ctx) {
        if (target == null || !target.isAlive()) return;

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
                    ctx.damageSystem.apply(new DamageEvent(combatant, target, scaledBase, DamageEvent.Source.SKILL, def));
                    break;
                case HEAL:
                    ctx.damageSystem.heal(new HealEvent(target, e.getValue()));
                    break;
                case APPLY_STATUS:
                    if (MathUtils.random(99) < e.getChance()) {
                        target.getStatusContainer().apply(
                            StatusFactory.create(e.getStatusType(), e.getDuration(), e.getValue()),
                            ctx.triggerBus
                        );
                    }
                    break;
                case KNOCKBACK:
                    //reserved
                    break;
            }
        }
    }

    public interface TileSink { void tile(int col, int row); }

    public void coveredTiles(TileSink sink){}

    public final void tick(float delta, SkillContext ctx) {
        if (engine == null ) engine = ctx.particleEngine;
        if (visual != null && !castFired ) {
            castFired = true;
            writeCasterPos();
            visual.onTrigger(Trigger.CAST, visualState, ctx.particleEngine);
        }
        if (!resolved) update(delta, ctx);
        if(visual != null){
            visualState.elapsed += delta;
            writeCasterPos();
            visual.update(delta);
        }
    }

    protected final void setPhase(Phase phase, SkillContext ctx) {
        visualState.phase = phase;
        visualState.phaseProgress = 0f;
        if (visual != null) visual.onTrigger(phase.trigger, visualState,ctx.particleEngine);
    }

    protected final void fireImpact(float x, float y, float z, SkillContext ctx) {
        if (visual == null) return;
        visualState.impactPos.set(x, y, z);
        visual.onTrigger(Trigger.IMPACT, visualState, ctx.particleEngine);
    }

    public final void fireClash(float x, float y, float z, ParticleEngine engine){
        if (visual == null) return;
        visualState.impactPos.set(x, y, z);
        visual.onTrigger(Trigger.CLASH, visualState, engine);
    }

    public final SkillVisual visual() { return visual; }

    public abstract void update(float delta, SkillContext ctx);

    public float depth() { return worldZ; }

    public RenderLayer layer() { return visual != null ? visual.layer() : RenderLayer.BILLBOARD; }

    @Override
    public void render(RenderContext rc) {
        // An instance spawned mid-tick (the lob's cloud) is submitted for
        // render before its first tick: no CAST yet, no phase, nothing to draw.
        if (visual != null && castFired) visual.render(rc, visualState);
    }

    private void writeCasterPos() {
        visualState.casterPos.set(Battlefield.floorX(combatant.getVisualCol()), 0f, Battlefield.floorZ(combatant.getVisualRow()));
    }
}
