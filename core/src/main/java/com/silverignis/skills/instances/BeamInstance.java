package com.silverignis.skills.instances;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
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
        combatant.getAnimController().enterCast();

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
        playVfx(this::beamPoint, this::intensity);   // layered effects listed in the skill def
    }

    private void enterFade() {
        phase = Phase.FADE;
        phaseTime = 0f;
        releaseInputLock();
        combatant.getAnimController().enterIdle();
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

    public float intensity() {
        return switch(phase) {
            case FIRE -> 1f;
            case FADE -> 1f - (phaseTime/FADE_TIME);
            default -> 0f;
        };
    }

    private void beamPoint(Vector3 out) {
        int nearCol = originCol + dir;
        int farCol = dir > 0 ? Battlefield.COLS - 1 : 0;
        Battlefield bf = battleContext().battlefield;
        out.set(MathUtils.random(bf.floorX(nearCol), bf.floorX(farCol)), 0f, bf.floorZ(row));
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
                // The stream runs out: the tail sweeps downrange toward the far edge
                // (the near end recedes) while the remainder thins out — no hard cut.
                float fadeProgress = Math.min(phaseTime / FADE_TIME, 1f);
                w     = fullW * (1f - fadeProgress);
                alpha = 1f - fadeProgress * fadeProgress;
                break;
            default:
                break;
        }

        // Anchor at the caster so the beam grows outward toward the far edge —
        // except in FADE, where the far end stays pinned and the tail recedes.
        float drawX;
        if (phase == Phase.FADE) drawX = dir > 0 ? rightX - w : leftX;
        else                     drawX = dir > 0 ? leftX : rightX - w;

        if (animation != null) {
            TextureRegion frame = animation.getKeyFrame(stateTime);
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
