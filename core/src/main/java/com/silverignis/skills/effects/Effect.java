package com.silverignis.skills.effects;

import com.silverignis.systems.combat.StatusType;

public class Effect {

    private final EffectType type;

    private final int value;
    private final float duration;
    private final int chance;
    private final StatusType statusType;

    private Effect(EffectType type, int value, float duration, int chance, StatusType statusType){

        this.type = type;
        this.value = value;
        this.duration = duration;
        this.chance = chance;
        this.statusType = statusType;
    }

    public static Effect damage(int amount){
        return new Effect(EffectType.DAMAGE, amount, 0f, 100, null);
    }

    public static Effect heal(int amount){
        return new Effect(EffectType.HEAL, amount, 0f, 100, null);
    }

    public static Effect applyStatus(StatusType status, float duration, int chance, int dotMagnitude){
        return new Effect(EffectType.APPLY_STATUS, dotMagnitude, duration, chance, status);
    }

    public static Effect knockback(int tiles){
        return new Effect(EffectType.KNOCKBACK, tiles, 0f, 100, null);
    }

    public EffectType getType() {
        return type;
    }

    public int getValue() {
        return value;
    }

    public float getDuration() {
        return duration;
    }

    public int getChance() {
        return chance;
    }

    public StatusType getStatusType() { return statusType; }

}
