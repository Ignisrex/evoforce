package com.silverignis.screens;

import com.badlogic.gdx.Screen;
import com.silverignis.Main;
import com.silverignis.entities.Enemy;
import com.silverignis.entities.Player;
import com.silverignis.input.InputManager;
import com.silverignis.skills.ChargeMeter;
import com.silverignis.skills.Skill;
import com.silverignis.skills.SkillLibrary;
import com.silverignis.state.GameScreenState;
import com.silverignis.state.PlayState;
import com.silverignis.state.SkillSelectState;
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


    public final SkillLibrary skills = SkillLibrary.defaults();

    public final ChargeMeter charge = new ChargeMeter(/* max */ 1f, /* fillRate */ 0.20f);

    //HUDs
    private final ChargeBarHud chargeHud = new ChargeBarHud();
    private final SlotsHud slotsHud = new SlotsHud();
    private final FpsHud fpsHud = new FpsHud();
    private final LifeBarHud lifeBarHud = new LifeBarHud();
    private final BasicAttackHud basicAttackHud = new BasicAttackHud();

    public GameScreen(Main game){
        this.game = game;
        this.playState = new PlayState(this);
        this.skillSelectState = new SkillSelectState(this);

        // Seed the player's deck with every library skill except the
        // basic-attack skill — that one is held in the Caster's basic-attack
        // slot and fired by the dedicated ATTACK_BASIC button, so it should
        // never appear in the staging hand.
        Player player = playState.getPlayer();
        Skill  windSlash = skills.get("wind_slash");
        for (Skill s : skills.all()) {
            if (s == windSlash) continue;
            player.getDeck().add(s);
        }
        player.getCaster().setBasicAttack(windSlash);
        // Each enemy fires the same wind_slash via the unified pipeline (team-flipped).
        // No deck seeding for enemies — only their basic attack is used today.
        for (Enemy enemy : playState.getEnemies()) {
            enemy.getCaster().setBasicAttack(windSlash);
        }

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

        // HUD draws on top of whichever state is active.
        game.batch.begin();
        chargeHud.render(game.batch, game.viewport, charge);
        slotsHud.render(game.batch, game.viewport, playState.getPlayer().getSlots());
        basicAttackHud.render(game.batch, game.viewport, playState.getPlayer());
        lifeBarHud.render(game.batch, game.viewport, playState.getPlayer());
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
        chargeHud.dispose();
        slotsHud.dispose();
        basicAttackHud.dispose();
        lifeBarHud.dispose();
        skills.dispose();
    }
}
