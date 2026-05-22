package com.silverignis.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.silverignis.components.*;
import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillDeck;
import com.silverignis.skills.slots.SkillSlots;
import com.silverignis.systems.BattleContext;
import com.silverignis.systems.combat.Combatant;
import com.silverignis.systems.combat.StatusContainer;
import com.silverignis.systems.combat.StatusType;
import com.silverignis.util.HitFlash;
import com.silverignis.util.InputLock;

public class Enemy implements Combatant {

    private static final float MIN_MOVE_INTERVAL   = 0.5f;
    private static final float MAX_MOVE_INTERVAL   = 1.5f;
    private static final float MIN_ATTACK_INTERVAL = 1.0f;
    private static final float MAX_ATTACK_INTERVAL = 2.0f;
    private static final float MOVE_SMOOTH_SPEED   = 18f;

    private static final Color FREEZE_TINT    = new Color(0.5f, 0.7f, 1f, 1f);
    private static final float DEATH_DURATION = 0.5f;

    private final DirectionalSprite sprites;
    private final Battlefield battlefield;
    private final Health health;
    private final Stats stats;
    private final StatusContainer statusContainer;
    private final GridMovement gridMovement;

    /** Visual height above the ground plane (world units). Non-zero for jumps/floats. */
    public float visualHeight = 0f;

    private final HitFlash     hitFlash     = new HitFlash();
    private final Caster       caster       = new Caster(Team.ENEMY);
    private final GlyphLayout  hpLayout     = new GlyphLayout();

    private float deathTimer  = 0f;

    private float moveTimer;
    private float moveInterval;
    private float attackTimer;
    private float attackInterval;

    public Enemy(int col, int row, DirectionalSprite sprites, Battlefield battlefield, Stats stats) {
        this.sprites      = sprites;
        this.battlefield  = battlefield;
        this.stats = stats;
        this.health = new Health(stats.getVitality());
        this.gridMovement = new GridMovement(
            new GridPosition(battlefield, col, row, MOVE_SMOOTH_SPEED),
            new GridBounds(Battlefield.COLS / 2, Battlefield.COLS - 1, 0, Battlefield.ROWS - 1));
        this.moveInterval   = MathUtils.random(MIN_MOVE_INTERVAL, MAX_MOVE_INTERVAL);
        this.moveTimer      = 0f;
        this.attackInterval = MathUtils.random(MIN_ATTACK_INTERVAL, MAX_ATTACK_INTERVAL);
        this.attackTimer    = 0f;
        this.statusContainer = new StatusContainer(this);
    }


    @Override
    public GridMovement getGridMovement() { return gridMovement; }
    public GridPosition getGridPosition() { return gridMovement.getPosition(); }
    public int          getCol()          { return gridMovement.getPosition().getCol(); }
    public int          getRow()          { return gridMovement.getPosition().getRow(); }
    public float        getVisualX()      { return gridMovement.getPosition().getVisualX(); }
    public float        getVisualY()      { return gridMovement.getPosition().getVisualY(); }
    public float        getDepthScale()   { return gridMovement.getPosition().getDepthScale(); }

    public void setProjectedTarget(float x, float y) { gridMovement.getPosition().setProjectedTarget(x, y); }
    public void setDepthScale(float s)               { gridMovement.getPosition().setDepthScale(s); }


    public Caster     getCaster()       { return caster; }
    public InputLock  getInputLock()    { return caster.getInputLock(); }
    public boolean    isInputLocked()   { return caster.getInputLock().isLocked(); }
    public Team       getTeam()         { return caster.getTeam(); }
    public SkillDeck  getDeck()         { return caster.getDeck(); }
    public SkillSlots getSlots()        { return caster.getSlots(); }
    public Skill      getBasicAttack()  { return caster.getBasicAttack(); }

    // --- Entity-proper state ---

