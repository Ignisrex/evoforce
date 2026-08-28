package com.silverignis.screens.state;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.crashinvaders.vfx.VfxManager;
import com.silverignis.components.Direction;
import com.silverignis.entities.Battlefield;
import com.silverignis.entities.Enemy;
import com.silverignis.entities.Player;
import com.silverignis.environment.BattlefieldDecor;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.input.GameAction;
import com.silverignis.input.InputManager;
import com.silverignis.render.RenderContext;
import com.silverignis.render.SceneRenderable;
import com.silverignis.render.WorldRenderer;
import com.silverignis.rewards.RewardOffer;
import com.silverignis.screens.GameOverScreen;
import com.silverignis.screens.GameScreen;
import com.silverignis.screens.OverworldScreen;
import com.silverignis.screens.RewardScreen;
import com.silverignis.skills.Skill;
import com.silverignis.skills.slots.ButtonSlot;
import com.silverignis.skills.slots.SlotKey;
import com.silverignis.environment.GameEnvironment;
import com.silverignis.systems.Encounter;
import com.silverignis.util.PanelGenerator;

import java.util.List;

public class PlayState implements GameScreenState {

    private GameScreen screen;
    private final InputManager input;

    private final Battlefield battlefield;
    private final Encounter encounter;
    private final Player player;
    private final List<Enemy> enemies;

    private final GameEnvironment environment;
    private final VfxManager vfxManager;

    private boolean transitionScheduled = false;

    private final WorldRenderer worldRenderer;
    private final ParticleEngine particles;
    private final RenderContext renderContext;
    private final SceneRenderable playerShadow;
    private final SceneRenderable[] enemyShadows;
    private final SceneRenderable[] enemyHpLabels;
    private final BattlefieldDecor battlefieldDecor;

    public PlayState(GameScreen screen) {
        this.screen = screen;
        this.input  = screen.getInputManager();

        this.encounter = screen.encounter;
        this.player    = encounter.player();
        this.enemies   = encounter.enemies();

        battlefield = new Battlefield(PanelGenerator.generatePanels());
        this.worldRenderer = screen.game.worldRenderer;
        this.particles = screen.game.particles;
        this.environment = screen.game.environment;
        environment.rebuild(MathUtils.random.nextLong());
        this.battlefieldDecor = new BattlefieldDecor(environment, battlefield);

        Texture shadowTex = screen.game.generated.shadow();
        this.renderContext = screen.game.renderContext;
        this.playerShadow = player.shadowView(shadowTex);
        this.enemyShadows = new SceneRenderable[enemies.size()];
        this.enemyHpLabels = new SceneRenderable[enemies.size()];
        for (int i = 0; i < enemies.size(); i++){
            enemyShadows[i] = enemies.get(i).shadowView(shadowTex);
            enemyHpLabels[i] = enemies.get(i).hpLabelView();
        }

        vfxManager = screen.game.vfxManager;
    }

    public Player      getPlayer()  { return player; }
    public List<Enemy> getEnemies() { return enemies; }

    @Override
    public void onEnter() {}

    @Override
    public void onExit() {}


    @Override
    public void input() {
        if (input.isActionJustPressed(GameAction.MOVE_UP))    encounter.tryMove(player, Direction.UP);
        if (input.isActionJustPressed(GameAction.MOVE_DOWN))  encounter.tryMove(player, Direction.DOWN);
        if (input.isActionJustPressed(GameAction.MOVE_LEFT))  encounter.tryMove(player, Direction.LEFT);
        if (input.isActionJustPressed(GameAction.MOVE_RIGHT)) encounter.tryMove(player, Direction.RIGHT);

        handleAttack();
        handleSlotFire();
        handleReleaseSkills();
        handleSkillSelectOpen();

    }

    @Override
    public void update(float delta) {
        encounter.tick(delta);

        encounter.collectCoveredTiles(battlefieldDecor::glow);
        battlefieldDecor.update(delta);

        particles.update(delta);
        environment.update(delta);
        checkBattleOver();
    }

