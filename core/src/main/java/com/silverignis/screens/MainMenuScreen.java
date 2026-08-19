package com.silverignis.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.silverignis.Main;
import com.silverignis.input.GameAction;
import com.silverignis.input.InputManager;
import com.silverignis.ui.MenuBackdrop;
import com.silverignis.ui.MainMenuStyle;
import com.silverignis.ui.UiUtil;

import java.util.ArrayList;
import java.util.List;

public class MainMenuScreen implements Screen {

    final Main game;
    private final MenuBackdrop backdrop;
    private final Stage stage;
    private final MainMenuStyle style;
    private final InputManager input = InputManager.defaultSetup();

    private final List<TextButton> buttons = new ArrayList<>();
    private final List<Runnable> actions = new ArrayList<>();
    private int selected = 0;
    private boolean pressing;

    public MainMenuScreen(Main game){
        this.game = game;
        this.backdrop = new MenuBackdrop();
        this.style = new MainMenuStyle(game.generated, game.viewport);
        this.stage = new Stage(game.viewport, game.batch);

        Table root = UiUtil.newTable();
        root.setFillParent(true);
        root.bottom().left().pad(0.4f);
        stage.addActor(root);

        addButton(root, "New Game", this::startGame);
        addButton(root, "Multiplayer", () -> {}).setDisabled(true);
        addButton(root, "Settings", () -> {});
        addButton(root, "Exit", () -> Gdx.app.exit());
        root.add(new Label("v" + version(), style.version)).left().padTop(0.1f);

        root.validate();
        for (TextButton b : buttons) {
            b.setTransform(true);
            b.setOrigin(b.getWidth() / 2f, b.getHeight() / 2f);
        }
        focus(0);
    }

    /** Written by the generateAssetList gradle task from projectVersion. */
    private static String version() {
        var f = Gdx.files.internal("version.txt");
        return f.exists() ? f.readString().trim() : "dev";
    }

    private TextButton addButton(Table root, String text, Runnable action) {
        TextButton b = new TextButton(text, style.button);
        b.setRound(false);   // world-unit stage; see UiUtil.newTable()
        int index = buttons.size();
        b.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) { press(index); }
        });
        b.addListener(new ClickListener() {
            @Override public void enter(InputEvent event, float x, float y, int pointer, Actor from) {
                if (pointer == -1 && !b.isDisabled()) focus(index);
            }
        });
        buttons.add(b);
        actions.add(action);
        root.add(b).size(MainMenuStyle.BUTTON_W, MainMenuStyle.BUTTON_H).padBottom(MainMenuStyle.BUTTON_GAP).row();
        return b;
    }

    /** Punch the plate and flash it, then run the button's action. */
    private void press(int index) {
        if (pressing) return;
        pressing = true;
        TextButton b = buttons.get(index);
        b.setStyle(style.buttonPressed);
        b.addAction(Actions.sequence(
            Actions.scaleTo(0.94f, 0.94f, 0.05f, Interpolation.pow2Out),
            Actions.scaleTo(1f, 1f, 0.12f, Interpolation.swingOut),
            Actions.run(() -> {
                pressing = false;
                focus(index);
                actions.get(index).run();
            })));
    }

    private void focus(int index) {
        buttons.get(selected).setStyle(style.button);
        selected = index;
        buttons.get(selected).setStyle(style.buttonFocused);
    }

    /** Move the focus by `dir`, skipping disabled buttons. */
    private void moveFocus(int dir) {
        int n = buttons.size(), i = selected;
        do { i = (i + dir + n) % n; } while (buttons.get(i).isDisabled() && i != selected);
        focus(i);
    }

    private void startGame() {
        game.setScreen(new OverworldScreen(game));
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(Color.BLACK);

        backdrop.update(delta);

        game.viewport.apply();
        game.batch.setProjectionMatrix(game.viewport.getCamera().combined);

        game.batch.begin();
        backdrop.draw(game.batch, game.viewport.getWorldWidth(), game.viewport.getWorldHeight());
        game.batch.end();

        input.update();
        if (input.isActionJustPressed(GameAction.MOVE_UP))   moveFocus(-1);
        if (input.isActionJustPressed(GameAction.MOVE_DOWN)) moveFocus(1);
        if (input.isActionJustPressed(GameAction.SKILL_SELECT_CONFIRM)) press(selected);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        if(width <= 0 || height <= 0) return;
        game.viewport.update(width, height, true);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        backdrop.dispose();
        stage.dispose();
        style.dispose();
    }
}
