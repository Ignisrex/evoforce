package com.silverignis.screens.state;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.crashinvaders.vfx.VfxManager;
import com.silverignis.components.Direction;
import com.silverignis.components.Team;
import com.silverignis.entities.Battlefield;
import com.silverignis.entities.BattleVfx;
import com.silverignis.entities.Enemy;
import com.silverignis.entities.Player;
import com.silverignis.environment.BattlefieldDecor;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.registry.Monster;
import com.silverignis.input.GameAction;
import com.silverignis.input.InputManager;
import com.silverignis.render.RenderContext;
import com.silverignis.render.SceneRenderable;
import com.silverignis.render.WorldRenderer;
import com.silverignis.screens.GameOverScreen;
import com.silverignis.screens.GameScreen;
import com.silverignis.screens.OverworldScreen;
import com.silverignis.skills.ProjectileConfig;
import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillDeck;
import com.silverignis.skills.slots.ButtonSlot;
import com.silverignis.skills.slots.SlotKey;
import com.silverignis.systems.BattleContext;
import com.silverignis.environment.GameEnvironment;
import com.silverignis.systems.CombatSystem;
import com.silverignis.systems.MovementSystem;
import com.silverignis.systems.SpawnSystem;
import com.silverignis.systems.combat.DamageSystem;
import com.silverignis.systems.combat.TriggerBus;
import com.silverignis.util.PanelGenerator;

import java.util.ArrayList;
import java.util.List;

public class PlayState implements GameScreenState {

    private GameScreen screen;
    private final InputManager input;

    private final Battlefield battlefield;
    private final Player player;
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<BattleVfx> effects = new ArrayList<>();

    private final BattleContext battleContext;
    private final CombatSystem combatSystem;

    private final GameEnvironment environment;
    private final VfxManager vfxManager;

    private boolean transitionScheduled = false;

    private final WorldRenderer worldRenderer;
    private final ParticleEngine particles;
    private final RenderContext renderContext;
    private final SceneRenderable playerShadow;
    private final SceneRenderable[] enemyShadows;
    private final SceneRenderable[] enemyHpLabels;

    private final SpawnSystem spawnSystem;

    public PlayState(GameScreen screen) {
        this.screen = screen;
        this.input  = screen.getInputManager();

        var assets   = screen.game.assets;
        var registry = screen.game.monsterRegistry;

        float panelWidth  = 10f / Battlefield.COLS;
        float panelHeight = 4f  / Battlefield.ROWS;
        battlefield = new Battlefield(3f, 1f, panelWidth, panelHeight, PanelGenerator.generatePanels());
        this.worldRenderer = screen.game.worldRenderer;
        this.particles = screen.game.particles;
        this.environment = screen.game.environment;
        BattlefieldDecor.apply(environment, battlefield);

        this.spawnSystem = new SpawnSystem(
            screen.game.session.spawnTable,
            registry,
            screen.game.session.skills);

        player = new Player(1, 1, registry.getAnimSet(Monster.BEASTKIN, Team.PLAYER), battlefield, screen.game.session.playerProfile.getCaster(), screen.game.session.playerProfile.getStats());
        int level = screen.game.session.playerProfile.getProgressionLevel();
        enemies.addAll(spawnSystem.spawnNext(battlefield, level));

        Texture shadowTex = screen.game.generated.shadow();
        this.renderContext = screen.game.renderContext;
        this.playerShadow = player.shadowView(shadowTex);
        this.enemyShadows = new SceneRenderable[enemies.size()];
        this.enemyHpLabels = new SceneRenderable[enemies.size()];
        for (int i = 0; i < enemies.size(); i++){
            enemyShadows[i] = enemies.get(i).shadowView(shadowTex);
            enemyHpLabels[i] = enemies.get(i).hpLabelView();
        }

        TriggerBus triggerBus = new TriggerBus();
        DamageSystem damageSystem = new DamageSystem(triggerBus);
        MovementSystem movementSystem = new MovementSystem();

        battleContext = new BattleContext(battlefield, player, enemies, effects, environment, assets.clash(), damageSystem, triggerBus, movementSystem);
        combatSystem  = new CombatSystem(battleContext);
        battleContext.combatSystem = combatSystem;
        battleContext.particleEngine = particles;

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
        if (input.isActionJustPressed(GameAction.MOVE_UP))    battleContext.movementSystem.tryGridStep(player, Direction.UP);
        if (input.isActionJustPressed(GameAction.MOVE_DOWN))  battleContext.movementSystem.tryGridStep(player, Direction.DOWN);
        if (input.isActionJustPressed(GameAction.MOVE_LEFT))  battleContext.movementSystem.tryGridStep(player, Direction.LEFT);
        if (input.isActionJustPressed(GameAction.MOVE_RIGHT)) battleContext.movementSystem.tryGridStep(player, Direction.RIGHT);

        handleAttack();
        handleSlotFire();
        handleReleaseSkills();
        handleSkillSelectOpen();

    }

    @Override
    public void update(float delta) {
        tickEntities(delta);
        tickMeters(delta);
        enemyAi();
        combatSystem.tickStatuses(delta);
        combatSystem.update(delta);

        particles.update(delta);
        if (checkBattleOver()) return;
        tickAndCullEffects(delta);
    }

    private boolean checkBattleOver() {
        if (transitionScheduled) return true;

        if (allEnemiesDead()) {
            this.combatSystem.finishAll();
            transitionScheduled = true;
            screen.game.session.playerProfile.progressPlayer();
            screen.game.session.playerProfile.getCaster().resetStaging();
            screen.game.setScreen(new OverworldScreen(screen.game));
            return true;
        }
        if (!player.isAlive()) {
            screen.game.session.playerProfile.getCaster().resetStaging();
            this.combatSystem.finishAll();
            transitionScheduled = true;
            screen.game.setScreen(new GameOverScreen(screen.game, GameOverScreen.Result.LOST));
            return true;
        }
        return false;
    }

