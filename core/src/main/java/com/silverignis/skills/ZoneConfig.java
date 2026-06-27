package com.silverignis.skills;

/**
 * Shape-specific configuration for {@code ZONE} skills.
 *
 * <p>Controls how long the zone stays active, how often it ticks, and whether
 * it pulls aligned opposing combatants inward (Void Pull) instead of only
 * affecting the single combatant standing on the zone tile.
 */
public final class ZoneConfig implements ShapeConfig {

    /** When true, each tick drags aligned foes one tile toward the zone tile. */
    public final boolean pull;
    /** Active-phase length in seconds. */
    public final float duration;
    /** Seconds between effect ticks. */
    public final float tickInterval;

    public ZoneConfig(boolean pull, float duration, float tickInterval) {
        this.pull = pull;
        this.duration = duration;
        this.tickInterval = tickInterval;
    }
}
