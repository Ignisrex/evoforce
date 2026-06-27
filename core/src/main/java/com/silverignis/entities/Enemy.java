package com.silverignis.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.silverignis.animation.AnimController;
import com.silverignis.animation.AnimSet;
import com.silverignis.animation.AnimState;
import com.silverignis.components.*;
import com.silverignis.render.RenderContext;
import com.silverignis.render.RenderLayer;
import com.silverignis.render.SceneRenderable;
import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillDeck;
import com.silverignis.skills.slots.SkillSlots;
import com.silverignis.systems.BattleContext;
import com.silverignis.systems.combat.Combatant;
import com.silverignis.systems.combat.StatusContainer;
import com.silverignis.systems.combat.StatusType;
import com.silverignis.util.InputLock;

import java.util.List;

public class Enemy implements Combatant, SceneRenderable {

    private static final float MIN_MOVE_INTERVAL   = 0.5f;
    private static final float MAX_MOVE_INTERVAL   = 1.5f;
    private static final float MIN_ATTACK_INTERVAL = 1.0f;
    private static final float MAX_ATTACK_INTERVAL = 2.0f;

    private static final Color FREEZE_TINT = new Color(0.5f, 0.7f, 1f, 1f);

    private final Battlefield     battlefield;
    private final AnimController  animController;
    private final Health          health;
    private final Stats           stats;
    private final StatusContainer statusContainer;
    private final GridMovement    gridMovement;

    /** Visual height above the ground plane (world units). Non-zero for jumps/floats. */
    public float visualHeight = 0f;

    private final Caster      caster   = new Caster(Team.ENEMY);
    private final GlyphLayout hpLayout = new GlyphLayout();

    private float moveTimer;
    private float moveInterval;
    private float attackTimer;
    private float attackInterval;

    public Enemy(int col, int row, AnimSet animSet, Battlefield battlefield, Stats stats) {
        this.battlefield = battlefield;
        this.stats  = stats;
        this.health = new Health(stats.getVitality());
        this.gridMovement = new GridMovement(
            new GridPosition(battlefield, col, row),
            new GridBounds(Battlefield.COLS / 2, Battlefield.COLS - 1, 0, Battlefield.ROWS - 1));
        this.moveInterval   = MathUtils.random(MIN_MOVE_INTERVAL, MAX_MOVE_INTERVAL);
        this.attackInterval = MathUtils.random(MIN_ATTACK_INTERVAL, MAX_ATTACK_INTERVAL);
        this.statusContainer = new StatusContainer(this);
        this.animController = new AnimController(animSet,
            battlefield.tileCenterX(col), battlefield.tileCenterY(row));
    }

    @Override public GridMovement   getGridMovement()   { return gridMovement; }
    @Override public AnimController getAnimController() { return animController; }
    public GridPosition getGridPosition() { return gridMovement.getPosition(); }

    public int   getCol()        { return gridMovement.getPosition().getCol(); }
    public int   getRow()        { return gridMovement.getPosition().getRow(); }
    public float getVisualX()    { return animController.getRenderX(); }
    public float getVisualY()    { return animController.getRenderY(); }
    public float getDepthScale() { return gridMovement.getPosition().getDepthScale(); }

    public void setProjectedTarget(float x, float y) {
        if (animController.getState() != AnimState.MOVE) {
            animController.snapTo(x, y);
        }
    }
    public void setDepthScale(float s) { gridMovement.getPosition().setDepthScale(s); }

    public void setupSkills(List<Skill> skills, Skill basicAttack) {
        for (Skill s : skills) caster.getDeck().add(s);
        caster.setBasicAttack(basicAttack);
    }

    public Caster     getCaster()      { return caster; }
    public InputLock  getInputLock()   { return caster.getInputLock(); }
    public boolean    isInputLocked()  { return caster.getInputLock().isLocked(); }
    public Team       getTeam()        { return caster.getTeam(); }
    public SkillDeck  getDeck()        { return caster.getDeck(); }
    public SkillSlots getSlots()       { return caster.getSlots(); }
    public Skill      getBasicAttack() { return caster.getBasicAttack(); }

    // --- Entity-proper state ---

    public int     getHp()      { return health.getCurrent(); }
    public boolean isAlive()    { return health.getCurrent() > 0; }
    public boolean isDying()    { return health.getCurrent() <= 0 && !animController.isDeathComplete(); }
    public boolean isDead()     { return health.getCurrent() <= 0 && animController.isDeathComplete(); }

