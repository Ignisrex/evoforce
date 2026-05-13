package com.silverignis.skills.instances;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.silverignis.entities.Enemy;
import com.silverignis.entities.Player;
import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillInstance;
import com.silverignis.systems.BattleContext;

public class ZoneInstance extends SkillInstance {

    private static final float APPEAR_TIME = 0.15f;
    private static final float ACTIVE_TIME = 1.00f;
    private static final float FADE_TIME   = 0.25f;
    private static final float TICK_INTERVAL = 0.33f;

    private enum Phase { APPEAR, ACTIVE, FADE, DONE }

    private Phase phase = Phase.APPEAR;
    private float phaseTime = 0f;
    private float tickTimer = 0f;

    private final Sprite sprite;
    private final int targetCol;
    private final int targetRow;
    private boolean rendersUnder = true;

    public ZoneInstance(Skill def, Player caster) {
        this(def, caster, caster.getCol() + 1, caster.getRow());
    }

    public ZoneInstance(Skill def, Player caster, int targetCol, int targetRow) {
        super(def, caster);
        this.targetCol = targetCol;
        this.targetRow = targetRow;

        this.sprite = new Sprite(def.getVfxTexture());
    }

    public boolean isRenderUnder() { return rendersUnder; }

    @Override
    public void update(float delta, BattleContext ctx) {
        phaseTime += delta;

        switch (phase) {
            case APPEAR:
                if (phaseTime >= APPEAR_TIME) enterActive();
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
        Enemy target = ctx.enemy;
        if (target == null || !target.isAlive()) return;
        if (target.getCol() != targetCol || target.getRow() != targetRow) return;
        applyEffectsTo(target);
    }

    @Override
    public void render(SpriteBatch batch, BattleContext ctx) {
        if (phase == Phase.DONE) return;

        float depthScale = ctx.tileDepthScale(targetRow);
        float panelW = ctx.battlefield.getPanelWidth() * depthScale;
        float panelH = ctx.battlefield.getPanelRenderHeight() * depthScale;
        Vector2 tilePos = ctx.projectedTileWorld(targetCol, targetRow);
        float tileX = tilePos.x;
        float tileY = tilePos.y;

        float scale;
        float alpha;

        switch (phase) {
            case APPEAR:
                // Edges expand inward: start oversized and shrink to tile bounds.
                scale = 1f + 0.3f * (1f - phaseTime / APPEAR_TIME);
                alpha = phaseTime / APPEAR_TIME;
                break;
            case ACTIVE:
                scale = 1f;
                alpha = 0.8f;
                break;
            case FADE:
                scale = 1f;
                alpha = 0.8f * (1f - phaseTime / FADE_TIME);
                break;
            default:
                return;
        }

        float w = panelW * scale;
        float h = panelH * scale;
        float cx = tileX;
        float cy = tileY;

        sprite.setBounds(cx - w * 0.5f, cy - h * 0.5f, w, h);
        sprite.setAlpha(alpha);
        sprite.draw(batch);
        sprite.setAlpha(1f);
    }
}
