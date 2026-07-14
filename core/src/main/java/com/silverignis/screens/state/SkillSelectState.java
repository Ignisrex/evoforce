package com.silverignis.screens.state;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.silverignis.entities.Player;
import com.silverignis.input.GameAction;
import com.silverignis.screens.GameScreen;
import com.silverignis.skills.Skill;
import com.silverignis.skills.slots.ButtonSlot;
import com.silverignis.skills.slots.SkillSlots;
import com.silverignis.skills.slots.SlotKey;
import com.silverignis.ui.SkillSelectOverlay;

import java.util.*;

public class SkillSelectState implements GameScreenState {

    private static int HAND_SIZE = 6;

    private final GameScreen screen;
    private final SkillSelectOverlay overlay;

    private List<Skill> hand;
    private int cursor;
    private boolean inSlots;
    private int slotCursor;
    private boolean tucked;
    private boolean exiting; // overlay is playing its exit animation; input is done

    private final Map<SlotKey, List<Skill>> slotsSnapshot = new EnumMap<>(SlotKey.class);
    private final Deque<SlotKey> selectHistory = new ArrayDeque<SlotKey>();

    public SkillSelectState(GameScreen screen) {
        this.screen = screen;
        this.overlay = new SkillSelectOverlay(screen.game.generated, screen.game.viewport, screen.game.batch);
    }

    @Override
    public void onEnter() {
        inSlots = false;
        slotCursor = 0;
        tucked = false;
        exiting = false;

        snapshotSlots();
        Player p = screen.playState.getPlayer();
        hand = new ArrayList<>(p.getDeck().drawHand(HAND_SIZE, p.getSlots()));
        cursor = 0;
        overlay.onShow();
    }

    @Override
    public void onExit() {
        selectHistory.clear();
        slotsSnapshot.clear();
    }

    @Override
    public void input() {
        if (exiting) return;
        var input = screen.getInputManager();

        if (input.isActionJustPressed(GameAction.SKILL_SELECT_TUCK)) tucked = !tucked;
        if (tucked) return; //view-only mode so all other input commands are ignored

        if (input.isActionJustPressed(GameAction.MOVE_UP)) inSlots = true;
        if (input.isActionJustPressed(GameAction.MOVE_DOWN)) inSlots = false;

        if (input.isActionJustPressed(GameAction.MOVE_LEFT)) moveCursor(-1);
        if (input.isActionJustPressed(GameAction.MOVE_RIGHT)) moveCursor(+1);

        if (input.isActionJustPressed(GameAction.SKILL_X)) tryAssign(SlotKey.X);
        if (input.isActionJustPressed(GameAction.SKILL_Y)) tryAssign(SlotKey.Y);
        if (input.isActionJustPressed(GameAction.SKILL_B)) tryAssign(SlotKey.B);

        if (input.isActionJustPressed(GameAction.SKILL_SELECT_UNDO)) tryUndo();

        if (input.isActionJustPressed(GameAction.SKILL_SELECT_CONFIRM)){ confirm();
        }else if (input.isActionJustPressed(GameAction.SKILL_SELECT_CANCEL)) cancel();
    }

    private void moveCursor(int d) {
        if( inSlots ) {
            slotCursor = Math.clamp(slotCursor + d, 0, SlotKey.values().length - 1);
        } else if(!hand.isEmpty()) {
            cursor = Math.clamp(cursor + d, 0, hand.size() - 1);
        }
    }

    private void tryUndo() {
        SlotKey key;
        if (inSlots) {
            key = SlotKey.values()[slotCursor];
        } else if(!selectHistory.isEmpty()) {
            key = selectHistory.pop();
        }else{
            return;
        }

        Skill s = screen.playState.getPlayer().getSlots().get(key).pop(); // top = most recently staged
        if (s == null) return;
        if (inSlots) selectHistory.removeFirstOccurrence(key);
        hand.add(s);
    }



    @Override
    public void update(float delta) {
        if (!exiting) {
            Player p = screen.playState.getPlayer();
            overlay.refresh(hand, cursor, inSlots, slotCursor, p.getSlots(),
                    highlighted(), tucked, screen.charge.getFillRatio());
        }
        overlay.act(delta);
    }

    @Override
    public void render(SpriteBatch batch) {
        screen.playState.render(batch);
        overlay.draw();
    }

    public void dispose() {
        overlay.dispose();
    }

    private Skill highlighted() {
        if (inSlots) {
            List<Skill> q = screen.playState.getPlayer().getSlots().get(SlotKey.values()[slotCursor]).view();
            return q.isEmpty() ? null : q.get(0); // top of the stack
        }
        return (cursor >= 0 && cursor < hand.size()) ? hand.get(cursor) : null;
    }

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
            // Snapshot is in top→bottom order; add() pushes on top, so
            // re-stack bottom-up to reproduce the original order.
            List<Skill> saved = slotsSnapshot.get(key);
            for (int i = saved.size() - 1; i >= 0; i--) {
                slot.add(saved.get(i));
            }
        }
    }

    private void tryAssign(SlotKey key){
        // Guard order: nothing to assign from an empty hand, and a full slot
        // can't accept more. NOTE: do NOT bail when the slot is empty —
        // that's exactly when we most; need to put a skill into it.
        if (hand.isEmpty()) return;
        SkillSlots slots = screen.playState.getPlayer().getSlots();
        if (slots.isFull()) return;

        ButtonSlot slot = slots.get(key);

        Skill picked = hand.remove(cursor);
        slot.add(picked);
        selectHistory.push(key);

        if (cursor >= hand.size()) cursor = Math.max(0, hand.size() - 1);
    }

    private void cancel(){
        restoreSnapshot();
        exit();
    }

    private void confirm() {
        screen.charge.consume();
        overlay.confirmFlourish(screen.playState.getPlayer().getSlots());
        exit();
    }

    /** Play the overlay's exit animation, then hand back to PlayState. */
    private void exit() {
        exiting = true;
        overlay.onHide(() -> screen.setState(screen.playState));
    }

    public boolean isTucked(){ return tucked; }
}