    public Health          getHealth()          { return health; }
    public Stats           getStats()           { return stats; }
    public StatusContainer getStatusContainer() { return statusContainer; }

    public void update(float delta, BattleContext ctx) {
        caster.update(delta);
        animController.update(delta);

        if (!isAlive()) return;
        if (statusContainer.blocksMovement()) return;

        moveTimer += delta;
        if (moveTimer >= moveInterval) {
            moveTimer = 0f;
            moveInterval = MathUtils.random(MIN_MOVE_INTERVAL, MAX_MOVE_INTERVAL);
            stepRandomly(ctx);
        }

        attackTimer += delta;
    }

    /**
     * AI-side throttle for basic attacks. The skill cooldown gates whether the
     * skill *can* fire; this gates whether the AI *wants* to fire it.
     */
    public boolean wantsToBasicAttack() {
        return isAlive() && !statusContainer.blocksAttack() && attackTimer >= attackInterval;
    }

    /** Reset the AI attack throttle after a successful basic attack. */
    public void onBasicAttackFired() {
        attackTimer = 0f;
        attackInterval = MathUtils.random(MIN_ATTACK_INTERVAL, MAX_ATTACK_INTERVAL);
    }

    private void stepRandomly(BattleContext ctx) {
        ctx.movementSystem.tryGridStep(this, Direction.values()[MathUtils.random(3)]);
    }

    /** Shadow ellipse drawn at ground position before the sprite pass. */
    private void renderShadow(SpriteBatch batch, Texture shadowTex) {
        if (isDead()) return;
        float pw = battlefield.getPanelWidth();
        float ph = battlefield.getPanelRenderHeight();
        float sw = pw * 0.75f;
        float sh = ph * 0.35f;
        float alpha = isDying() ? animController.getRenderAlpha() * 0.5f : 0.5f;
        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(shadowTex,
            animController.getRenderX() - sw * 0.5f,
            animController.getRenderY(),
            sw, sh);
        batch.setColor(Color.WHITE);
    }

    @Override
    public void render(RenderContext rc) {
        if (isDead()) return;
        if (animController.isHurtHidden()) return;
        TextureRegion frame = animController.currentFrame();
        if (frame == null) return;

        float pw = battlefield.getPanelWidth() * gridMovement.getPosition().getDepthScale();
        float alpha = animController.getRenderAlpha();
        boolean frozen = statusContainer.has(StatusType.FREEZE);
        if (frozen) rc.batch.setColor(FREEZE_TINT.r, FREEZE_TINT.g, FREEZE_TINT.b, alpha);
        else        rc.batch.setColor(1f, 1f, 1f, alpha);
        rc.batch.draw(frame,
            animController.getRenderX() - pw * 0.5f,
            animController.getRenderY() + visualHeight,
            pw, pw);
        rc.batch.setColor(Color.WHITE);
    }

    private void renderHpLabel(SpriteBatch batch, BitmapFont font) {
        hpLayout.setText(font, Integer.toString(health.getCurrent()));
        float x = animController.getRenderX() - hpLayout.width * 0.5f;
        float y = animController.getRenderY() - 0.05f;
        float alpha = isDying() ? animController.getRenderAlpha() : 1f;
        Color prev = font.getColor().cpy();
        font.setColor(1f, 1f, 1f, alpha);
        font.draw(batch, hpLayout, x, y);
        font.setColor(prev);
    }

    @Override public float       depth() { return battlefield.floorZ(getRow()); }
    @Override public RenderLayer layer() { return RenderLayer.BILLBOARD; }

    public SceneRenderable shadowView(Texture shadowTex) {
        return new SceneRenderable() {
            @Override public float       depth() { return battlefield.floorZ(getRow()); }
            @Override public RenderLayer layer() { return RenderLayer.GROUND; }
            @Override public void render(RenderContext rc) { renderShadow(rc.batch, shadowTex); }
        };
    }

    public SceneRenderable hpLabelView() {
        return new SceneRenderable() {
            @Override public float       depth() { return battlefield.floorZ(getRow()); }
            @Override public RenderLayer layer() { return RenderLayer.OVERLAY; }
            @Override public void render(RenderContext rc) {
                if (isDead()) return;
                renderHpLabel(rc.batch, rc.font);
            }
        };
    }
}