    /** The encounter reports the result; deciding what a result *means* for the
     *  run — progression, rewards, which screen comes next — is this screen's job. */
    private boolean checkBattleOver() {
        if (transitionScheduled) return true;

        Encounter.Outcome outcome = encounter.outcome();
        if (outcome == Encounter.Outcome.ONGOING) return false;

        encounter.finish();
        encounter.resetStaging();
        transitionScheduled = true;

        if (outcome == Encounter.Outcome.DEFEAT) {
            screen.game.setScreen(new GameOverScreen(screen.game, GameOverScreen.Result.LOST));
            return true;
        }

        screen.game.session.playerProfile.progressPlayer();
        List<RewardOffer> offers = RewardScreen.offersFor(screen.game.session);
        screen.game.setScreen(offers.isEmpty() ? new OverworldScreen(screen.game)
                                               : new RewardScreen(screen.game, offers));
        return true;
    }

    @Override
    public void render(SpriteBatch batch) {
        ScreenUtils.clear(Color.BLACK);

        vfxManager.cleanUpBuffers();
        vfxManager.beginInputCapture();

        ScreenUtils.clear(Color.BLACK);

        // ── 3D cave pass ──────────────────────────────────────────────────
        environment.render(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        // Clear depth so 2D sprites always draw on top regardless of 3D depth
        Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);

        // ── 2D battle pass ────────────────────────────────────────────────
        screen.game.viewport.apply();
        batch.setProjectionMatrix(screen.game.viewport.getCamera().combined);
        batch.begin();
        renderWorld();
        batch.end();

        vfxManager.endInputCapture();
        vfxManager.applyEffects();
        vfxManager.renderToScreen();
    }

    @Override
    public void resize(int width, int height) {
        environment.resize(width, height);
    }

    public void renderWorld() {
        worldRenderer.submit(playerShadow);
        if (player.isAlive()) worldRenderer.submit(player);
        for (int i = 0; i<enemies.size(); i++){
            Enemy e = enemies.get(i);
            if (e.isDead()) continue;
            worldRenderer.submit(enemyShadows[i]);
            worldRenderer.submit(e);
            worldRenderer.submit(enemyHpLabels[i]);
        }
        encounter.combat().submitRenderables(worldRenderer);
        worldRenderer.submit(particles.emitters());
        worldRenderer.flush(renderContext);
    }

    public void dispose() {
        BattlefieldDecor.clear(environment);
    }

    private void handleReleaseSkills(){
        if (!input.isActionPressed(GameAction.TRIGGER_RIGHT) && player.getCaster().areSkillsLoaded()) {
            encounter.releaseLoadedSkills(player);
        }
    }

    private void handleAttack() {
        if (!input.isActionJustPressed(GameAction.ATTACK_BASIC)) return;
        // Gate lives in tryCast — basic attacks check blocksAttack, casts check
        // blocksCasting, and neither is this screen's business to re-decide.
        encounter.tryCast(player, player.getBasicAttack());
    }

    private void handleSlotFire() {
        if (input.isActionJustPressed(GameAction.SKILL_X)) tryFireSlot(SlotKey.X);
        if (input.isActionJustPressed(GameAction.SKILL_Y)) tryFireSlot(SlotKey.Y);
        if (input.isActionJustPressed(GameAction.SKILL_B)) tryFireSlot(SlotKey.B);
    }

    private void tryFireSlot(SlotKey key){
        // Checked *before* popping: a refused cast must not eat the staged card.
        // Cooldown is deliberately not re-checked — the card was filtered against
        // cooldowns when it was staged, which is why this uses cast, not tryCast.
        if (!player.canStartCast()) return;

        ButtonSlot slot = player.getSlots().get(key);
        if (slot.isEmpty()) return;

        Skill skill = slot.pop();

        if (input.isActionPressed(GameAction.TRIGGER_RIGHT)) {
            encounter.loadSkill(player, skill);
        } else {
            encounter.cast(player, skill);
        }
    }

    private void handleSkillSelectOpen() {
        if (input.isActionPressed(GameAction.TRIGGER_LEFT)
            && input.isActionPressed(GameAction.TRIGGER_RIGHT)) {
            screen.setState(screen.skillSelectState);
        }
    }
}
