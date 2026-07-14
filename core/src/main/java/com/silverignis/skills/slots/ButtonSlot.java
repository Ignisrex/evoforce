package com.silverignis.skills.slots;

import com.silverignis.skills.Skill;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * A single button's stack. LIFO: the last skill staged sits on top and
 * fires first; earlier picks sink underneath. Capacity is fixed at
 * {@link #CAPACITY}; once full, {@link #add(Skill)} returns {@code false}.
 */
public class ButtonSlot {

    private final SlotKey key;
    private final Deque<Skill> stack = new ArrayDeque<>();

    public ButtonSlot(SlotKey key) {
        this.key = key;
    }

    public SlotKey getKey()     { return key; }
    public boolean isEmpty()    { return stack.isEmpty(); }
    public int     size()       { return stack.size(); }

    /** Top of the stack without consuming. */
    public Skill peek() {
        return stack.peekFirst();
    }

    /** Pop the top. Returns {@code null} if empty. */
    public Skill pop() {
        return stack.pollFirst();
    }

    /** Push on top. Returns {@code false} if the slot is already full. */
    public boolean add(Skill skill) {
        stack.addFirst(skill);
        return true;
    }

    public void clear() {
        stack.clear();
    }

    /** Read-only snapshot for HUD rendering. */
    public List<Skill> view() {
        return Collections.unmodifiableList(new java.util.ArrayList<>(stack));
    }
}
