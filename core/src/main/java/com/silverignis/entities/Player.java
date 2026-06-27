package com.silverignis.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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
import com.silverignis.systems.combat.Combatant;
import com.silverignis.systems.combat.StatusContainer;
import com.silverignis.util.InputLock;

public class Player implements Combatant, SceneRenderable {

    private final Battlefield     battlefield;
    private final AnimController  animController;
    private final Caster          caster;
    private final Health          health;
    private final Stats           stats;
    private final StatusContainer statusContainer;
    private final GridMovement    gridMovement;

    public float visualHeight = 0f;

    public Player(int col, int row, AnimSet animSet, Battlefield battlefield, Caster caster, Stats stats) {
        this.battlefield  = battlefield;
        this.gridMovement = new GridMovement(
            new GridPosition(battlefield, col, row),
            new GridBounds(0, Battlefield.COLS / 2 - 1, 0, Battlefield.ROWS - 1));
        this.caster = caster;
        this.stats  = stats;
        this.health = new Health(stats.getVitality());
        this.statusContainer = new StatusContainer(this);
        this.animController = new AnimController(animSet,
            battlefield.tileCenterX(col), battlefield.tileCenterY(row));
    }

    // --- Position (delegated to GridPosition / AnimController) ---

    public GridPosition getGridPosition() { return gridMovement.getPosition(); }

    @Override public GridMovement   getGridMovement()   { return gridMovement; }
    @Override public AnimController getAnimController() { return animController; }

    public int   getCol()        { return gridMovement.getPosition().getCol(); }
    public int   getRow()        { return gridMovement.getPosition().getRow(); }
    public float getVisualX()    { return animController.getRenderX(); }
    public float getVisualY()    { return animController.getRenderY(); }
    public float getDepthScale() { return gridMovement.getPosition().getDepthScale(); }

    /** Per-frame projection push from PlayState. Snaps the visual unless a MOVE is mid-tween. */
    public void setProjectedTarget(float x, float y) {
        if (animController.getState() != AnimState.MOVE) {
            animController.snapTo(x, y);
        }
    }
    public void setDepthScale(float s) { gridMovement.getPosition().setDepthScale(s); }

    // --- Caster role (delegated to Caster) ---

    public Caster     getCaster()      { return caster; }
    public InputLock  getInputLock()   { return caster.getInputLock(); }
    public boolean    isInputLocked()  { return caster.getInputLock().isLocked(); }
    public Team       getTeam()        { return caster.getTeam(); }
    public SkillDeck  getDeck()        { return caster.getDeck(); }
    public SkillSlots getSlots()       { return caster.getSlots(); }
    public Skill      getBasicAttack() { return caster.getBasicAttack(); }

    // --- Entity-proper state ---

    public int    getHp()    { return health.getCurrent(); }
    public int    getMaxHp() { return health.getMax(); }
    public boolean isAlive() { return health.getCurrent() > 0; }
    public boolean isDead()  { return health.getCurrent() <= 0; }

    public Health          getHealth()          { return health; }
    public Stats           getStats()           { return stats; }
    public StatusContainer getStatusContainer() { return statusContainer; }

    public void update(float delta) {
        caster.update(delta);
        animController.update(delta);
    }

    @Override public float       depth() { return battlefield.floorZ(getRow()); }
    @Override public RenderLayer layer() { return RenderLayer.BILLBOARD; }

    @Override
    public void render(RenderContext rc) {
        if (animController.isHurtHidden()) return;
        TextureRegion frame = animController.currentFrame();
        if (frame == null) return;

        float pw = battlefield.panelFloorWidth() * gridMovement.getPosition().getDepthScale();
        float alpha = animController.getRenderAlpha();
        rc.batch.setColor(1f, 1f, 1f, alpha);
        rc.batch.draw(frame,
            animController.getRenderX() - pw * 0.5f,
            animController.getRenderY() + visualHeight,
            pw, pw);
        rc.batch.setColor(Color.WHITE);
    }

    private void renderShadow(SpriteBatch batch, Texture shadowTex) {
        float pw = battlefield.getPanelWidth();
        float ph = battlefield.getPanelRenderHeight();
        float sw = pw * 0.75f;
        float sh = ph * 0.35f;
        batch.setColor(Color.WHITE);
        batch.draw(shadowTex,
            animController.getRenderX() - sw * 0.5f,
            animController.getRenderY(),
            sw, sh);
    }

    public SceneRenderable shadowView(Texture shadowTex) {
        return new SceneRenderable() {
            @Override public float       depth() { return battlefield.floorZ(getRow()); }
            @Override public RenderLayer layer() { return RenderLayer.GROUND; }
            @Override public void render(RenderContext rc) {
                if (isAlive()) renderShadow(rc.batch, shadowTex);
            }
        };
    }
}
