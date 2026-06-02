package com.silverignis.skills.instances;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillInstance;
import com.silverignis.skills.effects.Effect;
import com.silverignis.skills.effects.EffectType;
import com.silverignis.systems.BattleContext;
import com.silverignis.systems.combat.Combatant;
import com.silverignis.systems.combat.Trigger;
import com.silverignis.systems.combat.event.TriggerEvent;

public class AuraInstance extends SkillInstance {

    private static final float EXPAND_TIME    = 0.20f;
    private static final float ACTIVE_TIME    = 3.00f;
    private static final float FADE_TIME      = 0.20f;

    private enum Phase { EXPAND, ACTIVE, FADE, DONE }

    private Phase phase = Phase.EXPAND;
    private float phaseTime = 0f;

    private final Sprite sprite;
    private float activeDuration;

    public AuraInstance(Skill def, Combatant combatant, BattleContext ctx) {
        super(def, combatant, ctx);
        this.sprite = new Sprite(def.getVfxTexture());
        Color tint = def.getVfxTint();
        if (tint != null) sprite.setColor(tint);
    }

    @Override
    public void update(float delta) {
        if (!combatant.isAlive()) { finish(); return; }
        phaseTime += delta;

        switch (phase) {
            case EXPAND:
                if (phaseTime >= EXPAND_TIME) enterActive(battleContext());
                break;
            case ACTIVE:
                if (shouldFade()) enterFade();
                break;
            case FADE:
                if (phaseTime >= FADE_TIME) {
                    phase = Phase.DONE;
                    finish();
                }
                break;
            case DONE:
                break;
        }
    }

    private void enterActive(BattleContext ctx) {
        phase = Phase.ACTIVE;
        phaseTime = 0f;
        activeDuration = computeActiveDuration();
        if (combatant.isAlive()){
            applyEffectsTo(combatant);
            ctx.triggerBus.fire(new TriggerEvent(Trigger.ON_TICK, combatant, null)); //might need move to status onTick??
        }
    }

    private void enterFade() {
        phase = Phase.FADE;
        phaseTime = 0f;
    }

    private float computeActiveDuration() {
        float max = 0f;
        boolean hasStatusEffect = false;
        for (Effect e : def.getEffects()){
            if (e.getType() == EffectType.APPLY_STATUS){
                hasStatusEffect = true;
                max = Math.max(e.getDuration(), max);
            }
        }
        return hasStatusEffect ? max : ACTIVE_TIME;
    }

    public boolean shouldFade(){
        if (phaseTime >= activeDuration) return true;

        boolean hasStatusEffect = false;
        for(Effect effect : def.getEffects()){
            hasStatusEffect = true;
            if(combatant.getStatusContainer().has(effect.getStatusType())){
                return false;
            }
        }
        return hasStatusEffect;
    }

    @Override
    public void render(SpriteBatch batch, BattleContext ctx) {
        if (phase == Phase.DONE) return;

        float panelW = ctx.battlefield.getPanelWidth();
        float panelH = ctx.battlefield.getPanelHeight();

        float cx = pos.getVisualX();
        float cy = pos.getVisualY() + panelH * 0.5f;

        float scale;
        float alpha;

        switch (phase) {
            case EXPAND:
                scale = 0.3f + 0.7f * (phaseTime / EXPAND_TIME);
                alpha = 0.5f + 0.5f * (phaseTime / EXPAND_TIME);
                break;
            case ACTIVE:
                scale = 1f;
                alpha = 0.7f;
                break;
            case FADE:
                scale = 1f + 0.2f * (phaseTime / FADE_TIME);
                alpha = 0.7f * (1f - phaseTime / FADE_TIME);
                break;
            default:
                return;
        }

        float size = Math.max(panelW, panelH) * 1.6f * scale;
        sprite.setBounds(cx - size * 0.5f, cy - size * 0.5f, size, size);
        sprite.setAlpha(alpha);
        sprite.draw(batch);
        sprite.setAlpha(1f);
    }

    @Override
    public float depth() {
        return combatant.getGridPosition().getWorldZ();
    }
}
