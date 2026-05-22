package com.silverignis.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.silverignis.Main;
import com.silverignis.components.FreePosition;
import com.silverignis.evironment.GameEnvironment;
import com.silverignis.input.GameAction;
import com.silverignis.input.InputManager;
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

    private final Texture doorTex;
    private boolean transitioning = false;

    private final Main game;
    private final InputManager input = InputManager.defaultSetup();
    private final MovementSystem movementSystem = new MovementSystem();

    private final GameEnvironment environment;
    private final Texture avatarTex;
    private final Sprite avatar;
    private final FreePosition pos;

    public OverworldScreen(Main game){
        this.game = game;
        this.environment = new GameEnvironment(game.viewport);

        this.avatarTex = new Texture("sprites/beastkin.png");
        this.avatar = new Sprite(avatarTex);

        Rectangle bounds = new Rectangle(-5f, -3f, 10f, 6f);
        this.pos = new FreePosition(0f, 0f, AVATAR_SPEED, bounds);

        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        this.doorTex = new Texture(pm);
        pm.dispose();
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
        environment.render(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);

        game.viewport.apply();
        game.batch.setProjectionMatrix(game.viewport.getCamera().combined);
        game.batch.begin();
        renderDoors();
        renderAvatar();
        game.batch.end();
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

    private void renderDoors() {
        game.batch.setColor(0.3f, 0.8f, 1f, 0.85f);
        for (Rectangle d : DOORS) {
            float cx = d.x + d.width * 0.5f;
            float cz = d.y + d.height * 0.5f;
            Vector2 s = environment.project(cx, cz);
            float ds = environment.depthScale(cz);
            game.batch.draw(doorTex, s.x - DOOR_WIDTH * ds * 0.5f, s.y, DOOR_WIDTH * ds, DOOR_HEIGHT * ds);
        }
        game.batch.setColor(Color.WHITE);
    }

    private void handleInput(float delta) {
        float dx = 0f, dz = 0f;
        if (input.isActionPressed(GameAction.MOVE_LEFT))  dx -= 1f;
        if (input.isActionPressed(GameAction.MOVE_RIGHT)) dx += 1f;
        if (input.isActionPressed(GameAction.MOVE_UP))    dz -= 1f;   // away from camera
        if (input.isActionPressed(GameAction.MOVE_DOWN))  dz += 1f;   // toward camera
        movementSystem.applyFreeInput(pos, dx, dz, delta);
    }

    private void renderAvatar() {
        Vector2 screen = environment.project(pos.getX(), pos.getY());
        float size = AVATAR_SIZE * environment.depthScale(pos.getY());
        avatar.setBounds(screen.x - size * 0.5f, screen.y, size, size);
        avatar.draw(game.batch);
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
    public void dispose() {
        environment.dispose();
        avatarTex.dispose();
        doorTex.dispose();
    }
}
