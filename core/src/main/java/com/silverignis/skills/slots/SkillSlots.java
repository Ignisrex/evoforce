package com.silverignis.skills.slots;

import com.silverignis.skills.Skill;

import java.util.EnumMap;

/**
 * The full X/Y/B trio of {@link ButtonSlot}s. Owned by the {@code GameScreen}
 * for the duration of a battle. Slots persist across stagings: opening the
 * menu lets the player top up but doesn't wipe what's already loaded.
 */
public class SkillSlots {
    private int slotCapacity = 6;
    private final EnumMap<SlotKey, ButtonSlot> slots = new EnumMap<>(SlotKey.class);

    public SkillSlots() {
        for (SlotKey key : SlotKey.values()) {
            slots.put(key, new ButtonSlot(key));
        }
    }

    public ButtonSlot get(SlotKey key) {
        return slots.get(key);
    }

    /** {@code true} if the given skill is currently staged in any slot. */
    public boolean contains(Skill skill) {
        for (ButtonSlot slot : slots.values()) {
            for (Skill s : slot.view()) {
                if (s == skill) return true;
            }
        }
        return false;
    }

    public void clearAll() {
        for (ButtonSlot slot : slots.values()) slot.clear();
    }

    public boolean isFull() {
        return getSlotsUsed() >= slotCapacity;
    }

    public int getSlotsUsed(){
        int count = 0;
        for(ButtonSlot slot: slots.values()){
            count += slot.size();
        }
        return count;
    }

    public int getSlotCapacity(){ return this.slotCapacity; }
}
