package com.silverignis.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.ScreenUtils;
import com.silverignis.Main;
import com.silverignis.input.GameAction;
import com.silverignis.input.InputManager;
import com.silverignis.particles.*;
import com.silverignis.render.RenderContext;
import com.silverignis.rewards.RewardOffer;
import com.silverignis.rewards.RewardOption;
import com.silverignis.ui.RewardCardStyle;
import com.silverignis.ui.TraceBorder;
import com.silverignis.ui.UiUtil;

import java.util.ArrayList;
import java.util.List;

public class RewardScreen implements Screen {
    private Main game;
    private final List<RewardOffer> offers;
    private final InputManager input = InputManager.defaultSetup();
    private final Stage stage;
    private final RewardCardStyle style;

    /** Reveal-ceremony beats; gather runs 0 → REWARD_GATHER_TIME, trace connects at MATERIALIZE_AT. */
    private static final float TRACE_AT = 0.5f, TRACE_TIME = 0.6f, MATERIALIZE_AT = 1.1f, CEREMONY_TIME = 1.35f;
    /** Deep arcane blue — the "world system" void behind the ambience particles. */
    private static final Color BG = new Color(0.02f, 0.045f, 0.11f, 1f);

    private int offerIndex = 0;
    private int selected = 0;
    private boolean exiting;
    private boolean ceremonyActive;

    private final ParticleEngine particles = new ParticleEngine();
    private final RenderContext particleCtx;
    private final List<Table> frames = new ArrayList<>();
    private final List<TraceBorder> traces = new ArrayList<>();
    private EmitterHandle ambient, selectWisps;

    public RewardScreen(Main game, List<RewardOffer> offers) {
        this.game = game;
        this.offers = offers;
        this.style = new RewardCardStyle(game.generated, game.viewport);
        this.stage = new Stage(game.viewport, game.batch);
        this.particleCtx = RenderContext.screenSpace(game.batch);

        buildOffer();
    }

    private void buildOffer() {
        selected = 0;
        stage.clear();
        frames.clear();
        traces.clear();
        RewardOffer offer = offers.get(offerIndex);

        Table root = UiUtil.newTable();
        root.setFillParent(true);
        stage.addActor(root);

        root.add(new Label(offer.title, style.title)).padBottom(0.15f).row();
        if(offers.size() > 1) {
            root.add(new Label((offerIndex + 1) + " / " + offers.size(), style.small)).padBottom(0.15f).row();
        }

        Table cardRow = UiUtil.newTable();
        for (RewardOption option : offer.options) {
            Table frame = UiUtil.newTable();
            frame.setBackground(style.cardBg);
            frame.pad(0.3f);
            frame.add(option.buildContents(style)).grow().top();
            frame.setTransform(true);
            cardRow.add(frame).size(3.8f, 4.8f).pad(0.25f);
            frames.add(frame);
        }
        root.add(cardRow).row();

        root.add(new Label("< / >  select    confirm  take    cancel  skip", style.small)).padTop(0.3f);

        stage.addActor(new ParticleLayer());
        root.validate();
        for (Table frame : frames) frame.setOrigin(frame.getWidth() / 2f, frame.getHeight() / 2f);

        particles.clear(Channel.MENU);
        ambient = Vfx.rewardAmbience().play(particles,
            Anchor.region(8f, 0f, 4.5f, 8f, 0f, 4.5f), Drive.FULL, Channel.MENU);
        selectWisps = null;

        // Reveal ceremony: gather vortex → frame trace → materialize, all cards in sync.
        ceremonyActive = true;
        for (int i = 0; i < frames.size(); i++) {
            Table frame = frames.get(i);
            RewardOption option = offer.options.get(i);
            frame.getColor().a = 0f;
            frame.setScale(0.9f);

            Vector2 c = frame.localToStageCoordinates(new Vector2(frame.getWidth() / 2f, frame.getHeight() / 2f));
            for (int arm = 0; arm < 4; arm++) {
                Vfx.rewardGatherArm(option.accent()).play(particles,
                    Anchor.spiralIn(c.x, 0f, c.y, 2.4f, 2.2f, Vfx.REWARD_GATHER_SPAWNS, arm * 90f),
                    Drive.FULL, Channel.MENU);
            }
            Vfx.rewardGatherGlyphs(option.accent()).play(particles,
                Anchor.spiralIn(c.x, 0f, c.y, 2.6f, 1.8f, Vfx.REWARD_GATHER_GLYPHS, 45f),
                Drive.FULL, Channel.MENU);
            TraceBorder trace = new TraceBorder(style.pixel, option.accent(), TRACE_TIME, TRACE_AT);
            trace.setBounds(c.x - frame.getWidth() / 2f, c.y - frame.getHeight() / 2f,
                frame.getWidth(), frame.getHeight());
            stage.addActor(trace);
            traces.add(trace);

            frame.addAction(Actions.sequence(
                Actions.delay(MATERIALIZE_AT),
                Actions.parallel(
                    Actions.fadeIn(0.15f, Interpolation.pow2Out),
                    Actions.scaleTo(1f, 1f, 0.2f, Interpolation.swingOut)),
                Actions.run(() -> revealBurst(frame, option))));
        }
        stage.addAction(Actions.sequence(Actions.delay(CEREMONY_TIME), Actions.run(this::finishCeremony)));
    }

    private void finishCeremony() {
        ceremonyActive = false;
        applySelection(0, false);
    }

