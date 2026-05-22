package com.silverignis.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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
    private final Health health;
    private final Stats stats;
    private final StatusContainer statusContainer;
    private final GridMovement gridMovement;


    public float visualHeight = 0f;

    public Player(int col, int row, Sprite sprite, Battlefield battlefield, Stats stats) {
        this.sprite       = sprite;
        this.battlefield  = battlefield;
        this.gridMovement = new GridMovement(
            new GridPosition(battlefield, col, row, MOVE_SMOOTH_SPEED),
            new GridBounds(0, Battlefield.COLS / 2 - 1, 0, Battlefield.ROWS - 1));
        this.stats = stats;
        this.health = new Health(this.stats.getVitality());
        this.statusContainer = new StatusContainer(this);

    }

    // --- Position (delegated to GridPosition) ---

    public GridPosition getGridPosition() { return gridMovement.getPosition(); }

    @Override
    public GridMovement getGridMovement() { return gridMovement; }

    public int          getCol()          { return gridMovement.getPosition().getCol(); }
    public int          getRow()          { return gridMovement.getPosition().getRow(); }
    public float        getVisualX()      { return gridMovement.getPosition().getVisualX(); }
    public float        getVisualY()      { return gridMovement.getPosition().getVisualY(); }
    public float        getDepthScale()   { return gridMovement.getPosition().getDepthScale(); }

    public void setProjectedTarget(float x, float y) { gridMovement.getPosition().setProjectedTarget(x, y); }
    public void setDepthScale(float s)               { gridMovement.getPosition().setDepthScale(s); }

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

    public void update(float delta) {
        caster.update(delta);
        gridMovement.getPosition().update(delta);
        hitFlash.tick(delta);
    }

    public void renderShadow(SpriteBatch batch, Texture shadowTex) {
        float pw = battlefield.getPanelWidth();
        float ph = battlefield.getPanelRenderHeight();
        float sw = pw * 0.75f;
        float sh = ph * 0.35f;
        batch.setColor(Color.WHITE);
        batch.draw(shadowTex, gridMovement.getPosition().getVisualX() - sw * 0.5f, gridMovement.getPosition().getVisualY(), sw, sh);
    }

    public void render(SpriteBatch batch) {
        if (hitFlash.isHidden()) return;
        float pw = battlefield.getPanelWidth() * gridMovement.getPosition().getDepthScale();
        sprite.setBounds(
            gridMovement.getPosition().getVisualX() - pw * 0.5f,
            gridMovement.getPosition().getVisualY() + visualHeight,
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
