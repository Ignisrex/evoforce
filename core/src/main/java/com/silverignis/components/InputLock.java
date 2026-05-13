package com.silverignis.components;

/**
 * Tiny component that gates an entity's input. While locked by an "owner"
 * (typically a {@code SkillInstance} that's currently moving the entity
 * around the grid), the entity's normal input handlers should early-return
 * so they don't fight the skill for control.
 *
 * <p>The owner is stored as {@link Object} on purpose: components live in
 * the {@code components} package and shouldn't depend on
 * {@code skills.SkillInstance}. Identity comparison is the only thing
 * that matters here.
 */
public class InputLock {

    private Object owner;

    /** {@code true} while a skill (or any other system) is holding the lock. */
    public boolean isLocked() {
        return owner != null;
    }

    /**
     * Acquire the lock for {@code newOwner}. Idempotent for the same owner.
     * Refuses to take the lock if a different owner is already holding it,
     * which keeps overlapping skills from clobbering each other; the caller
     * should check {@link #isLocked()} first.
     */
    public boolean lock(Object newOwner) {
        if (owner != null && owner != newOwner) return false;
        owner = newOwner;
        return true;
    }

    /**
     * Release the lock if {@code currentOwner} is the one holding it.
     * No-op otherwise — protects against a stale skill releasing a lock
     * a newer skill has already taken.
     */
    public void unlock(Object currentOwner) {
        if (owner == currentOwner) owner = null;
    }
}