    /** Snap the ceremony to its finished state — cards fully visible, effects cleared. */
    private void skipCeremony() {
        stage.getRoot().clearActions();
        for (TraceBorder trace : traces) trace.remove();
        traces.clear();
        for (Table frame : frames) {
            frame.clearActions();
            frame.getColor().a = 1f;
            frame.setScale(1f);
        }
        particles.clear(Channel.MENU);
        ambient = Vfx.rewardAmbience().play(particles,
            Anchor.region(8f, 0f, 4.5f, 8f, 0f, 4.5f), Drive.FULL, Channel.MENU);
        finishCeremony();
    }

    private void applySelection(int index, boolean pop) {
        frames.get(selected).setBackground(style.cardBg);
        selected = index;
        Table frame = frames.get(selected);
        frame.setBackground(style.cardBgSelected);

        if (selectWisps != null) selectWisps.stop();
        Vector2 center = frame.localToStageCoordinates(new Vector2(frame.getWidth() / 2f, frame.getHeight() / 2f));
        selectWisps = Vfx.rewardSelectWisps(offers.get(offerIndex).options.get(selected).accent())
            .play(particles,
                Anchor.region(center.x, 0f, center.y, frame.getWidth() * 0.4f, 0f, frame.getHeight() * 0.35f),
                Drive.FULL, Channel.MENU);

        if (pop) {
            frame.addAction(Actions.sequence(
                Actions.scaleTo(1.05f, 1.05f, 0.06f, Interpolation.pow2Out),
                Actions.scaleTo(1f, 1f, 0.09f, Interpolation.pow2In)));
        }
    }

    private void revealBurst(Table frame, RewardOption option) {
        Vector2 c = frame.localToStageCoordinates(new Vector2(frame.getWidth() / 2f, frame.getHeight() / 2f));
        Vfx.rewardRevealBurst(option.accent()).play(particles,
            Anchor.rim(c.x, 0f, c.y, frame.getWidth() / 2f, frame.getHeight() / 2f),
            Drive.FULL, Channel.MENU);
    }

    private void advance() {
        offerIndex++;
        if (offerIndex >= offers.size()) {
            // TEMP: ceremony-testing loop — re-roll another reward screen instead of
            // returning to the overworld (skip offers to loop forever). Remove before ship.
            RewardOffer next = RewardOffer.skillOffer(game.session);
            game.setScreen(next != null ? new RewardScreen(game, List.of(next))
                                        : new OverworldScreen(game));
        } else {
            buildOffer();
        }
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(BG);
        game.viewport.apply();

        input.update();
        if (ceremonyActive) {
            if (input.isActionJustPressed(GameAction.MOVE_LEFT)
                || input.isActionJustPressed(GameAction.MOVE_RIGHT)
                || input.isActionJustPressed(GameAction.SKILL_SELECT_CONFIRM)
                || input.isActionJustPressed(GameAction.SKILL_SELECT_CANCEL)) {
                skipCeremony();
            }
        } else if (!exiting) {
            int count = offers.get(offerIndex).options.size();
            if (input.isActionJustPressed(GameAction.MOVE_LEFT))  applySelection((selected + count - 1) % count, true);
            if (input.isActionJustPressed(GameAction.MOVE_RIGHT)) applySelection((selected + 1) % count, true);

            if (input.isActionJustPressed(GameAction.SKILL_SELECT_CONFIRM)) {
                offers.get(offerIndex).options.get(selected).apply(game.session);
                exitOffer(true);
            }
            if (input.isActionJustPressed(GameAction.SKILL_SELECT_CANCEL)) {
                exitOffer(false);
            }
        }

        particles.update(delta);
        stage.act(delta);
        stage.draw();
    }

    private void exitOffer(boolean claimed) {
        exiting = true;
        if (selectWisps != null) {
            selectWisps.stop();
            selectWisps = null;
        }

        for(int i = 0; i < frames.size(); i++) {
            if (claimed && i == selected) continue;
            frames.get(i).addAction(Actions.parallel(
                Actions.fadeOut(0.2f, Interpolation.pow2In),
                Actions.moveBy(0f, -0.4f, 0.2f, Interpolation.pow2In)));
        }

        float wait = 0.25f;
        if (claimed) {
            Table chosen = frames.get(selected);
            Vector2 c = chosen.localToStageCoordinates(new Vector2(chosen.getWidth() / 2f, chosen.getHeight() / 2f));
            Vfx.rewardConfirmBurst(offers.get(offerIndex).options.get(selected).accent()).play(particles,
                Anchor.region(c.x, 0f, c.y, chosen.getWidth() * 0.3f, 0f, chosen.getHeight() * 0.3f),
                Drive.FULL, Channel.MENU);
            chosen.addAction(Actions.sequence(
                Actions.scaleTo(1.08f, 1.08f, 0.12f, Interpolation.pow2Out),
                Actions.delay(0.25f),
                Actions.parallel(
                    Actions.fadeOut(0.18f, Interpolation.pow2In),
                    Actions.scaleTo(0.9f, 0.9f, 0.18f, Interpolation.pow2In))));
            wait = 0.6f;
        }

        stage.addAction(Actions.sequence(
            Actions.delay(wait),
            Actions.run(() -> { exiting = false; advance(); })));
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

    }

    @Override
    public void dispose() {
        stage.dispose();
        style.dispose();
    }

    private final class ParticleLayer extends Actor {
        @Override
        public void draw(Batch b, float parentAlpha) {
            for (Emitter e : particles.emitters()) e.render(particleCtx);
        }
    }
}
