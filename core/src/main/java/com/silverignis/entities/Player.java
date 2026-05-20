package com.silverignis.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.silverignis.components.*;
import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillDeck;
import com.silverignis.skills.slots.SkillSlots;
import com.silverignis.systems.combat.Combatant;
import com.silverignis.systems.combat.StatusContainer;
import com.silverignis.util.HitFlash;
import com.silverignis.util.InputLock;

public class Player implements Combatant {

    private static final float MOVE_SMOOTH_SPEED = 18f;

    private final Sprite sprite;
    private final Battlefield battlefield;

    private final HitFlash     hitFlash     = new HitFlash();
    private final Caster       caster       = new Caster(Team.PLAYER);
    private final GridPosition gridPosition;
    private final Health health;
    private final Stats stats;
    private final StatusContainer statusContainer;


    public float visualHeight = 0f;

    public Player(int col, int row, Sprite sprite, Battlefield battlefield, Stats stats) {
        this.sprite       = sprite;
        this.battlefield  = battlefield;
        this.gridPosition = new GridPosition(battlefield, col, row, MOVE_SMOOTH_SPEED);
        this.stats = stats;
        this.health = new Health(this.stats.getVitality());
        this.statusContainer = new StatusContainer(this);

    }

    // --- Position (delegated to GridPosition) ---

    public GridPosition getGridPosition() { return gridPosition; }
    public int          getCol()          { return gridPosition.getCol(); }
    public int          getRow()          { return gridPosition.getRow(); }
    public float        getVisualX()      { return gridPosition.getVisualX(); }
    public float        getVisualY()      { return gridPosition.getVisualY(); }
    public float        getDepthScale()   { return gridPosition.getDepthScale(); }

    public void setProjectedTarget(float x, float y) { gridPosition.setProjectedTarget(x, y); }
    public void setDepthScale(float s)               { gridPosition.setDepthScale(s); }

    // --- Caster role (delegated to Caster) ---

    public Caster     getCaster()       { return caster; }
    public InputLock  getInputLock()    { return caster.getInputLock(); }
    public boolean    isInputLocked()   { return caster.getInputLock().isLocked(); }
    public Team       getTeam()         { return caster.getTeam(); }
    public SkillDeck  getDeck()         { return caster.getDeck(); }
    public SkillSlots getSlots()        { return caster.getSlots(); }
    public Skill      getBasicAttack()  { return caster.getBasicAttack(); }

    // --- Entity-proper state ---

    public int    getHp()         { return this.health.getCurrent(); }
    public int    getMaxHp()      { return this.health.getMax(); }
    public Sprite getSprite()     { return sprite; }
    public boolean isAlive()      { return this.health.getCurrent() > 0; }

    public void moveUp() {
        if (caster.getInputLock().isLocked() || statusContainer.blocksMovement()) return;
        int newRow = MathUtils.clamp(gridPosition.getRow() + 1, 0, Battlefield.ROWS - 1);
        gridPosition.setTile(gridPosition.getCol(), newRow);
    }

    public void moveDown() {
        if (caster.getInputLock().isLocked() || statusContainer.blocksMovement()) return;
        int newRow = MathUtils.clamp(gridPosition.getRow() - 1, 0, Battlefield.ROWS - 1);
        gridPosition.setTile(gridPosition.getCol(), newRow);
    }

    public void moveLeft() {
        if (caster.getInputLock().isLocked() || statusContainer.blocksMovement()) return;
        int newCol = MathUtils.clamp(gridPosition.getCol() - 1, 0, Battlefield.COLS / 2 - 1);
        gridPosition.setTile(newCol, gridPosition.getRow());
    }

    public void moveRight() {
        if (caster.getInputLock().isLocked() || statusContainer.blocksMovement()) return;
        int newCol = MathUtils.clamp(gridPosition.getCol() + 1, 0, Battlefield.COLS / 2 - 1);
        gridPosition.setTile(newCol, gridPosition.getRow());
    }

    /**
     * Snap to an arbitrary grid cell, bypassing the half-grid clamp. Used by
     * {@code StrikeInstance} to drive the body across the whole battlefield
     * during HIT phase. The visual smoother still tweens, so the dash looks smooth.
     */
    public void forceSetTile(int newCol, int newRow) {
        gridPosition.setTile(
                MathUtils.clamp(newCol, 0, Battlefield.COLS - 1),
                MathUtils.clamp(newRow, 0, Battlefield.ROWS - 1));
    }

    public void update(float delta) {
        caster.update(delta);
        gridPosition.update(delta);
        hitFlash.tick(delta);
    }

    public void renderShadow(SpriteBatch batch, Texture shadowTex) {
        float pw = battlefield.getPanelWidth();
        float ph = battlefield.getPanelRenderHeight();
        float sw = pw * 0.75f;
        float sh = ph * 0.35f;
        batch.setColor(Color.WHITE);
        batch.draw(shadowTex, gridPosition.getVisualX() - sw * 0.5f, gridPosition.getVisualY(), sw, sh);
    }

    public void render(SpriteBatch batch) {
        if (hitFlash.isHidden()) return;
        float pw = battlefield.getPanelWidth() * gridPosition.getDepthScale();
        sprite.setBounds(
                gridPosition.getVisualX() - pw * 0.5f,
                gridPosition.getVisualY() + visualHeight,
                pw, pw);
        sprite.draw(batch);
    }

    public Health          getHealth()          { return health; }
    public Stats           getStats()           { return stats; }
    public StatusContainer getStatusContainer() { return statusContainer; }
    public boolean         isDead()             { return health.getCurrent() <= 0; }

    public void onHitFlash() { hitFlash.flash();}

    public void onDeath(){}
}
