package com.silverignis.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.silverignis.components.HitFlash;
import com.silverignis.components.InputLock;
import com.silverignis.util.PositionSmoother;

public class Player implements Collider {

    private static final float BASIC_ATTACK_SPEED = 12f;
    private static final float MOVE_SMOOTH_SPEED  = 18f;
    private static final float HITBOX_INSET       = 0.25f;

    private int col;
    private int row;
    private final Sprite sprite;
    private final Battlefield battlefield;
    private int hp;

    private final InputLock inputLock = new InputLock();
    private final HitFlash  hitFlash  = new HitFlash();

    public float visualHeight = 0f;

    /** Reused per call to avoid per-frame allocations. */
    private final Rectangle bounds = new Rectangle();

    private float timeSinceLastAttack;
    private float basicAttackCooldown;
    private Texture basicAttackTexture;

    private final PositionSmoother smoother;

    private float projectedTargetX = Float.NaN;
    private float projectedTargetY = Float.NaN;
    private float depthScale = 1f;

    public Player(int col, int row, Sprite sprite, Battlefield battlefield, int hp,
                  Texture basicAttackTexture) {
        this.col = col;
        this.row = row;
        this.sprite = sprite;
        this.battlefield = battlefield;
        this.hp = hp;
        this.basicAttackCooldown = 0.5f;
        this.timeSinceLastAttack = basicAttackCooldown; // ready to fire immediately
        this.basicAttackTexture = basicAttackTexture;

        this.smoother = new PositionSmoother(MOVE_SMOOTH_SPEED,
                battlefield.tileCenterX(col), battlefield.tileCenterY(row));
    }

    // --- Getters / Setters ---

    public int getCol()             { return col; }
    public int getRow()             { return row; }
    public int getHp()              { return hp; }
    public void setHp(int hp)       { this.hp = hp; }
    public Sprite getSprite()       { return sprite; }
    public InputLock  getInputLock()  { return inputLock; }
    public boolean    isInputLocked() { return inputLock.isLocked();}
    public float      getVisualX()    { return smoother.getX(); }
    public float      getVisualY()    { return smoother.getY(); }

    public void setProjectedTarget(float x, float y) { projectedTargetX = x; projectedTargetY = y; }
    public void setDepthScale(float s) { depthScale = s; }

        public void moveUp() {
        if(inputLock.isLocked()) return;
        row = MathUtils.clamp(row + 1, 0, Battlefield.ROWS - 1);
    }

    public void moveDown() {
        if(inputLock.isLocked()) return;
        row = MathUtils.clamp(row - 1, 0, Battlefield.ROWS -1);
    }

    public void moveLeft() {
        if(inputLock.isLocked()) return;
        col = MathUtils.clamp(col - 1, 0,Battlefield.COLS / 2 -1);
    }

    public void moveRight() {
        if(inputLock.isLocked()) return;
        col = MathUtils.clamp(col + 1, 0, Battlefield.COLS / 2 - 1);
    }

    /**
     * Snap to an arbitrary grid cell, bypassing the half-grid clamp.
     * Intended for skill instances that want to drive the body across the
     * whole battlefield (e.g. a Strike's forward dash into enemy territory).
     * The visual smoother still tweens, so the dash looks smooth.
     */
    public void forceSetTile(int newCol, int newRow) {
        this.col = MathUtils.clamp(newCol, 0, Battlefield.COLS - 1);
        this.row = MathUtils.clamp(newRow, 0, Battlefield.ROWS - 1);
    }

    public void takeDamage(int amount) {
        if (amount <= 0 || hp <= 0) return;
        hp = Math.max(0, hp - amount);
        hitFlash.flash();
    }

    public void renderShadow(SpriteBatch batch, Texture shadowTex) {
        float pw = battlefield.getPanelWidth();
        float ph = battlefield.getPanelRenderHeight();
        float sw = pw * 0.75f;
        float sh = ph * 0.35f;
        batch.setColor(Color.WHITE);
        batch.draw(shadowTex, smoother.getX() - sw * 0.5f, smoother.getY(), sw, sh);
    }

    public void render(SpriteBatch batch) {
        if (hitFlash.isHidden()) return;
        float pw = battlefield.getPanelWidth() * depthScale;
        sprite.setBounds(smoother.getX() - pw * 0.5f, smoother.getY() + visualHeight, pw, pw);
        sprite.draw(batch);
    }

    public void update(float delta) {
        timeSinceLastAttack += delta;
        hitFlash.tick(delta);
        float targetX = Float.isNaN(projectedTargetX) ? battlefield.tileCenterX(col) : projectedTargetX;
        float targetY = Float.isNaN(projectedTargetY) ? battlefield.tileCenterY(row) : projectedTargetY;
        smoother.update(delta, targetX, targetY);
    }

    public boolean canBasicAttack(){
        return !inputLock.isLocked() && timeSinceLastAttack >= basicAttackCooldown;
    }

    public void flash() { hitFlash.flash(); }

    @Override
    public Rectangle getBounds() {
        float pw = battlefield.getPanelWidth() * depthScale;
        float ph = battlefield.getPanelRenderHeight() * depthScale;
        float ix = pw * HITBOX_INSET;
        float iy = ph * HITBOX_INSET;
        return bounds.set(smoother.getX() - pw * 0.5f + ix, smoother.getY() + iy, pw - 2f * ix, ph - 2f * iy);
    }

    @Override
    public Team getTeam() { return Team.PLAYER; }

    @Override
    public boolean isAlive() { return hp > 0; }

    public Projectile attack(){
        timeSinceLastAttack = 0f;

        float panelWidth  = battlefield.getPanelWidth() * depthScale;
        float panelHeight = battlefield.getPanelRenderHeight() * depthScale;

        float spawnX = smoother.getX() + panelWidth * 0.5f;
        float spawnY = smoother.getY();

        Sprite projectileSprite = new Sprite(basicAttackTexture);
        projectileSprite.setSize(panelWidth, panelHeight);

        Vector2 position = new Vector2(spawnX, spawnY);
        Vector2 velocity = new Vector2(BASIC_ATTACK_SPEED, 0f);

        return new Projectile(position, velocity, projectileSprite, Team.PLAYER);
    }
}