    public int    getHp()       { return this.health.getCurrent(); }
    public Sprite getSprite()   { return sprites.forTeam(getTeam()); }
    public boolean isAlive()    { return this.health.getCurrent() > 0; }

    public boolean isDying() { return this.health.getCurrent() <= 0 && deathTimer > 0f; }

    public boolean isDead() { return this.health.getCurrent() <= 0 && deathTimer <= 0f; }

    public Health          getHealth()          { return health; }
    public Stats           getStats()           { return stats; }
    public StatusContainer getStatusContainer() { return statusContainer; }

    public void onHitFlash(){ hitFlash.flash(); }

    public void onDeath(){
        if (deathTimer <= 0f) deathTimer = DEATH_DURATION;
    }

    public void update(float delta, BattleContext ctx) {
        caster.update(delta);

        if (this.health.getCurrent() <= 0) {
            deathTimer = Math.max(0f, deathTimer - delta);
            gridMovement.getPosition().update(delta);
            return;
        }

        hitFlash.tick(delta);

        if (statusContainer.blocksMovement()) { //check if status blocks movement
            gridMovement.getPosition().update(delta);
            return;
        }

        moveTimer += delta;
        if (moveTimer >= moveInterval) {
            moveTimer = 0f;
            moveInterval = MathUtils.random(MIN_MOVE_INTERVAL, MAX_MOVE_INTERVAL);
            stepRandomly(ctx);
        }

        attackTimer += delta;

        gridMovement.getPosition().update(delta);
    }

    /**
     * AI-side throttle for basic attacks. The skill cooldown gates whether the
     * skill *can* fire; this gates whether the AI *wants* to fire it. Together
     * they reproduce the original 1.0-2.0s attack cadence even when the
     * underlying skill (e.g. {@code wind_slash}) has a much shorter cooldown
     * tuned for player button-mashing.
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
    public void renderShadow(SpriteBatch batch, Texture shadowTex) {
        if (isDead()) return;
        float pw = battlefield.getPanelWidth();
        float ph = battlefield.getPanelRenderHeight();
        float sw = pw * 0.75f;
        float sh = ph * 0.35f;
        float alpha = isDying() ? (deathTimer / DEATH_DURATION) * 0.5f : 0.5f;
        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(shadowTex, gridMovement.getPosition().getVisualX() - sw * 0.5f, gridMovement.getPosition().getVisualY(), sw, sh);
        batch.setColor(Color.WHITE);
    }

    public void render(SpriteBatch batch, BitmapFont font) {
        if (isDead()) return;

        Sprite sprite = sprites.forTeam(getTeam());
        float pw = battlefield.getPanelWidth() * gridMovement.getPosition().getDepthScale();
        sprite.setBounds(
            gridMovement.getPosition().getVisualX() - pw * 0.5f,
            gridMovement.getPosition().getVisualY() + visualHeight,
                pw, pw);

        if (isDying()) {
            float alpha = deathTimer / DEATH_DURATION;
            sprite.setColor(1f, 1f, 1f, alpha);
            sprite.draw(batch);
            sprite.setColor(Color.WHITE);
        } else if (!hitFlash.isHidden()) {
            boolean frozen = statusContainer.has(StatusType.FREEZE);
            if (frozen) sprite.setColor(FREEZE_TINT);
            sprite.draw(batch);
            if (frozen) sprite.setColor(Color.WHITE);
        }



        renderHpLabel(batch, font);
    }

    private void renderHpLabel(SpriteBatch batch, BitmapFont font) {
        hpLayout.setText(font, Integer.toString(this.health.getCurrent()));
        float x = gridMovement.getPosition().getVisualX() - hpLayout.width * 0.5f;
        float y = gridMovement.getPosition().getVisualY() - 0.05f;
        float alpha = isDying() ? deathTimer / DEATH_DURATION : 1f;
        Color prev = font.getColor().cpy();
        font.setColor(1f, 1f, 1f, alpha);
        font.draw(batch, hpLayout, x, y);
        font.setColor(prev);
    }
}
