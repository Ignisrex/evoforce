package com.silverignis.skills.instances;

import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillContext;
import com.silverignis.skills.SkillInstance;
import com.silverignis.skills.effects.Effect;
import com.silverignis.skills.effects.EffectType;
import com.silverignis.skills.visuals.Phase;
import com.silverignis.systems.combat.Combatant;
import com.silverignis.systems.combat.Trigger;
import com.silverignis.systems.combat.event.TriggerEvent;

/** Self-cast aura: expands, applies its effects once, holds for the status
 *  duration (or a default), fades. Pure sim — the look rides the caster via
 *  the visual state's casterPos. */
public class AuraInstance extends SkillInstance {

    private static final float EXPAND_TIME    = 0.20f;
    private static final float ACTIVE_TIME    = 3.00f;
    private static final float FADE_TIME      = 0.20f;

    private float phaseTime = 0f;
    private float activeDuration;

    public AuraInstance(Skill def, Combatant combatant) {
        super(def, combatant);
    }

    @Override
    public void update(float delta, SkillContext ctx) {
        if (!combatant.isAlive()) { finish(); return; }
        if (visualState.phase == null) setPhase(Phase.WINDUP, ctx);
        phaseTime += delta;

        switch (visualState.phase) {
            case WINDUP -> {
                visualState.phaseProgress = Math.min(phaseTime / EXPAND_TIME, 1f);
                if (phaseTime >= EXPAND_TIME) enterActive(ctx);
            }
            case ACTIVE -> {
                visualState.phaseProgress = Math.min(phaseTime / activeDuration, 1f);
                if (shouldFade()) { phaseTime = 0f; setPhase(Phase.RECOVERY, ctx); }
            }
            case RECOVERY -> {
                visualState.phaseProgress = Math.min(phaseTime / FADE_TIME, 1f);
                if (phaseTime >= FADE_TIME) finish();
            }
        }
    }

    private void enterActive(SkillContext ctx) {
        phaseTime = 0f;
        activeDuration = computeActiveDuration();
        setPhase(Phase.ACTIVE, ctx);
        if (combatant.isAlive()) {
            applyEffectsTo(combatant, ctx);
            ctx.triggerBus.fire(new TriggerEvent(Trigger.ON_TICK, combatant, null)); //might need move to status onTick??
        }
    }

    private float computeActiveDuration() {
        float max = 0f;
        boolean hasStatusEffect = false;
        for (Effect e : def.getEffects()) {
            if (e.getType() == EffectType.APPLY_STATUS) {
                hasStatusEffect = true;
                max = Math.max(e.getDuration(), max);
            }
        }
        return hasStatusEffect ? max : ACTIVE_TIME;
    }

    public boolean shouldFade() {
        if (phaseTime >= activeDuration) return true;

        boolean hasStatusEffect = false;
        for (Effect effect : def.getEffects()) {
            hasStatusEffect = true;
            if (combatant.getStatusContainer().has(effect.getStatusType())) {
                return false;
            }
        }
        return hasStatusEffect;
    }

    /** Sorts where it's drawn — the caster's tweened tile, same as the caster's
     *  own billboard — so a row step can't flip it in front of the sprite. */
    @Override
    public float depth() {
        return visualState.casterPos.z;
    }
}
