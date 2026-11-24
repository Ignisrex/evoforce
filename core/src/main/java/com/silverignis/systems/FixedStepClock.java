package com.silverignis.systems;

public final class FixedStepClock {

    public static final double STEP = 1.0/ 60.0;
    public static final float DT = (float) STEP;
    private static final int MAX_STEPS = 5;

    private double accumulator;
    private long tick;

    public void add(float delta) {
        accumulator += Math.min(delta, MAX_STEPS * STEP);
    }

    public boolean step() {
        if (accumulator < STEP) return false;
        accumulator -= STEP;
        tick++;
        return true;
    }

    public long tick() { return tick; }

    /** 0..1: how far the render is past the last tick. Presentation only. */
    public float alpha() { return (float) (accumulator /STEP); }
}
