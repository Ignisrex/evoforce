package com.silverignis.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.silverignis.components.Caster;
import com.silverignis.components.GridPosition;
import com.silverignis.components.Team;
import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillDeck;
import com.silverignis.skills.slots.SkillSlots;
import com.silverignis.util.HitFlash;
import com.silverignis.util.InputLock;

public class Enemy {

    private static final float MIN_MOVE_INTERVAL   = 0.5f;
    private static final float MAX_MOVE_INTERVAL   = 1.5f;
    private static final float MIN_ATTACK_INTERVAL = 1.0f;
    private static final float MAX_ATTACK_INTERVAL = 2.0f;
    private static final float MOVE_SMOOTH_SPEED   = 18f;

    private static final Color FREEZE_TINT    = new Color(0.5f, 0.7f, 1f, 1f);
    private static final float DEATH_DURATION = 0.5f;

    private final Sprite sprite;
    private final Battlefield battlefield;
    private int hp;

    /** Visual height above the ground plane (world units). Non-zero for jumps/floats. */
    public float visualHeight = 0f;

    private final HitFlash     hitFlash     = new HitFlash();
    private final Caster       caster       = new Caster(Team.ENEMY);
    private final GridPosition gridPosition;
    private final GlyphLayout  hpLayout     = new GlyphLayout();

    private float freezeTimer = 0f;
    private float deathTimer  = 0f;

    private float moveTimer;
    private float moveInterval;
    private float attackTimer;
    private float attackInterval;

    public Enemy(int col, int row, Sprite sprite, Battlefield battlefield, int hp) {
        this.sprite       = sprite;
        this.battlefield  = battlefield;
        this.hp           = hp;
        this.gridPosition = new GridPosition(battlefield, col, row, MOVE_SMOOTH_SPEED);

        this.moveInterval   = MathUtils.random(MIN_MOVE_INTERVAL, MAX_MOVE_INTERVAL);
        this.moveTimer      = 0f;
        this.attackInterval = MathUtils.random(MIN_ATTACK_INTERVAL, MAX_ATTACK_INTERVAL);
        this.attackTimer    = 0f;
    }



    public GridPosition getGridPosition() { return gridPosition; }
    public int          getCol()          { return gridPosition.getCol(); }
    public int          getRow()          { return gridPosition.getRow(); }
    public float        getVisualX()      { return gridPosition.getVisualX(); }
    public float        getVisualY()      { return gridPosition.getVisualY(); }
    public float        getDepthScale()   { return gridPosition.getDepthScale(); }

    public void setProjectedTarget(float x, float y) { gridPosition.setProjectedTarget(x, y); }
    public void setDepthScale(float s)               { gridPosition.setDepthScale(s); }
    

    public Caster     getCaster()       { return caster; }
    public InputLock  getInputLock()    { return caster.getInputLock(); }
    public boolean    isInputLocked()   { return caster.getInputLock().isLocked(); }
    public Team       getTeam()         { return caster.getTeam(); }
    public SkillDeck  getDeck()         { return caster.getDeck(); }
    public SkillSlots getSlots()        { return caster.getSlots(); }
    public Skill      getBasicAttack()  { return caster.getBasicAttack(); }

    // --- Entity-proper state ---

    public int    getHp()       { return hp; }
    public void   setHp(int hp) { this.hp = hp; }
    public Sprite getSprite()   { return sprite; }
    public boolean isAlive()    { return hp > 0; }

    public void moveUp() {
        int newRow = MathUtils.clamp(gridPosition.getRow() + 1, 0, Battlefield.ROWS - 1);
        gridPosition.setTile(gridPosition.getCol(), newRow);
    }

    public void moveDown() {
        int newRow = MathUtils.clamp(gridPosition.getRow() - 1, 0, Battlefield.ROWS - 1);
        gridPosition.setTile(gridPosition.getCol(), newRow);
    }

    public void moveLeft() {
        int newCol = MathUtils.clamp(gridPosition.getCol() - 1, Battlefield.COLS / 2, Battlefield.COLS - 1);
        gridPosition.setTile(newCol, gridPosition.getRow());
    }

