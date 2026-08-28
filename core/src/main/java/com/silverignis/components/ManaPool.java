package com.silverignis.components;

/**
 * One battle's mana bar. Fills over time and gates how much can be staged in
 * the skill menu; using the menu costs the bar rather than a per-card price.
 *
 * Battle-scoped and owned by the Encounter: a fresh pool starts full by
 * construction, so nothing has to remember to refill it at the start of a fight.
 * Capacity and regen come from the run-level {@link ManaStats}.
 */
public final class ManaPool {

    private final ManaStats stats;
    private float current;

    public ManaPool(ManaStats stats) {
        this.stats = stats;
        this.current = stats.getMax();   // a battle opens with a full bar
    }

    public int   getMax()     { return stats.getMax(); }
    public float getCurrent() { return current; }

    public void update(float delta) {
        current = Math.min(stats.getMax(), current + stats.getRegenPerSecond() * delta);
    }

    /** Committing to a staged loadout costs the whole bar, whatever it held. */
    public void drain() { current = 0f; }

    /** Backing out still costs — half the bar, so reading your hand and
     *  cancelling to redraw is not free. */
    public void drainHalf() { current *= 0.5f; }
}
