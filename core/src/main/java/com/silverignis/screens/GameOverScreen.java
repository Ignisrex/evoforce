package com.silverignis.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.utils.ScreenUtils;
import com.silverignis.Main;
import com.silverignis.input.GameAction;
import com.silverignis.input.InputManager;

public class GameOverScreen implements Screen {

    public enum Result { WON, LOST }

    private static final float TITLE_SCALE = 3f;
    private static final float HINT_OFFSET_BELOW_CENTER = 0.6f;

    private final Main game;
    private final Result result;
    private final InputManager input = InputManager.defaultSetup();
    private final GlyphLayout layout = new GlyphLayout();

    public GameOverScreen(Main game, Result result) {
        this.game = game;
        this.result = result;
    }

    @Override
    public void show() {}

    @Override
    public void render(float delta) {
        ScreenUtils.clear(Color.BLACK);

        game.viewport.apply();
        game.batch.setProjectionMatrix(game.viewport.getCamera().combined);

        float worldW = game.viewport.getWorldWidth();
        float worldH = game.viewport.getWorldHeight();

        game.batch.begin();

        float baseScaleX = game.font.getData().scaleX;
        float baseScaleY = game.font.getData().scaleY;

        game.font.getData().setScale(baseScaleX * TITLE_SCALE, baseScaleY * TITLE_SCALE);
        String title = (result == Result.WON) ? "YOU WON" : "YOU LOST";
        layout.setText(game.font, title);
        float titleX = (worldW - layout.width) / 2f;
        float titleY = worldH / 2f + layout.height / 2f;
        game.font.draw(game.batch, layout, titleX, titleY);

        game.font.getData().setScale(baseScaleX, baseScaleY);
        String hint = "Press Enter / A to restart";
        layout.setText(game.font, hint);
        float hintX = (worldW - layout.width) / 2f;
        float hintY = worldH / 2f - HINT_OFFSET_BELOW_CENTER;
        game.font.draw(game.batch, layout, hintX, hintY);

        game.batch.end();

        input.update();
        if (input.isActionJustPressed(GameAction.SKILL_SELECT_CONFIRM)) {
            game.setScreen(new GameScreen(game));
        }
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) return;
        game.viewport.update(width, height, true);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {}
}
