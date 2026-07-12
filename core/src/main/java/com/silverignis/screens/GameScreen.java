package com.silverignis.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.silverignis.Main;
import com.silverignis.input.InputManager;
import com.silverignis.skills.ChargeMeter;
import com.silverignis.screens.state.GameScreenState;
import com.silverignis.screens.state.PlayState;
import com.silverignis.screens.state.SkillSelectState;
import com.silverignis.ui.BasicAttackHud;
import com.silverignis.ui.ChargeBarHud;
import com.silverignis.ui.FpsHud;
import com.silverignis.ui.LifeBarHud;
import com.silverignis.ui.SlotsHud;

public class GameScreen implements Screen {

    public Main game;
    private final InputManager input = InputManager.defaultSetup();

    //States
    public final PlayState playState;
    public final SkillSelectState skillSelectState;
    private GameScreenState currentState;

    public final ChargeMeter charge = new ChargeMeter(/* max */ 1f, /* fillRate */ 0.20f);

    //HUDs — share one 1x1 pixel from GeneratedAssets; FpsHud owns no texture.
    private final ChargeBarHud chargeHud;
    private final SlotsHud slotsHud;
    private final FpsHud fpsHud = new FpsHud();
    private final LifeBarHud lifeBarHud;
    private final BasicAttackHud basicAttackHud;

    public GameScreen(Main game){
        this.game = game;

        Texture pixel = game.generated.pixel();
        this.chargeHud      = new ChargeBarHud(pixel);
        this.slotsHud       = new SlotsHud(game.generated);
        this.lifeBarHud     = new LifeBarHud(pixel);
        this.basicAttackHud = new BasicAttackHud(game.generated);

        this.playState = new PlayState(this);
        this.skillSelectState = new SkillSelectState(this);

        setState(playState);
    }

    public void setState(GameScreenState nextState) {
        if (currentState == nextState) return;
        if (currentState != null) currentState.onExit();
        currentState = nextState;
        currentState.onEnter();
    }

    public InputManager getInputManager(){ return input; }

    @Override
    public void show() {
    }

    @Override
    public void render(float delta) {

        input.update();
        currentState.input();
        currentState.update(delta);
        currentState.render(game.batch);

        // HUD draws on top of whichever state is active — except the bottom-left
        // stack, which the staging overlay owns while it's open and untucked.
        boolean overlayUp = currentState == skillSelectState && !skillSelectState.isTucked();

        game.batch.begin();
        if (!overlayUp) {
            chargeHud.render(game.batch, game.viewport, charge);
            slotsHud.render(game.batch, game.viewport, playState.getPlayer().getSlots());
            basicAttackHud.render(game.batch, game.viewport, playState.getPlayer());
            lifeBarHud.render(game.batch, game.viewport, playState.getPlayer());
        }
        fpsHud.render(game.batch, game.viewport, game.font, delta);
        game.batch.end();
    }

    @Override
    public void resize(int width, int height) {
        if(width <= 0 || height <= 0) return;
        game.viewport.update(width, height, true);
        currentState.resize(width, height);
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
        playState.dispose();
        skillSelectState.dispose();
    }
}
