package com.silverignis.animation;

public enum AnimState {
    IDLE(1, true),
    MOVE(2, false),
    ATTACK(3, false),
    CAST(3, true),
    HURT(4, false),
    DEATH(5, false);

    private final int priority;
    private final boolean loops;

    AnimState(int priority, boolean loops){
        this.priority = priority;
        this.loops = loops;
    }

    public int priority() { return priority; }
    public boolean loops() { return loops; }

    /** Asset subfolder for this state's split sheets, e.g. IDLE -> "idle". */
    public String assetDir() { return name().toLowerCase(java.util.Locale.ROOT); }

}
