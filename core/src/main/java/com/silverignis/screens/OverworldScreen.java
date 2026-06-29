package com.silverignis.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.silverignis.Main;
import com.silverignis.components.FreePosition;
import com.silverignis.environment.GameEnvironment;
import com.silverignis.input.GameAction;
import com.silverignis.input.InputManager;
import com.silverignis.render.RenderContext;
import com.silverignis.render.RenderLayer;
import com.silverignis.render.SceneRenderable;
import com.silverignis.systems.MovementSystem;

public class OverworldScreen implements Screen {

    private static final float AVATAR_SPEED = 4f;
    private static final float AVATAR_SIZE = 1.25f;

    private static final float DOOR_WIDTH  = 1.4f;
    private static final float DOOR_HEIGHT = 2.4f;
    // Floor-space trigger zones near the back wall (x, z, width, depthExtent).
    private static final Rectangle[] DOORS = {
        new Rectangle(-3.5f, -3f, 2f, 1.3f),
        new Rectangle( 1.5f, -3f, 2f, 1.3f),
    };

    private boolean transitioning = false;

    private final Main game;
    private final InputManager input = InputManager.defaultSetup();
    private final MovementSystem movementSystem = new MovementSystem();

    private final GameEnvironment environment;
    private final Sprite avatar;
    private final FreePosition pos;

    private final DoorRenderable[] doorRenderables;
    private final AvatarRenderable avatarRenderable = new AvatarRenderable();

    // Shared per-frame projection scratch; render() uses it immediately, never retains it.
    private final Vector2 projTmp = new Vector2();

    public OverworldScreen(Main game){
        this.game = game;
        this.environment = game.environment;

        this.avatar = new Sprite(game.assets.avatar());

        Rectangle bounds = new Rectangle(-5f, -3f, 10f, 6f);
        this.pos = new FreePosition(0f, 0f, AVATAR_SPEED, bounds);

        this.doorRenderables = new DoorRenderable[DOORS.length];
        for(int i =0; i<DOORS.length; i++) this.doorRenderables[i] = new DoorRenderable(DOORS[i]);
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
        input.update();
        handleInput(delta);
        checkDoors();
        if (transitioning) return;

        ScreenUtils.clear(Color.BLACK);

        game.vfxManager.cleanUpBuffers();
        game.vfxManager.beginInputCapture();

        ScreenUtils.clear(Color.BLACK);
        environment.render(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);

        game.viewport.apply();
        game.batch.setProjectionMatrix(game.viewport.getCamera().combined);
        game.batch.begin();
        for (DoorRenderable d : doorRenderables) game.worldRenderer.submit(d);
        game.worldRenderer.submit(avatarRenderable);
        game.worldRenderer.flush(game.renderContext);
        game.batch.end();

        game.vfxManager.endInputCapture();
        game.vfxManager.applyEffects();
        game.vfxManager.renderToScreen();
    }

    private void checkDoors() {
        for (Rectangle d: DOORS){
            if (d.contains(pos.getX(), pos.getY())){
                transitioning = true;
                game.setScreen(new GameScreen(game));
                return;
            }
        }
    }

    private void handleInput(float delta) {
        float dx = 0f, dz = 0f;
        if (input.isActionPressed(GameAction.MOVE_LEFT))  dx -= 1f;
        if (input.isActionPressed(GameAction.MOVE_RIGHT)) dx += 1f;
        if (input.isActionPressed(GameAction.MOVE_UP))    dz -= 1f;   // away from camera
        if (input.isActionPressed(GameAction.MOVE_DOWN))  dz += 1f;   // toward camera
        movementSystem.applyFreeInput(pos, dx, dz, delta);
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) return;
        game.viewport.update(width, height, true);
        environment.resize(width, height);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {}

    private final class DoorRenderable implements SceneRenderable {
        private final float centerX;
        private final float centerZ;

        DoorRenderable(Rectangle rect) {
            this.centerX = rect.x + rect.width * 0.5f;
            this.centerZ = rect.y + rect.height * 0.5f;
        }

        public float depth() { return centerZ; }
        public RenderLayer layer() { return RenderLayer.BILLBOARD; }

        public void render(RenderContext rc){
            Vector2 s = rc.project(centerX, centerZ, projTmp);
            float ds = rc.depthScale(centerZ);
            rc.batch.setColor(0.3f, 0.8f, 1f, 0.85f);
            rc.batch.draw(game.generated.pixel(), s.x - DOOR_WIDTH * ds * 0.5f, s.y, DOOR_WIDTH * ds, DOOR_HEIGHT * ds);
            rc.batch.setColor(Color.WHITE);
        }
    }

    private final class AvatarRenderable implements SceneRenderable {
        public float depth() { return pos.getY(); }
        public RenderLayer layer() { return RenderLayer.BILLBOARD; }

        public void render(RenderContext rc) {
            Vector2 screen = rc.project(pos.getX(), pos.getY(), projTmp);
            float size = AVATAR_SIZE * rc.depthScale(pos.getY());
            avatar.setBounds(screen.x - size * 0.5f, screen.y, size, size);
            avatar.draw(rc.batch);
        }
    }
}
