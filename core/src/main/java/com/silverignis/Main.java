package com.silverignis;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.crashinvaders.vfx.VfxManager;
import com.crashinvaders.vfx.effects.BloomEffect;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.silverignis.assets.GameAssets;
import com.silverignis.assets.GeneratedAssets;
import com.silverignis.environment.CaveTheme;
import com.silverignis.environment.GameEnvironment;
import com.silverignis.particles.Anchor;
import com.silverignis.particles.Channel;
import com.silverignis.particles.EmitterSpec;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.particles.Vfx;
import com.silverignis.registry.MonsterRegistry;
import com.silverignis.render.RenderContext;
import com.silverignis.render.SkillShaders;
import com.silverignis.render.WorldRenderer;
import com.silverignis.screens.MainMenuScreen;
import com.silverignis.sessions.GameSession;
import com.silverignis.skills.visuals.SkillVisuals;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game {


    public SpriteBatch batch;
    public BitmapFont font;
    public FitViewport viewport;
    public final GameAssets assets = new GameAssets();
    public final MonsterRegistry monsterRegistry = new MonsterRegistry(assets);
    public GeneratedAssets generated;
    public GameEnvironment environment;
    public WorldRenderer worldRenderer;
    public SkillShaders skillShaders;
    public RenderContext renderContext;
    public VfxManager vfxManager;
    public BloomEffect bloomEffect;

    public GameSession session;

    public ParticleEngine particles;

    public void create(){
        batch = new SpriteBatch();
        font = new BitmapFont();
        viewport = new FitViewport(16, 9);
        loadAssets();
        Vfx.init(assets);
        SkillVisuals.init(assets);

        this.generated = new GeneratedAssets();
        EmitterSpec.init(generated.pixel());

        this.environment = new GameEnvironment(viewport, CaveTheme.cave(assets.caveWall(), assets.caveFloor(), null));
        this.worldRenderer = new WorldRenderer();
        this.skillShaders = new SkillShaders(generated.pixel());
        this.renderContext = new RenderContext(batch, font, environment, skillShaders);

        this.particles = new ParticleEngine();
        Vfx.ambientDust().play(particles, Anchor.region(0f, 2.5f, -5f,  7f, 2.5f, 1.8f), Channel.AMBIENT);  // back
        Vfx.ambientDust().play(particles, Anchor.region(0f, 2.5f,  3.8f, 7f, 2.5f, 1.5f), Channel.AMBIENT); // fore

        this.vfxManager = new VfxManager(Pixmap.Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        this.bloomEffect = new BloomEffect();
        bloomEffect.setBaseIntensity(1.0f);
        bloomEffect.setBloomIntensity(1.4f);
        bloomEffect.setBloomSaturation(0.85f);
        bloomEffect.setThreshold(0.18f);
        vfxManager.addEffect(bloomEffect);

        font.setUseIntegerPositions(false);
        font.getData().setScale(viewport.getWorldHeight()/ Gdx.graphics.getHeight());

        this.session = new GameSession();

        this.setScreen(new MainMenuScreen(this));
    }

    private void loadAssets(){
        this.assets.queueLoad();
        this.assets.finishLoading();
    }

    public void render(){
        super.render();
    }

    @Override
    public void setScreen(Screen newScreen) {
        Screen old = getScreen();
        super.setScreen(newScreen);
        if (old != null) Gdx.app.postRunnable(old::dispose);
    }

    public void dispose(){
        if(session != null) session.dispose();
        batch.dispose();
        font.dispose();
        screen.dispose();
        if(skillShaders != null) skillShaders.dispose();
        if(generated != null) generated.dispose();
        if(bloomEffect != null) bloomEffect.dispose();
        if(vfxManager != null) vfxManager.dispose();
        environment.dispose();
        assets.dispose();
    }



    @Override
    public void resize(int width, int height) {
        // If the window is minimized on a desktop (LWJGL3) platform, width and height are 0, which causes problems.
        // In that case, we don't resize anything and wait for the window to be a normal size before updating.
        if(width <= 0 || height <= 0) return;

        // Resize your application here. The parameters represent the new window size.
        viewport.update(width, height, true);
        if (vfxManager != null) vfxManager.resize(width, height);
    }



    @Override
    public void pause() {
        // Invoked when your application is paused.
    }

    @Override
    public void resume() {
        // Invoked when your application is resumed after pause.
    }
}
