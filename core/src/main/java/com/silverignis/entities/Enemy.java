package com.silverignis.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
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
import com.silverignis.systems.combat.StatusType;
import com.silverignis.util.InputLock;

import java.util.List;

public class Enemy implements Combatant, SceneRenderable {

    private static final Color FREEZE_TINT = new Color(0.5f, 0.7f, 1f, 1f);

    private final AnimController  animController;
    private final Health          health;
    private final Stats           stats;
    private final StatusContainer statusContainer;
    private final GridMovement    gridMovement;

    /** Visual height above the ground plane (world units). Non-zero for jumps/floats. */
    public float visualHeight = 0f;

    private final Caster      caster   = new Caster(Team.ENEMY);
    private final GlyphLayout hpLayout = new GlyphLayout();

    public Enemy(int col, int row, AnimSet animSet, Stats stats) {
        this.stats  = stats;
        this.health = new Health(stats.getVitality());
        this.gridMovement = new GridMovement(
            new GridPosition(col, row),
            new GridBounds(Battlefield.COLS / 2, Battlefield.COLS - 1, 0, Battlefield.ROWS - 1));
        this.statusContainer = new StatusContainer(this);
        this.animController = new AnimController(animSet, col, row);
    }

    @Override public GridMovement   getGridMovement()   { return gridMovement; }
    @Override public AnimController getAnimController() { return animController; }
    public GridPosition getGridPosition() { return gridMovement.getPosition(); }

    public int   getCol()       { return gridMovement.getPosition().getCol(); }
    public int   getRow()       { return gridMovement.getPosition().getRow(); }
    public float getVisualCol() { return animController.getVisualCol(); }
    public float getVisualRow() { return animController.getVisualRow(); }

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

    /** Same shape as Player.update — deciding what this enemy does is EnemyAi's job. */
    public void update(float delta) {
        caster.update(delta);
        animController.update(delta);
    }

    /** Shadow ellipse drawn at ground position before the sprite pass. */
    private void renderShadow(RenderContext rc, Texture shadowTex) {
        if (isDead()) return;
        Vector2 p = rc.tileWorld(getVisualCol(), getVisualRow());
        float sw = rc.panelWidth() * 0.75f;
        float sh = rc.panelRenderHeight() * 0.35f;
        float alpha = isDying() ? animController.getRenderAlpha() * 0.5f : 0.5f;
        rc.batch.setColor(1f, 1f, 1f, alpha);
        rc.batch.draw(shadowTex, p.x - sw * 0.5f, p.y, sw, sh);
        rc.batch.setColor(Color.WHITE);
    }

    @Override
    public void render(RenderContext rc) {
        if (isDead()) return;
        if (animController.isHurtHidden()) return;
        TextureRegion frame = animController.currentFrame();
        if (frame == null) return;

        Vector2 p = rc.tileWorld(getVisualCol(), getVisualRow());
        float pw = rc.panelWidth() * rc.tileDepthScale(getVisualRow());
        float alpha = animController.getRenderAlpha();
        boolean frozen = statusContainer.has(StatusType.FREEZE);
        if (frozen) rc.batch.setColor(FREEZE_TINT.r, FREEZE_TINT.g, FREEZE_TINT.b, alpha);
        else        rc.batch.setColor(1f, 1f, 1f, alpha);
        rc.batch.draw(frame, p.x - pw * 0.5f, p.y + visualHeight, pw, pw);
        rc.batch.setColor(Color.WHITE);
    }

    private void renderHpLabel(RenderContext rc) {
        BitmapFont font = rc.font;
        hpLayout.setText(font, Integer.toString(health.getCurrent()));
        Vector2 p = rc.tileWorld(getVisualCol(), getVisualRow());
        float x = p.x - hpLayout.width * 0.5f;
        float y = p.y - 0.05f;
        float alpha = isDying() ? animController.getRenderAlpha() : 1f;
        Color prev = font.getColor().cpy();
        font.setColor(1f, 1f, 1f, alpha);
        font.draw(rc.batch, hpLayout, x, y);
        font.setColor(prev);
    }

    @Override public float       depth() { return Battlefield.floorZ(getVisualRow()); }
    @Override public RenderLayer layer() { return RenderLayer.BILLBOARD; }

    public SceneRenderable shadowView(Texture shadowTex) {
        return new SceneRenderable() {
            @Override public float       depth() { return Battlefield.floorZ(getVisualRow()); }
            @Override public RenderLayer layer() { return RenderLayer.GROUND; }
            @Override public void render(RenderContext rc) { renderShadow(rc, shadowTex); }
        };
    }

    public SceneRenderable hpLabelView() {
        return new SceneRenderable() {
            @Override public float       depth() { return Battlefield.floorZ(getVisualRow()); }
            @Override public RenderLayer layer() { return RenderLayer.OVERLAY; }
            @Override public void render(RenderContext rc) {
                if (isDead()) return;
                renderHpLabel(rc);
            }
        };
    }
}
