package com.silverignis.skills.slots;

import com.silverignis.skills.Skill;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * A single button's stack. LIFO: the last skill staged sits on top and
 * fires first; earlier picks sink underneath. Unbounded — the shared
 * pool limit lives on {@link SkillSlots}.
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

    /** Push on top. */
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
