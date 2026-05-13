package com.silverignis.skills;

public final class ProjectileConfig implements ShapeConfig {

    public enum MovementType { STRAIGHT, LOB }

    private final MovementType movementType;
    private final float speed;
    private final int targetRange;   // tiles ahead of caster (LOB only)
    private final float arcHeight;   // world units above the baseline (LOB only)

    private ProjectileConfig(MovementType movementType, float speed,
                             int targetRange, float arcHeight) {
        this.movementType = movementType;
        this.speed = speed;
        this.targetRange = targetRange;
        this.arcHeight = arcHeight;
    }

    public static ProjectileConfig straight(float speed) {
        return new ProjectileConfig(MovementType.STRAIGHT, speed, 0, 0f);
    }

    public static ProjectileConfig lob(int targetRange, float arcHeight) {
        return new ProjectileConfig(MovementType.LOB, 0f, targetRange, arcHeight);
    }

    public MovementType getMovementType() { return movementType; }
    public float        getSpeed()        { return speed; }
    public int          getTargetRange()  { return targetRange; }
    public float        getArcHeight()    { return arcHeight; }
}
