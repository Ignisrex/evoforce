package com.silverignis.skills;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.silverignis.components.Caster;
import com.silverignis.components.GridPosition;
import com.silverignis.components.Stats;
import com.silverignis.skills.effects.Effect;
import com.silverignis.systems.BattleContext;
import com.silverignis.systems.CombatSystem;
import com.silverignis.systems.combat.Combatant;
import com.silverignis.systems.combat.StatusFactory;
import com.silverignis.systems.combat.event.DamageEvent;
import com.silverignis.systems.combat.event.HealEvent;

public abstract class SkillInstance {

    protected final Skill def;
    protected final Combatant combatant;
    protected final Caster caster;
    protected final GridPosition pos;

    /** Logical caster position at the moment the skill was fired. */
    protected final int originCol;
    protected final int originRow;

    private boolean finished  = false;
    private boolean lockTaken = false;

    protected SkillInstance(Skill def, Combatant combatant) {
        this.def       = def;
        this.combatant = combatant;
        this.caster    = this.combatant.getCaster();
        this.pos       = this.combatant.getGridPosition();
        this.originCol = pos.getCol();
        this.originRow = pos.getRow();
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
    }

    public final boolean isFinished() { return finished; }

    protected void applyEffectsTo(Combatant target, BattleContext ctx) {
        if(target == null || !target.isAlive()) return;

        for (Effect e : def.getEffects()) {
            switch (e.getType()) {
                case DAMAGE:
                    Stats casterStats =  combatant.getStats();
                    int scaledBase = e.getValue()
                        + Math.round(casterStats.getPower() * def.getPowerScale())
                        + Math.round(casterStats.getMagic() * def.getMagicScale());
                    ctx.damageSystem.apply(new DamageEvent(combatant, target, scaledBase, DamageEvent.Source.SKILL, def));
                    break;
                case HEAL:
                    ctx.damageSystem.heal(new HealEvent(target, e.getValue()));
                    break;
                case APPLY_STATUS:
                    if(MathUtils.random(99) < e.getChance()){
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

    public abstract void update(float delta, BattleContext ctx);

    public void render(SpriteBatch batch, BattleContext ctx) {}
}