    private boolean allEnemiesDead() {
        for (Enemy e : enemies) if (!e.isDead()) return false;
        return true;
    }

    private void tickEntities(float delta) {
        player.update(delta);
        for (Enemy e : enemies) e.update(delta, battleContext);

        Vector2 pp = battleContext.projectedTileWorld(player.getCol(), player.getRow());
        player.setProjectedTarget(pp.x, pp.y);
        player.setDepthScale(battleContext.tileDepthScale(player.getRow()));

        for (Enemy e : enemies) {
            Vector2 ep = battleContext.projectedTileWorld(e.getCol(), e.getRow());
            e.setProjectedTarget(ep.x, ep.y);
            e.setDepthScale(battleContext.tileDepthScale(e.getRow()));
        }
    }

    private void tickMeters(float delta) {
        screen.charge.update(delta);
        // Per-caster cooldowns tick inside the deck, driven by Player.update / Enemy.update
        // (see Caster.update). Nothing to do here for them anymore.
    }

    private void tickAndCullEffects(float delta) {
        for (BattleVfx e : effects) e.update(delta);
        effects.removeIf(e -> !e.isAlive());
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
        battleContext.buildCache();
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
        combatSystem.submitRenderables(worldRenderer);
        worldRenderer.submit(effects);
        worldRenderer.submit(particles.emitters());
        worldRenderer.flush(renderContext);
    }

    public void dispose() {
        BattlefieldDecor.clear(environment);
    }

    private void handleReleaseSkills(){
        if (!input.isActionPressed(GameAction.TRIGGER_RIGHT) && player.getCaster().areSkillsLoaded()) {
            combatSystem.resolveLoadedSkills(player);
        }
    }

    private void handleAttack() {
        //handles basic attack
        if( player.isInputLocked() || player.getStatusContainer().blocksMovement()) return;

        if (!input.isActionJustPressed(GameAction.ATTACK_BASIC)) return;
        if (player.isInputLocked()) return;

        Skill skill = player.getBasicAttack();
        if (skill == null) return;

        if (player.getDeck().isOnCooldown(skill)) return;
        combatSystem.fireSkill(player, skill);
    }

    private void handleSlotFire() {
        if (input.isActionJustPressed(GameAction.SKILL_X)) tryFireSlot(SlotKey.X);
        if (input.isActionJustPressed(GameAction.SKILL_Y)) tryFireSlot(SlotKey.Y);
        if (input.isActionJustPressed(GameAction.SKILL_B)) tryFireSlot(SlotKey.B);
    }

    private void tryFireSlot(SlotKey key){
        if (player.isInputLocked() || player.getStatusContainer().blocksMovement()) return;

        ButtonSlot slot = player.getSlots().get(key);
        if (slot.isEmpty()) return;

        Skill skill = slot.pop();

        if (input.isActionPressed(GameAction.TRIGGER_RIGHT)) {
            combatSystem.loadSkill(player, skill);
        }else {
            combatSystem.fireSkill(player, skill);
        }
    }

    private void handleSkillSelectOpen() {
        // The charge meter gates *menu access*, not individual casts:
        // you can only stage new skills once the bar is full.
        if (!screen.charge.isFull()) return;
        if (input.isActionPressed(GameAction.TRIGGER_LEFT)
            && input.isActionPressed(GameAction.TRIGGER_RIGHT)) {
            screen.setState(screen.skillSelectState);
        }
    }

    // Self-contained placeholder AI. Each enemy picks the first off-cooldown
    // skill whose shape could plausibly land on the player from its current
    // tile, falling back to its basic attack. Slated for overhaul — keep all
    // the logic in this section so the rewrite can lift it cleanly.
    private void enemyAi() {
        for (Enemy enemy : enemies) {
            if (!enemy.wantsToBasicAttack()) continue;
            Skill chosen = pickEnemyAction(enemy);
            if (chosen == null) continue;
            enemy.onBasicAttackFired();
            combatSystem.fireSkill(enemy, chosen);
        }
    }

    private Skill pickEnemyAction(Enemy enemy) {
        SkillDeck deck = enemy.getDeck();
        for (Skill s : deck.all()) {
            if (deck.isOnCooldown(s)) continue;
            if (canSkillReachPlayer(enemy, s)) return s;
        }
        Skill basic = enemy.getBasicAttack();
        if (basic != null && !deck.isOnCooldown(basic) && canSkillReachPlayer(enemy, basic)) {
            return basic;
        }
        return null;
    }

    private boolean canSkillReachPlayer(Enemy enemy, Skill skill) {
        int dir = -1; // enemies face west
        int dr = player.getRow() - enemy.getRow();
        int dc = player.getCol() - enemy.getCol();

        switch (skill.getShape()) {
            case AURA:
                return true;
            case STRIKE:
                return dr == 0 && dc == 2 * dir;
            case ZONE:
                return dr == 0 && dc == dir;
            case BEAM:
                return dr == 0 && dc * dir > 0;
            case PROJECTILE:
                if (dr != 0) return false;
                if (skill.getShapeConfig() instanceof ProjectileConfig) {
                    ProjectileConfig pc = (ProjectileConfig) skill.getShapeConfig();
                    if (pc.getMovementType() == ProjectileConfig.MovementType.LOB) {
                        return dc == pc.getTargetRange() * dir;
                    }
                }
                return dc * dir > 0;
            default:
                return false;
        }
    }
}
