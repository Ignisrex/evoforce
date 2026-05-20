package com.silverignis.systems.combat;

import com.silverignis.components.*;

public interface Combatant {
    Health getHealth();
    Stats getStats();
    Caster getCaster();
    GridPosition getGridPosition();
    Team getTeam();
    StatusContainer getStatusContainer();

    int getCol();
    int getRow();
    float getVisualX();
    float getVisualY();
    float getDepthScale();

    boolean isAlive();
    boolean isDead();
    boolean isInputLocked();

    void onHitFlash();
    void onDeath();

}
