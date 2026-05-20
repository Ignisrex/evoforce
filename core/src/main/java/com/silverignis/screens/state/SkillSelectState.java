package com.silverignis.screens.state;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.silverignis.entities.Player;
import com.silverignis.input.GameAction;
import com.silverignis.screens.GameScreen;
import com.silverignis.skills.Skill;
import com.silverignis.skills.slots.ButtonSlot;
import com.silverignis.skills.slots.SlotKey;
import com.silverignis.ui.SkillSelectOverlay;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class SkillSelectState implements GameScreenState {

    private static int HAND_SIZE = 6;

    private final GameScreen screen;
    private final SkillSelectOverlay overlay = new SkillSelectOverlay();

    private List<Skill> hand;
    private int cursor;

    private final Map<SlotKey, List<Skill>> slotsSnapshot = new EnumMap<>(SlotKey.class);

    public SkillSelectState(GameScreen screen) {
        this.screen = screen;
    }

    @Override
    public void onEnter() {
        snapshotSlots();
        Player p = screen.playState.getPlayer();
        hand = new ArrayList<>(p.getDeck().drawHand(HAND_SIZE, p.getSlots()));
        cursor = 0;
        overlay.show(hand);
    }

    @Override
    public void onExit() {
        overlay.hide();
        slotsSnapshot.clear();
    }

    @Override
    public void input() {
        var input = screen.getInputManager();

        if (!hand.isEmpty()) {
            if (input.isActionJustPressed(GameAction.MOVE_LEFT))  cursor = Math.max(0, cursor - 1);
            if (input.isActionJustPressed(GameAction.MOVE_RIGHT)) cursor = Math.min(hand.size() - 1, cursor + 1);

            if (input.isActionJustPressed(GameAction.SKILL_X)) tryAssign(SlotKey.X);
            if (input.isActionJustPressed(GameAction.SKILL_Y)) tryAssign(SlotKey.Y);
            if (input.isActionJustPressed(GameAction.SKILL_B)) tryAssign(SlotKey.B);
        }

        if (input.isActionJustPressed(GameAction.SKILL_SELECT_CONFIRM)) confirm();
        else if (input.isActionJustPressed(GameAction.SKILL_SELECT_CANCEL)) cancel();
    }

    @Override
    public void update(float delta) {
        overlay.update(delta, cursor);
    }

    @Override
    public void render(SpriteBatch batch) {
        ScreenUtils.clear(Color.BLACK);
        screen.game.viewport.apply();
        batch.setProjectionMatrix(screen.game.viewport.getCamera().combined);
        batch.begin();
        screen.playState.renderWorld(batch);
        overlay.render(batch, screen.game.viewport);
        batch.end();
    }

    public void dispose() { overlay.dispose(); }

    private void snapshotSlots(){
        var slots = screen.playState.getPlayer().getSlots();
        slotsSnapshot.clear();
        for(SlotKey key: SlotKey.values()){
            slotsSnapshot.put(key, new ArrayList<>(slots.get(key).view()));
        }
    }

    private void restoreSnapshot() {
        var slots = screen.playState.getPlayer().getSlots();
        for (SlotKey key: SlotKey.values()) {
            ButtonSlot slot = slots.get(key);
            slot.clear();
            for (Skill s : slotsSnapshot.get(key)){
                slot.add(s);
            }
        }
    }

    private void tryAssign(SlotKey key){
        // Guard order: nothing to assign from an empty hand, and a full slot
        // can't accept more. NOTE: do NOT bail when the slot is empty —
        // that's exactly when we most need to put a skill into it.
        if (hand.isEmpty()) return;
        ButtonSlot slot = screen.playState.getPlayer().getSlots().get(key);
        if (slot.isFull()) return;

        Skill picked = hand.remove(cursor);
        slot.add(picked);

        if (cursor >= hand.size()) cursor = Math.max(0, hand.size() - 1);

        overlay.show(hand);
    }

    private void cancel(){
        restoreSnapshot();
        screen.setState(screen.playState);
    }

    private void confirm() {
        screen.charge.consume();
        screen.setState(screen.playState);
    }
}
