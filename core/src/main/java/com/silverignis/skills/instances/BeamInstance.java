package com.silverignis.skills.instances;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.silverignis.components.Team;
import com.silverignis.entities.Battlefield;
import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillInstance;
import com.silverignis.systems.BattleContext;
import com.silverignis.systems.combat.Combatant;

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
    /** +1 = caster faces east (player), -1 = faces west (enemy). Matches ProjectileInstance. */
    private final int dir;

    private final Color tint;

    public BeamInstance(Skill def, Combatant combatant, BattleContext ctx) {
        super(def, combatant, ctx);
        this.row = originRow;
        this.dir = combatant.getTeam() == Team.PLAYER ? 1 : -1;
        acquireInputLock();

        this.animation = def.getVfxAnimation();
        this.sprite    = new Sprite(def.getVfxTexture());
        this.tint      = def.getVfxTint() != null ? def.getVfxTint() : Color.WHITE;
        sprite.setColor(tint);
    }

    @Override
    public void update(float delta) {
        phaseTime += delta;
        stateTime += delta;

        switch (phase) {
            case CHARGE:
                if (phaseTime >= CHARGE_TIME) enterFire(battleContext());
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

        for(Combatant target: ctx.opposingOnRow(combatant, row)){
            // Only targets ahead of the caster in the beam's facing direction.
            if ((target.getCol() - originCol) * dir > 0){
                applyEffectsTo(target);
            }
        }
    }

    @Override
    public void render(SpriteBatch batch, BattleContext ctx) {
        if (phase == Phase.DONE) return;

        // Beam spans from the tile just ahead of the caster to the far grid edge
        // in the caster's facing direction. If the caster sits on the edge it
        // faces (player at col 7, enemy at col 0) there's no room ahead — skip.
        int nearCol = originCol + dir;
        if (nearCol < 0 || nearCol >= Battlefield.COLS) return;
        int farCol = dir > 0 ? Battlefield.COLS - 1 : 0;

        float scale  = ctx.tileDepthScale(row);
        float panelW = ctx.battlefield.getPanelWidth() * scale;
        float panelH = ctx.battlefield.getPanelRenderHeight() * scale;
        Vector2 nearPos = ctx.projectedTileWorld(nearCol, row);
        Vector2 farPos  = ctx.projectedTileWorld(farCol, row);
        float leftX  = Math.min(nearPos.x, farPos.x) - panelW * 0.5f;
        float rightX = Math.max(nearPos.x, farPos.x) + panelW * 0.5f;
        float y      = nearPos.y - panelH * 0.5f;
        float fullW  = rightX - leftX;
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

        // Anchor at the caster so the beam grows outward toward the far edge.
        float drawX = dir > 0 ? leftX : rightX - w;

        if (animation != null) {
            TextureRegion frame = animation.getKeyFrame(stateTime, false);
            batch.setColor(tint.r, tint.g, tint.b, tint.a * alpha);
            if (dir < 0) batch.draw(frame, drawX + w, y, -w, h); // mirror for westward beams
            else         batch.draw(frame, drawX, y, w, h);
            batch.setColor(1f, 1f, 1f, 1f);
        } else {
            sprite.setFlip(dir < 0, false);
            sprite.setBounds(drawX, y, w, h);
            sprite.setAlpha(alpha);
            sprite.draw(batch);
            sprite.setAlpha(1f);
        }
    }
}
