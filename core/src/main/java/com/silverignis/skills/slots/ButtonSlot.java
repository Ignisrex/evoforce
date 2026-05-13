package com.silverignis.skills.slots;

import com.silverignis.skills.Skill;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * A single button's queue. FIFO: skills assigned first fire first.
 * Capacity is fixed at {@link #CAPACITY}; once full,
 * {@link #add(Skill)} returns {@code false}.
 */
public class ButtonSlot {

    public static final int CAPACITY = 2;

    private final SlotKey key;
    private final Deque<Skill> queue = new ArrayDeque<>(CAPACITY);

    public ButtonSlot(SlotKey key) {
        this.key = key;
    }

    public SlotKey getKey()     { return key; }
    public boolean isEmpty()    { return queue.isEmpty(); }
    public boolean isFull()     { return queue.size() >= CAPACITY; }
    public int     size()       { return queue.size(); }

    /** Front of the queue without consuming. */
    public Skill peek() {
        return queue.peekFirst();
    }

    /** Pop the front. Returns {@code null} if empty. */
    public Skill pop() {
        return queue.pollFirst();
    }

    /** Add to the back. Returns {@code false} if the slot is already full. */
    public boolean add(Skill skill) {
        if (isFull()) return false;
        queue.addLast(skill);
        return true;
    }

    /** Pop the most recently added. Used by the staging menu's "undo". */
    public Skill removeLast() {
        return queue.pollLast();
    }

    public void clear() {
        queue.clear();
    }

    /** Read-only snapshot for HUD rendering. */
    public List<Skill> view() {
        return Collections.unmodifiableList(new java.util.ArrayList<>(queue));
    }
}
