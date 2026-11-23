package com.silverignis.screens.state;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.silverignis.entities.Player;
import com.silverignis.input.GameAction;
import com.silverignis.input.InputEvent;
import com.silverignis.input.InputFrame;
import com.silverignis.screens.GameScreen;
import com.silverignis.skills.Skill;
import com.silverignis.skills.elements.Element;
import com.silverignis.skills.keywords.Keywords;
import com.silverignis.skills.slots.ButtonSlot;
import com.silverignis.skills.slots.SkillSlots;
import com.silverignis.skills.slots.SlotKey;
import com.silverignis.traits.Trait;
import com.silverignis.ui.SkillSelectOverlay;

import java.util.*;

public class SkillSelectState implements GameScreenState {

    private static int HAND_SIZE = 6;

    private final GameScreen screen;
    private final SkillSelectOverlay overlay;

    private enum Zone { HAND, DETAIL, STACKS, OPERATOR }

    private List<Skill> hand;
    private int cursor;
    private Zone zone;
    private boolean traitTrayOpen;
    private int traitCursor;
    private int slotCursor;
    private boolean tucked;
    private boolean exiting; // overlay is playing its exit animation; input is done

    private final Map<SlotKey, List<Skill>> slotsSnapshot = new EnumMap<>(SlotKey.class);

    private record Pick(SlotKey slot, int costPaid, int chillUsed) {}
    private final Deque<Pick> selectHistory = new ArrayDeque<>();
    private int chillUsedTotal;

    private int pendingCost;

    public SkillSelectState(GameScreen screen) {
        this.screen = screen;
        this.overlay = new SkillSelectOverlay(screen.game.generated, screen.game.viewport, screen.game.batch);
    }

