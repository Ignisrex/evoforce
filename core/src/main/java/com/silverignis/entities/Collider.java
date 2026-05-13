package com.silverignis.entities;

import com.badlogic.gdx.math.Rectangle;

public interface Collider {
    Rectangle getBounds();
    Team getTeam();
    boolean isAlive();
}
