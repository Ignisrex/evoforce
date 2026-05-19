package com.silverignis.skills.effects;

public class Effect {

    private final Type type;

    private final int value;
    private final int duration;
    private final int chance;

    public Effect(Type type, int value, int duration, int chance){

        this.type = type;
        this.value = value;
        this.duration = duration;
        this.chance = chance;
    }

    public Type getType() {
        return type;
    }

    public int getValue() {
        return value;
    }

    public int getDuration() {
        return duration;
    }

    public int getChance() {
        return chance;
    }

    public enum Type {
        DAMAGE,
        POISON,
        BURN,
        STUN,
        FREEZE,
    }
}
