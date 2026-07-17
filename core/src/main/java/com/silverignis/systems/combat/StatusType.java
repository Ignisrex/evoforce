package com.silverignis.systems.combat;

public enum StatusType {
    FREEZE(false),
    BURN(false),
    POISON(false),
    STUN(false),
    REGEN(true),
    SHIELD(true),
    POWER_UP(true),
    MAGIC_UP(true);

    private final boolean beneficial;

    StatusType(boolean beneficial) { this.beneficial = beneficial; }

    public boolean isBeneficial() { return beneficial; }
}
