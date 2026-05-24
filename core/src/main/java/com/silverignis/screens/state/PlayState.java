package com.silverignis.screens.state;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.crashinvaders.vfx.VfxManager;
import com.crashinvaders.vfx.effects.BloomEffect;
import com.silverignis.components.Direction;
import com.silverignis.components.Stats;
import com.silverignis.components.Team;
import com.silverignis.entities.Battlefield;
import com.silverignis.entities.BattleVfx;
import com.silverignis.registry.Monster;
import com.silverignis.entities.Enemy;
import com.silverignis.entities.Player;
import com.silverignis.evironment.BattlefieldDecor;
import com.silverignis.input.GameAction;
import com.silverignis.screens.GameOverScreen;
import com.silverignis.screens.GameScreen;
import com.silverignis.screens.OverworldScreen;
import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillDeck;
import com.silverignis.skills.SkillFactory;
import com.silverignis.skills.SkillInstance;
import com.silverignis.skills.slots.ButtonSlot;
import com.silverignis.skills.slots.SlotKey;
import com.badlogic.gdx.graphics.GL20;
import com.silverignis.systems.BattleContext;
import com.silverignis.evironment.GameEnvironment;
import com.silverignis.systems.CombatSystem;
import com.silverignis.systems.MovementSystem;
import com.silverignis.systems.combat.DamageSystem;
import com.silverignis.systems.combat.TriggerBus;
import com.silverignis.util.PanelGenerator;

import java.util.ArrayList;
import java.util.List;

public class PlayState implements GameScreenState {

    private GameScreen screen;

    private final Battlefield battlefield;
    private final Player player;
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<BattleVfx> effects = new ArrayList<>();

    private final BattleContext battleContext;
    private final CombatSystem combatSystem;

    private final GameEnvironment environment;
    private final VfxManager vfxManager;
    private final BloomEffect bloomEffect;

    private boolean transitionScheduled = false;

