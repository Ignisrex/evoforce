package com.silverignis.systems.combat;

import com.silverignis.animation.AnimController;
import com.silverignis.components.*;

public interface Combatant {
    Health getHealth();
    Stats getStats();
    Caster getCaster();
    GridPosition getGridPosition();
    GridMovement getGridMovement();
    Team getTeam();
    StatusContainer getStatusContainer();
    AnimController getAnimController();

    int getCol();
    int getRow();
    float getVisualX();
    float getVisualY();
    float getDepthScale();

    boolean isAlive();
    boolean isDead();
    boolean isInputLocked();
}