    @Override
    public void onEnter() {
        zone = Zone.HAND;
        traitTrayOpen = false;
        traitCursor = 0;
        slotCursor = 0;
        tucked = false;
        exiting = false;
        pendingCost = 0;
        chillUsedTotal = 0;

        Player p = screen.playState.getPlayer();
        // Bargain-returned cards don't survive a staging entry — expire before
        // the snapshot so they can't be laundered back through the hand.
        Keywords.expireBargains(screen.encounter.skillVars(), p);
        snapshotSlots();
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
    public void input(InputFrame frame) {
        for (InputEvent inputEvent : frame.events()) {
            if (exiting) return;
            if (inputEvent.type() == InputEvent.Type.PRESSED) press(inputEvent.action());
        }
    }

    private void press(GameAction action) {
        if (action == GameAction.SKILL_SELECT_TUCK) {
            tucked = !tucked;
            return;
        }
        if (tucked) return; //view-only mode so all other input commands are ignored

        if (traitTrayOpen) {
            List<Trait> traits = screen.playState.getPlayer().getCaster().getTraits().all();
            int max = Math.max(0, traits.size() - 1);
            switch (action) {
                case MOVE_LEFT -> traitCursor = Math.clamp(traitCursor - 1, 0, max);
                case MOVE_RIGHT -> traitCursor = Math.clamp(traitCursor + 1, 0, max);
                case SKILL_SELECT_CONFIRM, SKILL_SELECT_CANCEL -> traitTrayOpen = false;
                default -> {
                }
            }
            return; // modal: swallow everything else — especially cancel's mana drain
        }

        switch (action) {
            // Spatial navigation: UP/DOWN walks the left column (hand -> detail -> operator);
            // LEFT/RIGHT jumps between the column and the stacks on the right (see moveCursor).
            case MOVE_UP -> {
                if (zone == Zone.HAND) zone = Zone.DETAIL;
                else if (zone == Zone.DETAIL) zone = Zone.OPERATOR;
            }

            case MOVE_DOWN -> {
                if (zone == Zone.OPERATOR) zone = Zone.DETAIL;
                else if (zone == Zone.DETAIL || zone == Zone.STACKS) zone = Zone.HAND;
            }

            case MOVE_LEFT -> moveCursor(-1);
            case MOVE_RIGHT -> moveCursor(+1);

            case SKILL_X -> tryAssign(SlotKey.X);
            case SKILL_Y -> tryAssign(SlotKey.Y);
            case SKILL_B -> tryAssign(SlotKey.B);

            case SKILL_SELECT_UNDO -> tryUndo();

            case SKILL_SELECT_CONFIRM -> {
                if (zone == Zone.OPERATOR) {
                    traitTrayOpen = true;
                    traitCursor = 0;
                } else  confirm();
            }
            case SKILL_SELECT_CANCEL -> cancel();
            default -> {}
        }
    }

    private void moveCursor(int d) {
        switch (zone) {
            case HAND -> {
                if (!hand.isEmpty()) cursor = Math.clamp(cursor + d, 0, hand.size() - 1);
            }
            case STACKS -> {
                if (d < 0 && slotCursor == 0) zone = Zone.DETAIL; // walk off the left edge
                else slotCursor = Math.clamp(slotCursor + d, 0, SlotKey.values().length - 1);
            }
            case DETAIL, OPERATOR -> {
                if (d > 0) zone = Zone.STACKS;
            }
        }
    }

    private void tryUndo() {
        if (zone == Zone.OPERATOR) return;

        SlotKey key;
        if (zone == Zone.STACKS) key = SlotKey.values()[slotCursor];
        else if (!selectHistory.isEmpty()) key = selectHistory.peek().slot();
        else return;

        ButtonSlot slot = screen.playState.getPlayer().getSlots().get(key);
        Skill skill = slot.pop();
        if (skill == null) return;

        Pick pick = null;
        for (Pick p : selectHistory) if (p.slot() == key) { pick = p; break; }
        if (pick != null) {
            selectHistory.remove(pick);
            pendingCost -= pick.costPaid();
            chillUsedTotal -= pick.chillUsed();
        }
        hand.add(skill);
    }



    @Override
    public void update(float delta) {
        if (!exiting) {
            Player p = screen.playState.getPlayer();
            overlay.refresh(hand, cursor, zone == Zone.STACKS, slotCursor, p.getSlots(),
                    highlighted(), tucked,
                    screen.encounter.mana().getCurrent() - pendingCost, screen.encounter.mana().getMax(),
                    p.getCaster().getTraits().all(), zone == Zone.DETAIL, zone == Zone.OPERATOR,
                    traitTrayOpen, traitCursor);
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
        if (zone == Zone.OPERATOR) return null;
        if (zone == Zone.STACKS) {
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

    private boolean isPrepaid(Skill s) {
        for (List<Skill> saved : slotsSnapshot.values()){
            if (saved.contains(s)) return true;
        }
        return false;
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
        if (zone == Zone.OPERATOR) return;
        if (hand.isEmpty()) return;
        SkillSlots slots = screen.playState.getPlayer().getSlots();
        if (slots.isFull()) return;

        ButtonSlot slot = slots.get(key);

        Skill picked = hand.get(cursor);

        int cost = isPrepaid(picked) ? 0 : picked.getManaCost();
        int chillUsed = 0;
        if(picked.getElement() == Element.ICE) {
            chillUsed = Math.clamp(
                Keywords.chillStacks(screen.playState.getPlayer()) - chillUsedTotal, 0, cost);
            cost -= chillUsed;
        }
        float shortFall = pendingCost + cost - screen.encounter.mana().getCurrent();
        if (shortFall > picked.getDebt()) return;

        hand.remove(cursor);
        slot.add(picked);
        selectHistory.push(new Pick(key, cost, chillUsed));
        pendingCost += cost;
        chillUsedTotal += chillUsed;

        if (cursor >= hand.size()) cursor = Math.max(0, hand.size() - 1);
    }

    /** Backing out costs the whole bar — reading your hand and redrawing is not free. */
    private void cancel(){
        screen.encounter.mana().drainAll();
        restoreSnapshot();
        exit();
    }

    /** Committing costs the whole bar, not the staged total: mana is the budget
     *  that limits what can be staged, not a per-card price. */
    private void confirm() {
        Keywords.consumeChill(screen.playState.getPlayer(), chillUsedTotal);
        screen.encounter.mana().commitStaged(pendingCost);
        overlay.confirmFlourish(screen.playState.getPlayer().getSlots());
        exit();
    }

    /** Play the overlay's exit animation, then hand back to PlayState.**/
    private void exit() {
        screen.playState.getPlayer().getCaster().tickDeck(1f);
        exiting = true;
        overlay.onHide(() -> screen.setState(screen.playState));
    }

    public boolean isTucked(){ return tucked; }
}
