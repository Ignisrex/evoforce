package com.silverignis.state;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.crashinvaders.vfx.VfxManager;
import com.crashinvaders.vfx.effects.BloomEffect;
import com.silverignis.entities.*;
import com.silverignis.entities.BattleVfx;
import com.silverignis.input.GameAction;
import com.silverignis.screens.GameScreen;
import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillFactory;
import com.silverignis.skills.SkillInstance;
import com.silverignis.skills.slots.ButtonSlot;
import com.silverignis.skills.slots.SlotKey;
import com.badlogic.gdx.graphics.GL20;
import com.silverignis.systems.BattleContext;
import com.silverignis.systems.CaveEnvironment;
import com.silverignis.systems.CombatSystem;
import com.silverignis.util.PanelGenerator;

import java.util.ArrayList;
import java.util.List;

public class PlayState implements GameScreenState {

    private static final class Assets {
        final Texture player, enemy, windSlash, clash, shadow;
        final Music music;
        final Sound drop;

        Assets() {
            player         = new Texture("sprites/beastkin.png");
            enemy          = new Texture("sprites/skeleton.png");
            windSlash      = new Texture("attacks/wind_slash.png");
            clash          = new Texture("effects/clash.png");
            shadow         = buildShadowTexture();
            drop           = Gdx.audio.newSound(Gdx.files.internal("drop.mp3"));
            music          = Gdx.audio.newMusic(Gdx.files.internal("music.mp3"));
            music.setLooping(true);
            music.setVolume(0.5f);
        }

        void dispose() {
            player.dispose(); enemy.dispose();   windSlash.dispose();
            clash.dispose();  shadow.dispose();  drop.dispose(); music.dispose();
        }
    }

    private GameScreen screen;
    private final Assets assets;

    private final Battlefield battlefield;
    private final Player player;
    private final Enemy enemy;
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<BattleVfx> effects      = new ArrayList<>();

    private final BattleContext battleContext;
    private final CombatSystem combatSystem;

    private final List<Vector2> clashPositions = new ArrayList<>();

    private final CaveEnvironment cave3D;
    private final VfxManager vfxManager;
    private final BloomEffect bloomEffect;

