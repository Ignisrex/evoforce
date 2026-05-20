package com.silverignis.skills.instances;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillInstance;
import com.silverignis.systems.BattleContext;
import com.silverignis.systems.combat.Combatant;
import com.silverignis.systems.combat.Trigger;
import com.silverignis.systems.combat.event.TriggerEvent;

public class AuraInstance extends SkillInstance {

    private static final float EXPAND_TIME    = 0.20f;
    private static final float ACTIVE_TIME    = 3.00f;
    private static final float FADE_TIME      = 0.20f;
    private static final float TICK_INTERVAL  = 0.50f;

    private enum Phase { EXPAND, ACTIVE, FADE, DONE }

    private Phase phase = Phase.EXPAND;
    private float phaseTime = 0f;
    private float tickTimer = 0f;

    private final Sprite sprite;

    public AuraInstance(Skill def, Combatant combatant) {
        super(def, combatant);
        this.sprite = new Sprite(def.getVfxTexture());
    }

    @Override
    public void update(float delta, BattleContext ctx) {
        if (!combatant.isAlive()) { finish(); return; }
        phaseTime += delta;

        switch (phase) {
            case EXPAND:
                if (phaseTime >= EXPAND_TIME) enterActive();
                break;
            case ACTIVE:
                tickTimer += delta;
                if (tickTimer >= TICK_INTERVAL) {
                    tickTimer -= TICK_INTERVAL;
                    applyTick(ctx);
                }
                if (phaseTime >= ACTIVE_TIME) enterFade();
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

    private void enterActive() {
        phase = Phase.ACTIVE;
        phaseTime = 0f;
    }

    private void enterFade() {
        phase = Phase.FADE;
        phaseTime = 0f;
    }

    private void applyTick(BattleContext ctx) {
        applyEffectsTo(combatant, ctx);//apply effects to caster {heal, regen  debuff}

        ctx.triggerBus.fire( new TriggerEvent(Trigger.ON_TICK, combatant, null)); //Notify status/aura tick listeners
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
}
