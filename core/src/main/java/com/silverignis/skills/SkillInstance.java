package com.silverignis.skills;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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
import com.silverignis.systems.BattleContext;
import com.silverignis.systems.combat.Combatant;
import com.silverignis.systems.combat.StatusContainer;
import com.silverignis.systems.combat.StatusFactory;
import com.silverignis.systems.combat.StatusType;
import com.silverignis.systems.combat.event.DamageEvent;
import com.silverignis.systems.combat.event.HealEvent;

public abstract class SkillInstance implements SceneRenderable {

    protected final Skill def;
    protected final Combatant combatant;
    protected final Caster caster;
    protected final GridPosition pos;

    /** Logical caster position at the moment the skill was fired. */
    protected final int originCol;
    protected final int originRow;
    protected final float worldZ;

    private boolean finished  = false;
    private boolean lockTaken = false;

    /** Handles for the skill's layered particle effects (from {@code def.getVfx()}), stopped on finish. */
    private final Array<EmitterHandle> vfxHandles = new Array<>(false, 4);

    private final BattleContext battleContext;

    protected SkillInstance(Skill def, Combatant combatant, BattleContext ctx) {
        this.def       = def;
        this.combatant = combatant;
        this.caster    = this.combatant.getCaster();
        this.pos       = this.combatant.getGridPosition();
        this.originCol = pos.getCol();
        this.originRow = pos.getRow();
        this.worldZ = combatant.getGridPosition().getWorldZ();
        this.battleContext = ctx;
    }

    public Skill        getDef()    { return def; }
    public Combatant    getCombatant() { return combatant; }
    public Caster       getCaster() { return caster; }
    public GridPosition getPos()    { return pos; }

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
        if (finished) return;
        finished = true;
        releaseInputLock();
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
    protected void playVfx(Anchor anchor, Drive drive) {
        ParticleEngine engine = battleContext.particleEngine;
        if (engine == null || def.getVfx().isEmpty()) return;
        int dir = combatant.getTeam() == Team.PLAYER ? 1 : -1;
        for (VfxFactory f : def.getVfx()) {
            vfxHandles.add(f.create(def.getElement(), def.getVfxTint(), dir)
                            .play(engine, anchor, drive, Channel.COMBAT));
        }
    }

    protected void playVfx(Anchor anchor) { playVfx(anchor, Drive.FULL); }

    /** Ground point at the center of a grid tile — the natural anchor for tile-targeted shapes. */
    protected Anchor tileAnchor(int col, int row) {
        Battlefield bf = battleContext.battlefield;
        return out -> out.set(bf.floorX(col), 0f, bf.floorZ(row));
    }

    public final boolean isFinished() { return finished; }

    protected void applyEffectsTo(Combatant target) {
        if(target == null || !target.isAlive()) return;

        for (Effect e : def.getEffects()) {
            switch (e.getType()) {
                case DAMAGE:
                    Stats casterStats =  combatant.getStats();
                    StatusContainer casterStatus = combatant.getStatusContainer();
                    float powerMul = casterStatus.has(StatusType.POWER_UP) ? 3f : 1f;
                    float magicMul = casterStatus.has(StatusType.MAGIC_UP) ? 3f : 1f;
                    int scaledBase = e.getValue()
                        + Math.round(casterStats.getPower() * powerMul * def.getPowerScale())
                        + Math.round(casterStats.getMagic() * magicMul * def.getMagicScale());
                    battleContext.damageSystem.apply(new DamageEvent(combatant, target, scaledBase, DamageEvent.Source.SKILL, def));
                    break;
                case HEAL:
                    battleContext.damageSystem.heal(new HealEvent(target, e.getValue()));
                    break;
                case APPLY_STATUS:
                    if(MathUtils.random(99) < e.getChance()){
                        target.getStatusContainer().apply(
                            StatusFactory.create(e.getStatusType(), e.getDuration(), e.getValue()),
                            battleContext.triggerBus
                        );
                    }
                    break;
                case KNOCKBACK:
                    //reserved
                    break;
            }
        }
    }

    public BattleContext battleContext() {
        return battleContext;
    }

    public abstract void update(float delta);

    public void render(SpriteBatch batch, BattleContext ctx) {}

    public float depth() {
        return worldZ;
    }

    public RenderLayer layer() { return RenderLayer.BILLBOARD; }
    public void render(RenderContext rc) {
        render(rc.batch, this.battleContext);
    }
}
