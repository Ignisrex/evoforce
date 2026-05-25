package com.silverignis;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.silverignis.assets.GameAssets;
import com.silverignis.assets.GeneratedAssets;
import com.silverignis.evironment.GameEnvironment;
import com.silverignis.registry.MonsterRegistry;
import com.silverignis.render.RenderContext;
import com.silverignis.render.WorldRenderer;
import com.silverignis.screens.MainMenuScreen;
import com.silverignis.sessions.GameSession;

import java.util.ArrayList;

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
    public RenderContext renderContext;

    public GameSession session;

    public void create(){
        batch = new SpriteBatch();
        font = new BitmapFont();
        viewport = new FitViewport(16, 9);
        loadAssets();

        this.environment = new GameEnvironment(viewport, assets.caveWall(), assets.caveFloor());
        this.worldRenderer = new WorldRenderer();
        this.renderContext = new RenderContext(batch, font, environment);

        font.setUseIntegerPositions(false);
        font.getData().setScale(viewport.getWorldHeight()/ Gdx.graphics.getHeight());

        this.generated = new GeneratedAssets();

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
        if(generated != null) generated.dispose();
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
