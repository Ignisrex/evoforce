package com.silverignis.skills;

public final class ProjectileConfig implements ShapeConfig {

    public enum MovementType { STRAIGHT, LOB }

    private static final float DEFAULT_ZONE_DURATION = 3.0f;
    private static final float DEFAULT_ZONE_TICK     = 0.5f;

    private final MovementType movementType;
    private final float speed;
    private final int rowSpan;           // rows covered, centered on the caster's (STRAIGHT only)
    private final int targetRange;       // tiles ahead of caster (LOB only)
    private final float arcHeight;       // world units above the baseline (LOB only)
    private final float zoneDuration;    // landing-cloud active time in seconds (LOB only)
    private final float zoneTickInterval;// landing-cloud tick interval in seconds (LOB only)

    private ProjectileConfig(MovementType movementType, float speed, int rowSpan,
                             int targetRange, float arcHeight,
                             float zoneDuration, float zoneTickInterval) {
        this.movementType = movementType;
        this.speed = speed;
        this.rowSpan = rowSpan;
        this.targetRange = targetRange;
        this.arcHeight = arcHeight;
        this.zoneDuration = zoneDuration;
        this.zoneTickInterval = zoneTickInterval;
    }

    public static ProjectileConfig straight(float speed) {
        return straight(speed, 1);
    }

    public static ProjectileConfig straight(float speed, int rowSpan) {
        return new ProjectileConfig(MovementType.STRAIGHT, speed, rowSpan, 0, 0f,
                DEFAULT_ZONE_DURATION, DEFAULT_ZONE_TICK);
    }

    public static ProjectileConfig lob(int targetRange, float arcHeight,
                                       float zoneDuration, float zoneTickInterval) {
        return new ProjectileConfig(MovementType.LOB, 0f, 1, targetRange, arcHeight,
                zoneDuration, zoneTickInterval);
    }

    public MovementType getMovementType()     { return movementType; }
    public float        getSpeed()            { return speed; }
    public int          getRowSpan()          { return rowSpan; }
    public int          getTargetRange()      { return targetRange; }
    public float        getArcHeight()        { return arcHeight; }
    public float        getZoneDuration()     { return zoneDuration; }
    public float        getZoneTickInterval() { return zoneTickInterval; }
}
