package com.silverignis.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.silverignis.animation.AnimController;
import com.silverignis.animation.AnimSet;
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

    private final AnimController  animController;
    private final Caster          caster;
    private final Health          health;
    private final Stats           stats;
    private final StatusContainer statusContainer;
    private final GridMovement    gridMovement;

    public float visualHeight = 0f;

    public Player(int col, int row, AnimSet animSet, Caster caster, Stats stats) {
        this.gridMovement = new GridMovement(
            new GridPosition(col, row),
            new GridBounds(0, Battlefield.COLS / 2 - 1, 0, Battlefield.ROWS - 1));
        this.caster = caster;
        this.stats  = stats;
        this.health = new Health(stats.getVitality());
        this.statusContainer = new StatusContainer(this);
        this.animController = new AnimController(animSet);
    }

    // --- Position (delegated to GridPosition / AnimController) ---

    public GridPosition getGridPosition() { return gridMovement.getPosition(); }

    @Override public GridMovement   getGridMovement()   { return gridMovement; }
    @Override public AnimController getAnimController() { return animController; }

    public int   getCol()       { return gridMovement.getPosition().getCol(); }
    public int   getRow()       { return gridMovement.getPosition().getRow(); }

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
        gridMovement.update(delta);
        animController.update(delta);
    }

    /** Depth-sorts by the *visual* row so a mid-step entity sorts where it looks. */
    @Override public float       depth() { return Battlefield.floorZ(getVisualRow()); }
    @Override public RenderLayer layer() { return RenderLayer.BILLBOARD; }

    @Override
    public void render(RenderContext rc) {
        if (animController.isHurtHidden()) return;
        TextureRegion frame = animController.currentFrame();
        if (frame == null) return;

        Vector2 p = rc.tileWorld(getVisualCol(), getVisualRow());
        float pw = Battlefield.panelFloorWidth() * rc.tileDepthScale(getVisualRow());
        float alpha = animController.getRenderAlpha();
        rc.batch.setColor(1f, 1f, 1f, alpha);
        rc.batch.draw(frame, p.x - pw * 0.5f, p.y + visualHeight, pw, pw);
        rc.batch.setColor(Color.WHITE);
    }

    private void renderShadow(RenderContext rc, Texture shadowTex) {
        Vector2 p = rc.tileWorld(getVisualCol(), getVisualRow());
        float sw = rc.panelWidth() * 0.75f;
        float sh = rc.panelRenderHeight() * 0.35f;
        rc.batch.setColor(Color.WHITE);
        rc.batch.draw(shadowTex, p.x - sw * 0.5f, p.y, sw, sh);
    }

    public SceneRenderable shadowView(Texture shadowTex) {
        return new SceneRenderable() {
            @Override public float       depth() { return Battlefield.floorZ(getVisualRow()); }
            @Override public RenderLayer layer() { return RenderLayer.GROUND; }
            @Override public void render(RenderContext rc) {
                if (isAlive()) renderShadow(rc, shadowTex);
            }
        };
    }
}