    public PlayState(GameScreen screen) {
        this.screen = screen;

        var assets   = screen.game.assets;
        var registry = screen.game.monsterRegistry;

        float panelWidth  = 10f / Battlefield.COLS;
        float panelHeight = 4f  / Battlefield.ROWS;
        battlefield = new Battlefield(3f, 1f, panelWidth, panelHeight, PanelGenerator.generatePanels());

        environment = new GameEnvironment(screen.game.viewport, assets.caveWall(), assets.caveFloor());
        BattlefieldDecor.apply(environment, battlefield);

        player = new Player(1, 1, new Sprite(registry.getMonsterTexture(Monster.BEASTKIN, Team.PLAYER)), battlefield, screen.game.session.playerProfile.getCaster(), screen.game.session.playerProfile.getStats());
        enemies.add(new Enemy(Battlefield.COLS - 2, 1, new Sprite(registry.getMonsterTexture(Monster.ELDER_LICH, Team.ENEMY)), battlefield, new Stats(20, 10, 100, 10, 20)));
        enemies.add(new Enemy(Battlefield.COLS - 1, 2, new Sprite(registry.getMonsterTexture(Monster.SKELETON, Team.ENEMY)), battlefield, new Stats(20, 10, 100, 10, 20)));

        TriggerBus triggerBus = new TriggerBus();
        DamageSystem damageSystem = new DamageSystem(triggerBus);
        MovementSystem movementSystem = new MovementSystem();

        battleContext = new BattleContext(battlefield, player, enemies, effects, environment, assets.clash(), damageSystem, triggerBus, movementSystem);
        combatSystem  = new CombatSystem(battleContext);
        battleContext.combatSystem = combatSystem;

        vfxManager = new VfxManager(Pixmap.Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        bloomEffect = new BloomEffect();
        bloomEffect.setBaseIntensity(1.0f);
        bloomEffect.setBloomIntensity(1.2f);
        bloomEffect.setBloomSaturation(0.85f);
        bloomEffect.setThreshold(0.25f);
        vfxManager.addEffect(bloomEffect);
    }

    public Player      getPlayer()  { return player; }
    public List<Enemy> getEnemies() { return enemies; }

    @Override
    public void onEnter() {}

    @Override
    public void onExit() {}


    @Override
    public void input() {
        var input = screen.getInputManager();
        if (input.isActionJustPressed(GameAction.MOVE_UP))    battleContext.movementSystem.tryGridStep(player, Direction.UP);
        if (input.isActionJustPressed(GameAction.MOVE_DOWN))  battleContext.movementSystem.tryGridStep(player, Direction.DOWN);
        if (input.isActionJustPressed(GameAction.MOVE_LEFT))  battleContext.movementSystem.tryGridStep(player, Direction.LEFT);
        if (input.isActionJustPressed(GameAction.MOVE_RIGHT)) battleContext.movementSystem.tryGridStep(player, Direction.RIGHT);

        handleAttack();
        handleSlotFire();
        handleSkillSelectOpen();
    }

    @Override
    public void update(float delta) {
        tickEntities(delta);
        tickMeters(delta);
        enemyAi();
        combatSystem.tickStatuses(delta);
        combatSystem.update(delta);
        if (checkBattleOver()) return;
        tickAndCullEffects(delta);
    }

    private boolean checkBattleOver() {
        if (transitionScheduled) return true;
        if (allEnemiesDead()) {
            transitionScheduled = true;
            screen.game.session.playerProfile.getCaster().getSlots().clearAll();
            screen.game.setScreen(new OverworldScreen(screen.game));
            return true;
        }
        if (!player.isAlive()) {
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
        renderWorld(batch);
        batch.end();

        vfxManager.endInputCapture();
        vfxManager.applyEffects();
        vfxManager.renderToScreen();
    }

    @Override
    public void resize(int width, int height) {
        vfxManager.resize(width, height);
        environment.resize(width, height);
        battleContext.buildCache();
    }

    public void renderWorld(SpriteBatch batch) {
        // ── Layer 1: Zone effects (terrain-level skill visuals) ───────────
        combatSystem.renderUnder(batch);

        // ── Layer 3: Shadows ──────────────────────────────────────────────
        Texture shadow = screen.game.generated.shadow();
        if (player.isAlive()) player.renderShadow(batch, shadow);
        for (Enemy e : enemies) if (e.isAlive()) e.renderShadow(batch, shadow);

        // ── Layer 4: Entities — Y-sorted (higher Y = farther = drawn first) ─
        renderEntitiesYSorted(batch);

        // ── Layer 5: Skill VFX (both casters' projectiles, beams, auras, etc.) ─
        combatSystem.render(batch);
        for (BattleVfx e : effects) e.render(batch);
    }

    private void renderEntitiesYSorted(SpriteBatch batch) {

        List<Object> drawList = new ArrayList<>(1 + enemies.size());
        if (player.isAlive()) drawList.add(player);
        for (Enemy e : enemies) if (!e.isDead()) drawList.add(e);
        drawList.sort((a, b) -> Float.compare(visualY(b), visualY(a)));
        for (Object o : drawList) {
            if (o instanceof Player) ((Player) o).render(batch);
            else                     ((Enemy) o).render(batch, screen.game.font);
        }
    }

    private static float visualY(Object o) {
        if (o instanceof Player) return ((Player) o).getVisualY();
        return ((Enemy) o).getVisualY();
    }

    public void dispose() {
        environment.dispose();
        bloomEffect.dispose();
        vfxManager.dispose();
    }

    private void handleAttack() {
        if( player.isInputLocked() || player.getStatusContainer().blocksMovement()) return;

        var input = screen.getInputManager();
        if (!input.isActionJustPressed(GameAction.ATTACK_BASIC)) return;
        if (player.isInputLocked()) return;
        Skill skill = player.getBasicAttack();
        if (skill == null) return;
        SkillDeck deck = player.getDeck();
        if (deck.isOnCooldown(skill)) return;
        deck.onUsed(skill);
        combatSystem.spawn(SkillFactory.create(skill, player));
    }

    private void handleSlotFire() {
        var input = screen.getInputManager();
        if (input.isActionJustPressed(GameAction.SKILL_X)) tryFireSlot(SlotKey.X);
        if (input.isActionJustPressed(GameAction.SKILL_Y)) tryFireSlot(SlotKey.Y);
        if (input.isActionJustPressed(GameAction.SKILL_B)) tryFireSlot(SlotKey.B);
    }

    private void tryFireSlot(SlotKey key){
        if (player.isInputLocked() || player.getStatusContainer().blocksMovement()) return;

        ButtonSlot slot = player.getSlots().get(key);
        if (slot.isEmpty()) return;

        Skill skill = slot.pop();
        player.getDeck().onUsed(skill);
        SkillInstance instance = SkillFactory.create(skill, player);
        combatSystem.spawn(instance);
    }

    private void handleSkillSelectOpen() {
        // The charge meter gates *menu access*, not individual casts:
        // you can only stage new skills once the bar is full.
        if (!screen.charge.isFull()) return;
        var input = screen.getInputManager();
        if (input.isActionPressed(GameAction.TRIGGER_LEFT)
            && input.isActionPressed(GameAction.TRIGGER_RIGHT)) {
            screen.setState(screen.skillSelectState);
        }
    }

    private void enemyAi() {
        for (Enemy enemy : enemies) {
            if (!enemy.wantsToBasicAttack()) continue;
            Skill skill = enemy.getBasicAttack();
            if (skill == null) continue;
            SkillDeck deck = enemy.getDeck();
            if (deck.isOnCooldown(skill)) continue;
            deck.onUsed(skill);
            enemy.onBasicAttackFired();
            combatSystem.spawn(SkillFactory.create(skill, enemy));
        }
    }
}
