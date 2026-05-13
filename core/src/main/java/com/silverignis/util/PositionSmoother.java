package com.silverignis.util;

import com.badlogic.gdx.math.Vector2;

public class PositionSmoother {

    private final float speed;
    private final Vector2 visual = new Vector2();

    public PositionSmoother(float speed, float startX, float startY) {
        this.speed = speed;
        this.visual.set(startX, startY);
    }

    public void update(float delta, float targetX, float targetY) {
        float t = 1f - (float) Math.exp(-speed * delta);
        visual.x += (targetX - visual.x) * t;
        visual.y += (targetY - visual.y) * t;
    }

    public float getX() { return visual.x; }
    public float getY() { return visual.y; }
}

