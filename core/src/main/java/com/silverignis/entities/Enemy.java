package com.silverignis.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.silverignis.components.HitFlash;
import com.silverignis.util.PositionSmoother;

public class Enemy implements Collider {

    private static final float BASIC_ATTACK_SPEED = 10f;
    private static final int   BASIC_ATTACK_DAMAGE = 5;

    private static final float MIN_MOVE_INTERVAL = 0.5f;
    private static final float MAX_MOVE_INTERVAL = 1.5f;

    private static final float MOVE_SMOOTH_SPEED = 18f;
    private static final float HITBOX_INSET       = 0.25f;

    private static final Color FREEZE_TINT = new Color(0.5f, 0.7f, 1f, 1f);

    private static final float DEATH_DURATION = 0.5f;

    private int col;
    private int row;
    private final Sprite sprite;
    private final Battlefield battlefield;
    private int hp;

    /** Visual height above the ground plane (world units). Non-zero for jumps/floats. */
    public float visualHeight = 0f;

    private final Rectangle   bounds   = new Rectangle();
    private final HitFlash    hitFlash = new HitFlash();
    private final GlyphLayout hpLayout = new GlyphLayout();
    private float freezeTimer = 0f;
    private float deathTimer  = 0f;

    private float moveTimer;
    private float moveInterval;

    private float timeSinceLastAttack;
    private final float basicAttackCooldown;
    private final Texture basicAttackTexture;

    private final PositionSmoother smoother;

    private float projectedTargetX = Float.NaN;
    private float projectedTargetY = Float.NaN;
    private float depthScale = 1f;

    public Enemy(int col, int row, Sprite sprite, Battlefield battlefield, int hp,
                 Texture basicAttackTexture) {
        this.col = col;
        this.row = row;
        this.sprite = sprite;
        this.battlefield = battlefield;
        this.hp = hp;

        this.basicAttackTexture = basicAttackTexture;
        this.basicAttackCooldown = 1.25f;
        this.timeSinceLastAttack = 0f;
        this.moveInterval = MathUtils.random(MIN_MOVE_INTERVAL, MAX_MOVE_INTERVAL);
        this.moveTimer = 0f;

        this.smoother = new PositionSmoother(MOVE_SMOOTH_SPEED,
                battlefield.tileCenterX(col), battlefield.tileCenterY(row));
    }

    public int    getCol()      { return col; }
    public int    getRow()      { return row; }
    public int    getHp()       { return hp; }
    public void   setHp(int hp) { this.hp = hp; }
    public Sprite getSprite()   { return sprite; }
    public float  getVisualX()  { return smoother.getX(); }
    public float  getVisualY()  { return smoother.getY(); }

    public void setProjectedTarget(float x, float y) { projectedTargetX = x; projectedTargetY = y; }
    public void setDepthScale(float s) { depthScale = s; }
    public float getDepthScale() { return depthScale; }

    public void moveUp()    { row = MathUtils.clamp(row + 1, 0, Battlefield.ROWS - 1); }
    public void moveDown()  { row = MathUtils.clamp(row - 1, 0, Battlefield.ROWS - 1); }
    public void moveLeft()  { col = MathUtils.clamp(col - 1, Battlefield.COLS / 2, Battlefield.COLS - 1); }
    public void moveRight() { col = MathUtils.clamp(col + 1, Battlefield.COLS / 2, Battlefield.COLS - 1); }

    public void takeDamage(int amount) {
        if (amount <= 0 || hp <= 0) return;
        hp = Math.max(0, hp - amount);
        hitFlash.flash();
        if (hp <= 0) deathTimer = DEATH_DURATION;
    }