    public void moveRight() {
        int newCol = MathUtils.clamp(gridPosition.getCol() + 1, Battlefield.COLS / 2, Battlefield.COLS - 1);
        gridPosition.setTile(newCol, gridPosition.getRow());
    }

    public void takeDamage(int amount) {
        if (amount <= 0 || hp <= 0) return;
        hp = Math.max(0, hp - amount);
        hitFlash.flash();
        if (hp <= 0) deathTimer = DEATH_DURATION;
    }

    /** Freeze the enemy for the given duration (seconds). Stacks replace. */
    public void applyFreeze(float duration) {
        freezeTimer = Math.max(freezeTimer, duration);
        hitFlash.flash();
    }

    public boolean isFrozen() { return freezeTimer > 0f; }

    public boolean isDying() { return hp <= 0 && deathTimer > 0f; }

    public boolean isDead() { return hp <= 0 && deathTimer <= 0f; }

    public void flash() { hitFlash.flash(); }

    public void update(float delta) {
        caster.update(delta);

        if (hp <= 0) {
            deathTimer = Math.max(0f, deathTimer - delta);
            gridPosition.update(delta);
            return;
        }

        hitFlash.tick(delta);
        freezeTimer = Math.max(0f, freezeTimer - delta);

        if (freezeTimer > 0f) {
            // Frozen: skip movement timer; still tick the smoother so any in-flight tween settles.
            gridPosition.update(delta);
            return;
        }

        moveTimer += delta;
        if (moveTimer >= moveInterval) {
            moveTimer = 0f;
            moveInterval = MathUtils.random(MIN_MOVE_INTERVAL, MAX_MOVE_INTERVAL);
            stepRandomly();
        }

        attackTimer += delta;

        gridPosition.update(delta);
    }

    /**
     * AI-side throttle for basic attacks. The skill cooldown gates whether the
     * skill *can* fire; this gates whether the AI *wants* to fire it. Together
     * they reproduce the original 1.0-2.0s attack cadence even when the
     * underlying skill (e.g. {@code wind_slash}) has a much shorter cooldown
     * tuned for player button-mashing.
     */
    public boolean wantsToBasicAttack() {
        return isAlive() && !isFrozen() && attackTimer >= attackInterval;
    }

    /** Reset the AI attack throttle after a successful basic attack. */
    public void onBasicAttackFired() {
        attackTimer = 0f;
        attackInterval = MathUtils.random(MIN_ATTACK_INTERVAL, MAX_ATTACK_INTERVAL);
    }

    private void stepRandomly() {
        switch (MathUtils.random(3)) {
            case 0: moveUp();    break;
            case 1: moveDown();  break;
            case 2: moveLeft();  break;
            case 3: moveRight(); break;
        }
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
        batch.draw(shadowTex, gridPosition.getVisualX() - sw * 0.5f, gridPosition.getVisualY(), sw, sh);
        batch.setColor(Color.WHITE);
    }

    public void render(SpriteBatch batch, BitmapFont font) {
        if (isDead()) return;

        float pw = battlefield.getPanelWidth() * gridPosition.getDepthScale();
        sprite.setBounds(
                gridPosition.getVisualX() - pw * 0.5f,
                gridPosition.getVisualY() + visualHeight,
                pw, pw);

        if (isDying()) {
            float alpha = deathTimer / DEATH_DURATION;
            sprite.setColor(1f, 1f, 1f, alpha);
            sprite.draw(batch);
            sprite.setColor(Color.WHITE);
        } else if (!hitFlash.isHidden()) {
            if (freezeTimer > 0f) sprite.setColor(FREEZE_TINT);
            sprite.draw(batch);
            if (freezeTimer > 0f) sprite.setColor(Color.WHITE);
        }

        renderHpLabel(batch, font);
    }

    private void renderHpLabel(SpriteBatch batch, BitmapFont font) {
        hpLayout.setText(font, Integer.toString(Math.max(0, hp)));
        float x = gridPosition.getVisualX() - hpLayout.width * 0.5f;
        float y = gridPosition.getVisualY() - 0.05f;
        float alpha = isDying() ? deathTimer / DEATH_DURATION : 1f;
        Color prev = font.getColor().cpy();
        font.setColor(1f, 1f, 1f, alpha);
        font.draw(batch, hpLayout, x, y);
        font.setColor(prev);
    }
}
