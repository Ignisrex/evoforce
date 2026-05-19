package com.silverignis.skills.instances;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.silverignis.components.Caster;
import com.silverignis.components.GridPosition;
import com.silverignis.entities.Battlefield;
import com.silverignis.entities.Enemy;
import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillInstance;
import com.silverignis.systems.BattleContext;

public class BeamInstance extends SkillInstance {

    private static final float CHARGE_TIME = 0.20f;
    private static final float FIRE_TIME   = 0.70f;
    private static final float FADE_TIME   = 0.25f;

    private enum Phase { CHARGE, FIRE, FADE, DONE }

    private Phase phase = Phase.CHARGE;
    private float phaseTime = 0f;
    private boolean hitApplied = false;

    private final Sprite sprite;
    private final Animation<TextureRegion> animation;
    private float stateTime = 0f;
    private final int row;

    public BeamInstance(Skill def, Caster caster, GridPosition pos) {
        super(def, caster, pos);
        this.row = originRow;
        acquireInputLock();

        this.animation = def.getVfxAnimation(); // may be null
        this.sprite    = new Sprite(def.getVfxTexture());
    }

    @Override
    public void update(float delta, BattleContext ctx) {
        phaseTime += delta;
        stateTime += delta;

        switch (phase) {
            case CHARGE:
                if (phaseTime >= CHARGE_TIME) enterFire(ctx);
                break;
            case FIRE:
                if (phaseTime >= FIRE_TIME) enterFade();
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

    private void enterFire(BattleContext ctx) {
        phase = Phase.FIRE;
        phaseTime = 0f;
        applyHit(ctx);
    }

    private void enterFade() {
        phase = Phase.FADE;
        phaseTime = 0f;
        releaseInputLock();
    }

    private void applyHit(BattleContext ctx) {
        if (hitApplied) return;
        hitApplied = true;

        // Beam pierces — every alive enemy on this row past the caster eats the hit.
        for (Enemy target : ctx.enemiesOnRow(row)) {
            if (target.getCol() > originCol) {
                applyEffectsTo(target);
            }
        }
    }

    @Override
    public void render(SpriteBatch batch, BattleContext ctx) {
        if (phase == Phase.DONE) return;

        float scale  = ctx.tileDepthScale(row);
        float panelW = ctx.battlefield.getPanelWidth() * scale;
        float panelH = ctx.battlefield.getPanelRenderHeight() * scale;
        Vector2 startPos = ctx.projectedTileWorld(originCol + 1, row);
        Vector2 endPos   = ctx.projectedTileWorld(Battlefield.COLS - 1, row);
        float startX = startPos.x - panelW * 0.5f;
        float y      = startPos.y - panelH * 0.5f;
        float endX   = endPos.x + panelW * 0.5f;
        float fullW  = endX - startX;
        float h      = panelH;
        float alpha = 1f;
        float w     = fullW;

        switch (phase) {
            case CHARGE:
                // Beam extends outward from the caster during charge-up
                float chargeProgress = Math.min(phaseTime / CHARGE_TIME, 1f);
                w     = fullW * chargeProgress;
                alpha = 0.5f + 0.5f * chargeProgress;
                break;
            case FIRE:
                // Full beam, full alpha — animation loops here
                break;
            case FADE:
                alpha = 1f - (phaseTime / FADE_TIME);
                break;
            default:
                break;
        }

        if (animation != null) {
            TextureRegion frame = animation.getKeyFrame(stateTime, false);
            batch.setColor(1f, 1f, 1f, alpha);
            batch.draw(frame, startX, y, w, h);
            batch.setColor(1f, 1f, 1f, 1f);
        } else {
            sprite.setBounds(startX, y, w, h);
            sprite.setAlpha(alpha);
            sprite.draw(batch);
            sprite.setAlpha(1f);
        }
    }
}