    public PlayState(GameScreen screen) {
        this.screen = screen;
        this.assets = new Assets();

        float panelWidth  = 10f / Battlefield.COLS;
        float panelHeight = 4f  / Battlefield.ROWS;
        battlefield = new Battlefield(3f, 1f, panelWidth, panelHeight, PanelGenerator.generatePanels());

        cave3D = new CaveEnvironment(battlefield, screen.game.viewport);

        player = new Player(1, 1, new Sprite(assets.player), battlefield, 100, assets.windSlash);
        enemy  = new Enemy(Battlefield.COLS - 2, 1, new Sprite(assets.enemy), battlefield, 100, assets.windSlash);

        battleContext = new BattleContext(battlefield, player, enemy, effects, cave3D);
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

    @Override
    public void onEnter() {}

    @Override
    public void onExit() {}


    @Override
    public void input() {
        var input = screen.getInputManager();
        if (input.isActionJustPressed(GameAction.MOVE_UP))    player.moveUp();
        if (input.isActionJustPressed(GameAction.MOVE_DOWN))  player.moveDown();
        if (input.isActionJustPressed(GameAction.MOVE_LEFT))  player.moveLeft();
        if (input.isActionJustPressed(GameAction.MOVE_RIGHT)) player.moveRight();

        handleAttack();
        handleSlotFire();
        handleSkillSelectOpen();
    }

    @Override
    public void update(float delta) {
        tickEntities(delta);
        tickMeters(delta);
        enemyAi();
        tickProjectiles(delta);
        combatSystem.update(delta);
        resolveCollisions();
        tickAndCullEffects(delta);
        cullDeadProjectiles();
    }

    private void tickEntities(float delta) {
        player.update(delta);
        enemy.update(delta);

        Vector2 pp = battleContext.projectedTileWorld(player.getCol(), player.getRow());
        player.setProjectedTarget(pp.x, pp.y);
        player.setDepthScale(battleContext.tileDepthScale(player.getRow()));

        Vector2 ep = battleContext.projectedTileWorld(enemy.getCol(), enemy.getRow());
        enemy.setProjectedTarget(ep.x, ep.y);
        enemy.setDepthScale(battleContext.tileDepthScale(enemy.getRow()));
    }

    private void tickMeters(float delta) {
        screen.charge.update(delta);
        screen.cooldowns.update(delta);
    }

    private void tickProjectiles(float delta) {
        float worldWidth = screen.game.viewport.getWorldWidth();
        for (Projectile projectile : projectiles) {
            projectile.update(delta, worldWidth);
        }
    }

    private void resolveCollisions() {
        clashPositions.clear();
        CollisionResolver.resolve(projectiles, player, enemy, clashPositions);

        float panelSize = Math.max(battlefield.getPanelWidth(), battlefield.getPanelHeight());
        for (Vector2 pos : clashPositions) {
            effects.add(new ClashEffect(assets.clash, pos.x, pos.y, panelSize));
        }
    }

    private void tickAndCullEffects(float delta) {
        for (BattleVfx e : effects) e.update(delta);
        effects.removeIf(e -> !e.isAlive());
    }

    private void cullDeadProjectiles() {
        projectiles.removeIf(p -> !p.isAlive());
    }

    @Override
    public void render(SpriteBatch batch) {
        ScreenUtils.clear(Color.BLACK);

        vfxManager.cleanUpBuffers();
        vfxManager.beginInputCapture();

        ScreenUtils.clear(Color.BLACK);

        // ── 3D cave pass ──────────────────────────────────────────────────
        cave3D.render(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
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
        cave3D.resize(width, height);
        battleContext.buildCache();
    }

    public void renderWorld(SpriteBatch batch) {
        // ── Layer 1: Zone effects (terrain-level skill visuals) ───────────
        combatSystem.renderUnder(batch);

        // ── Layer 3: Shadows ──────────────────────────────────────────────
        if (player.isAlive()) player.renderShadow(batch, assets.shadow);
        if (enemy.isAlive())  enemy.renderShadow(batch, assets.shadow);

        // ── Layer 4: Entities — Y-sorted (higher Y = farther = drawn first) ─
        if (player.getVisualY() >= enemy.getVisualY()) {
            player.render(batch);
            enemy.render(batch);
        } else {
            enemy.render(batch);
            player.render(batch);
        }

        // ── Layer 5: Projectiles and skill VFX ───────────────────────────
        for (Projectile p : projectiles) p.render(batch);
        combatSystem.render(batch);
        for (BattleVfx e : effects)      e.render(batch);
    }

    private static Texture buildShadowTexture() {
        int w = 64, h = 32;
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pm.setColor(0f, 0f, 0f, 0f);
        pm.fill();
        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                float nx = (px - w * 0.5f) / (w * 0.5f);
                float ny = (py - h * 0.5f) / (h * 0.5f);
                float d  = nx * nx + ny * ny;
                if (d < 1f) {
                    pm.setColor(0f, 0f, 0f, (1f - d) * 0.6f);
                    pm.drawPixel(px, py);
                }
            }
        }
        Texture tex = new Texture(pm);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        return tex;
    }

    public void dispose() {
        assets.dispose();
        battlefield.dispose();
        cave3D.dispose();
        bloomEffect.dispose();
        vfxManager.dispose();
    }

    private void handleAttack() {
        var input = screen.getInputManager();
        if (input.isActionPressed(GameAction.ATTACK_BASIC) && player.canBasicAttack()) {
            projectiles.add(player.attack());
        }
    }

    private void handleSlotFire() {
        var input = screen.getInputManager();
        if (input.isActionJustPressed(GameAction.SKILL_X)) tryFireSlot(SlotKey.X);
        if (input.isActionJustPressed(GameAction.SKILL_Y)) tryFireSlot(SlotKey.Y);
        if (input.isActionJustPressed(GameAction.SKILL_B)) tryFireSlot(SlotKey.B);
    }

    private void tryFireSlot(SlotKey key){
        if (player.isInputLocked()) return;

        ButtonSlot slot = screen.slots.get(key);
        if (slot.isEmpty()) return;

        Skill skill = slot.pop();
        screen.cooldowns.onUsed(skill);
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
        if (enemy.canBasicAttack()) {
            projectiles.add(enemy.attack());
        }
    }
}
