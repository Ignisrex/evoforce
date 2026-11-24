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
    private float debt;

    public ManaPool(ManaStats stats) {
        this.stats = stats;
        this.current = stats.getMax();   // a battle opens with a full bar
    }

    public int   getMax()     { return stats.getMax(); }
    public float getCurrent() { return current; }
    public float getDebt() { return debt; }

    public void update(float delta) {
        float gain = stats.getRegenPerSecond() * delta;
        float payment = Math.min(debt, gain);
        debt -= payment;
        current = Math.min(stats.getMax(), current + gain - payment);
    }

    public void commitStaged(int staged) {
        debt += Math.max(0f, staged - current);
        current = 0f;
    }

    /** Backing out forfeits the whole bar — a pass is a turn, so it costs
     *  the same clock as a real one. */
    public void drainAll() { current = 0f; }

    public boolean canOpenMenu() { return current >= stats.getMax() / 2f; }

    /** Spend exactly {@code cost} if the bar holds it. */
    public boolean trySpend(int cost) {
        if (current < cost) return false;
        current -= cost;
        return true;
    }
    public void recover(int amount) {
        float payment = Math.min(debt, amount);
        debt -= payment;
        current = Math.min(stats.getMax(), current + amount - payment);
    }
}
