package com.silverignis.systems.combat;

public enum StatusType {
    FREEZE(false),
    BURN(false),
    POISON(false),
    STUN(false),
    REGEN(true),
    SHIELD(true),
    POWER_UP(true),
    MAGIC_UP(true),
    COMBUSTION(false),
    FROSTBITE(false),
    VENOM(false),
    CONDUCTIVE(false),
    GRAVITY(false),
    METABOLIZE(true),
    CHILL(true),
    MIASMA(false),
    SAP(false);

    private final boolean beneficial;

    StatusType(boolean beneficial) { this.beneficial = beneficial; }

    public boolean isBeneficial() { return beneficial; }
}
