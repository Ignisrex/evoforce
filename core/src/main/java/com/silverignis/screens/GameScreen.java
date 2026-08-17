package com.silverignis.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.silverignis.Main;
import com.silverignis.components.Team;
import com.silverignis.entities.Enemy;
import com.silverignis.entities.Player;
import com.silverignis.input.InputManager;
import com.silverignis.registry.Monster;
import com.silverignis.screens.state.GameScreenState;
import com.silverignis.screens.state.PlayState;
import com.silverignis.screens.state.SkillSelectState;
import com.silverignis.systems.Encounter;
import com.silverignis.systems.SpawnSystem;
import com.silverignis.ui.*;

import java.util.List;

public class GameScreen implements Screen {

    public Main game;
    private final InputManager input = InputManager.defaultSetup();

    //States
    public final PlayState playState;
    public final SkillSelectState skillSelectState;
    private GameScreenState currentState;

    /** The battle itself. Owned here rather than by PlayState because
     *  SkillSelectState and the HUDs read it too. */
    public final Encounter encounter;

    //HUDs — Scene2D actors on hudStage sharing one SDF shader; BasicAttackHud
    //still draws immediate-mode, FpsHud owns nothing.
    private final RoundedRectShader hudShader;
    private final Stage hudStage;
    private final ManaBarHud manaBarHud;
    private final SlotsHud slotsHud;
    private final LifeBarHud lifeBarHud;
    private final StatusHud statusHud;
    private final FpsHud fpsHud = new FpsHud();
    private final BasicAttackHud basicAttackHud;

    public GameScreen(Main game){
        this.game = game;

        Player player = new Player(1, 1,
            game.monsterRegistry.getAnimSet(Monster.BEASTKIN, Team.PLAYER),
            game.session.playerProfile.getCaster(),
            game.session.playerProfile.getStats());
        List<Enemy> enemies = new SpawnSystem(game.session.spawnTable,
                                              game.monsterRegistry,
                                              game.session.skills)
            .spawnNext(game.session.playerProfile.getProgressionLevel());
        this.encounter = Encounter.create(player, enemies,
                                          game.session.playerProfile.getManaStats(),
                                          game.particles);

        Texture pixel = game.generated.pixel();
        this.hudShader      = new RoundedRectShader(pixel);
        this.manaBarHud     = new ManaBarHud(hudShader);
        this.slotsHud       = new SlotsHud(hudShader, game.viewport);
        this.lifeBarHud     = new LifeBarHud(hudShader);
        this.statusHud      = new StatusHud(hudShader, game.viewport);
        this.basicAttackHud = new BasicAttackHud(game.generated);

        this.hudStage = new Stage(game.viewport, game.batch);
        hudStage.addActor(slotsHud);

        Table bars = new Table();
        bars.setRound(false); // world-unit stage; see SkillSelectOverlay.newTable()
        bars.setFillParent(true);
        bars.top().left().padTop(LifeBarHud.MARGIN_TOP).padLeft(LifeBarHud.MARGIN_LEFT);
        bars.defaults().left();
        bars.add(lifeBarHud).size(LifeBarHud.BAR_WIDTH, LifeBarHud.BAR_HEIGHT).row();
        bars.add(manaBarHud).size(LifeBarHud.BAR_WIDTH, ManaBarHud.BAR_HEIGHT)
            .padTop(ManaBarHud.GAP_BELOW_LIFE).row();
        bars.add(statusHud).size(LifeBarHud.BAR_WIDTH, StatusHud.BAR_HEIGHT)
            .padTop(StatusHud.GAP_BELOW_MANA);
        hudStage.addActor(bars);

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
            basicAttackHud.render(game.batch, game.viewport, playState.getPlayer());
        }
        fpsHud.render(game.batch, game.viewport, game.font, delta);
        game.batch.end();

        // Stage manages its own begin()/end(), so it draws outside the block above.
        if (!overlayUp) {
            slotsHud.refresh(playState.getPlayer().getSlots());
            lifeBarHud.refresh(playState.getPlayer());
            manaBarHud.refresh(encounter.mana());
            statusHud.refresh(playState.getPlayer());
            hudStage.act(delta);
            hudStage.draw();
        }
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
        hudStage.dispose(); // external batch — stage doesn't dispose it
        slotsHud.dispose();
        statusHud.dispose();
        hudShader.dispose();
    }
}
