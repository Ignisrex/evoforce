package com.silverignis.components;

/**
 * Run-level mana capacity and regeneration. Persists for the whole run and is
 * what rewards and traits upgrade.
 *
 * Split from {@link ManaPool} because the two have different lifetimes: these
 * numbers outlive every battle, while the pool's fill is meaningless outside
 * one. Keeping them in a single object meant whichever side owned it had the
 * other half in the wrong place.
 */
public final class ManaStats {

    private int max;
    private float regenPerSecond;

    public ManaStats(int max, float regenPerSecond) {
        this.max = max;
        this.regenPerSecond = regenPerSecond;
    }

    public int   getMax()            { return max; }
    public float getRegenPerSecond() { return regenPerSecond; }

    public void increaseMax(int amt)          { max += amt; }
    public void increaseRegenRate(float rate) { regenPerSecond += rate; }
}