    public void update(float delta) {
        if (hp <= 0) {
            deathTimer = Math.max(0f, deathTimer - delta);
            return;
        }

        hitFlash.tick(delta);
        freezeTimer = Math.max(0f, freezeTimer - delta);

        if (freezeTimer > 0f) {
            // Frozen: skip movement and attack timers
            float targetX = Float.isNaN(projectedTargetX) ? battlefield.tileCenterX(col) : projectedTargetX;
            float targetY = Float.isNaN(projectedTargetY) ? battlefield.tileCenterY(row) : projectedTargetY;
            smoother.update(delta, targetX, targetY);
            return;
        }

        timeSinceLastAttack += delta;
        moveTimer += delta;

        if (moveTimer >= moveInterval) {
            moveTimer = 0f;
            moveInterval = MathUtils.random(MIN_MOVE_INTERVAL, MAX_MOVE_INTERVAL);
            stepRandomly();
        }

        float targetX = Float.isNaN(projectedTargetX) ? battlefield.tileCenterX(col) : projectedTargetX;
        float targetY = Float.isNaN(projectedTargetY) ? battlefield.tileCenterY(row) : projectedTargetY;
        smoother.update(delta, targetX, targetY);
    }

    public void flash() { hitFlash.flash(); }

    /** Freeze the enemy for the given duration (seconds). Stacks replace. */
    public void applyFreeze(float duration) {
        freezeTimer = Math.max(freezeTimer, duration);
        hitFlash.flash();
    }

    public boolean isFrozen() { return freezeTimer > 0f; }

    /** True while the death fade-out is playing. */
    public boolean isDying() { return hp <= 0 && deathTimer > 0f; }

    /** True once the death animation has finished — safe to remove. */
    public boolean isDead() { return hp <= 0 && deathTimer <= 0f; }

    private void stepRandomly() {
        switch (MathUtils.random(3)) {
            case 0: moveUp();    break;
            case 1: moveDown();  break;
            case 2: moveLeft();  break;
            case 3: moveRight(); break;
        }
    }

    public boolean canBasicAttack() {
        return isAlive() && !isFrozen() && timeSinceLastAttack >= basicAttackCooldown;
    }

    @Override
    public Rectangle getBounds() {
        float pw = battlefield.getPanelWidth() * depthScale;
        float ph = battlefield.getPanelRenderHeight() * depthScale;
        float ix = pw * HITBOX_INSET;
        float iy = ph * HITBOX_INSET;
        return bounds.set(smoother.getX() - pw * 0.5f + ix, smoother.getY() + iy, pw - 2f * ix, ph - 2f * iy);
    }

    @Override
    public Team getTeam() { return Team.ENEMY; }

    @Override
    public boolean isAlive() { return hp > 0; }

    public Projectile attack() {
        timeSinceLastAttack = 0f;

        float panelWidth        = battlefield.getPanelWidth() * depthScale;
        float panelRenderHeight = battlefield.getPanelRenderHeight() * depthScale;

        float spawnX = smoother.getX() - panelWidth * 1.5f;
        float spawnY = smoother.getY();

        Sprite projectileSprite = new Sprite(basicAttackTexture);
        projectileSprite.setSize(panelWidth, panelRenderHeight);
        projectileSprite.setFlip(true, false);

        Vector2 position = new Vector2(spawnX, spawnY);
        Vector2 velocity = new Vector2(-BASIC_ATTACK_SPEED, 0f);

        return new Projectile(position, velocity, projectileSprite, Team.ENEMY, BASIC_ATTACK_DAMAGE);
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
        batch.draw(shadowTex, smoother.getX() - sw * 0.5f, smoother.getY(), sw, sh);
        batch.setColor(Color.WHITE);
    }

    public void render(SpriteBatch batch, BitmapFont font) {
        if (isDead()) return;

        float pw = battlefield.getPanelWidth() * depthScale;
        sprite.setBounds(smoother.getX() - pw * 0.5f, smoother.getY() + visualHeight, pw, pw);

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
        float x = smoother.getX() - hpLayout.width * 0.5f;
        float y = smoother.getY() - 0.05f;
        float alpha = isDying() ? deathTimer / DEATH_DURATION : 1f;
        Color prev = font.getColor().cpy();
        font.setColor(1f, 1f, 1f, alpha);
        font.draw(batch, hpLayout, x, y);
        font.setColor(prev);
    }
}
