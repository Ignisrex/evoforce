package com.silverignis.skills;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.silverignis.components.Caster;
import com.silverignis.components.GridPosition;
import com.silverignis.entities.Enemy;
import com.silverignis.entities.Player;
import com.silverignis.skills.effects.Effect;
import com.silverignis.systems.BattleContext;

public abstract class SkillInstance {

    protected final Skill def;
    protected final Caster caster;
    protected final GridPosition pos;

    /** Logical caster position at the moment the skill was fired. */
    protected final int originCol;
    protected final int originRow;

    private boolean finished  = false;
    private boolean lockTaken = false;

    protected SkillInstance(Skill def, Caster caster, GridPosition pos) {
        this.def       = def;
        this.caster    = caster;
        this.pos       = pos;
        this.originCol = pos.getCol();
        this.originRow = pos.getRow();
    }

    public Skill        getDef()    { return def; }
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

    protected final void finish() {
        if (finished) return;
        finished = true;
        releaseInputLock();
    }

    public final boolean isFinished() { return finished; }

    protected void applyEffectsTo(Enemy target) {
        for (Effect e : def.getEffects()) {
            switch (e.getType()) {
                case DAMAGE:
                    target.takeDamage(e.getValue());
                    break;
                case FREEZE:
                    if (MathUtils.random(99) < e.getChance()) {
                        target.applyFreeze(e.getDuration());
                    }
                    break;
                default:
                    break;
            }
        }
    }

    protected void applyEffectsTo(Player target) {
        for (Effect e : def.getEffects()) {
            switch (e.getType()) {
                case DAMAGE:
                    target.takeDamage(e.getValue());
                    break;
                case FREEZE:
                    if (MathUtils.random(99) < e.getChance()) {
                        target.applyFreeze(e.getDuration());
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public abstract void update(float delta, BattleContext ctx);

    public void render(SpriteBatch batch, BattleContext ctx) {}
}
