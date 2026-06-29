package com.silverignis.systems.combat;

import com.badlogic.gdx.math.Vector3;
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
    // ponytail: tile-snapped, not smoothed — fine for feet emission; invert projection if you need sprite-glued anchors; for example particles on dashing sprite
    default void worldPos(Vector3 out) {getGridPosition().worldPos(out);}

    boolean isAlive();
    boolean isDead();
    boolean isInputLocked();
}
